#!/usr/bin/env bash
set -Eeuo pipefail

remote_image="${1:?Uso: venus-classificacao-deploy <remote-image> [registry-user]}"
registry_user="${2:-github-actions}"
service="venus-classificacao.service"
container_name="venus-classificacao"
local_image="venus-classificacao:current"
previous_image="venus-classificacao:previous"
health_url="http://127.0.0.1:8081/swagger-ui.html"
state_dir="/var/lib/venus-classificacao"
failed_image_file="$state_dir/failed-image"
health_attempts=60
health_interval=2

allowed='^ghcr\.io/venus-system/venus-classificacao(:[A-Za-z0-9._-]+|@sha256:[0-9a-f]{64})$'
if [[ ! "$remote_image" =~ $allowed ]]; then
  echo "Imagem não permitida: $remote_image" >&2
  exit 2
fi

systemctl cat "$service" >/dev/null 2>&1 || {
  echo "A unit $service não existe ou não pôde ser lida." >&2
  exit 3
}

install -d -m 0755 "$state_dir"
previous_id="$(docker inspect --format '{{.Image}}' "$container_name" 2>/dev/null || true)"
failed_image_id="$(cat "$failed_image_file" 2>/dev/null || true)"
deployed_new_image=false
logged_in=false
new_image_id=""

cleanup() {
  if [[ "$logged_in" == true ]]; then
    docker logout ghcr.io >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT

login_with_optional_token() {
  local token
  if [[ -t 0 ]]; then
    return 0
  fi

  token="$(cat || true)"
  if [[ -n "$token" ]]; then
    printf '%s' "$token" | docker login ghcr.io --username "$registry_user" --password-stdin
    logged_in=true
  fi
}

wait_for_image() {
  local expected_image_id="$1"
  local last_error="nenhuma resposta do health check"
  local health_error
  local running_image_id

  for ((attempt = 1; attempt <= health_attempts; attempt++)); do
    if health_error="$(curl --fail --location --silent --show-error --max-time 3 -o /dev/null "$health_url" 2>&1)"; then
      running_image_id="$(docker inspect --format '{{.Image}}' "$container_name" 2>/dev/null || true)"
      if [[ "$running_image_id" == "$expected_image_id" ]]; then
        return 0
      fi
      last_error="o health check respondeu, mas o container ativo usa outra imagem"
    else
      last_error="${health_error:-nenhuma resposta do health check}"
    fi
    sleep "$health_interval"
  done

  echo "Health check falhou: $last_error" >&2
  return 1
}

mark_failed_image() {
  local image_id="$1"
  printf '%s\n' "$image_id" > "$failed_image_file"
  chmod 0644 "$failed_image_file"
}

rollback() {
  if [[ -z "$previous_id" ]]; then
    echo "Não existe imagem anterior local para rollback." >&2
    return 1
    
  fi

  echo "Tentando rollback para a imagem anterior..." >&2
  docker tag "$previous_image" "$local_image" || return 1
  systemctl restart "$service" || return 1
  wait_for_image "$previous_id"
}

cleanup_previous_image() {
  docker image rm "$previous_image" >/dev/null 2>&1 || true
  docker image prune -f >/dev/null 2>&1 || true
}

handle_signal() {
  local signal="$1"
  local exit_code=130
  trap '' INT TERM
  if [[ "$signal" == TERM ]]; then
    exit_code=143
  fi

  echo "Recebido SIG$signal durante o deploy; revertendo sem aguardar health check." >&2
  if [[ "$deployed_new_image" == true && -n "$previous_id" ]]; then
    if docker tag "$previous_image" "$local_image" && systemctl restart "$service"; then
      echo "Rollback rápido concluído." >&2
    else
      echo "Rollback rápido falhou; o serviço pode estar indisponível." >&2
    fi
  fi
  exit "$exit_code"
}
trap 'handle_signal INT' INT
trap 'handle_signal TERM' TERM

login_with_optional_token
docker pull --quiet "$remote_image" >/dev/null
new_image_id="$(docker image inspect "$remote_image" --format '{{.Id}}' 2>/dev/null || true)"

if [[ -z "$new_image_id" ]]; then
  echo "Não foi possível identificar a imagem nova após o pull." >&2
  exit 1
fi

if [[ -n "$failed_image_id" && "$new_image_id" == "$failed_image_id" ]]; then
  exit 0
fi

if [[ "$new_image_id" == "$previous_id" ]]; then
  rm -f "$failed_image_file"
  exit 0
fi

if [[ -n "$previous_id" ]]; then
  docker tag "$previous_id" "$previous_image"
fi

new_revision="$(docker image inspect "$remote_image" --format '{{index .Config.Labels "org.opencontainers.image.revision"}}' 2>/dev/null || true)"
if [[ -z "$new_revision" || "$new_revision" == "<no value>" ]]; then
  new_revision="unknown"
fi

docker tag "$new_image_id" "$local_image"
deployed_new_image=true

if ! systemctl restart "$service"; then
  mark_failed_image "$new_image_id"
  echo "Falha ao reiniciar $service." >&2
  journalctl -u "$service" -n 100 --no-pager >&2 || true
  rollback || true
  exit 1
fi

if wait_for_image "$new_image_id"; then
  rm -f "$failed_image_file"
  cleanup_previous_image
  echo "Deploy concluído: service=$service image=$remote_image revision=$new_revision local=$local_image health=$health_url"
  exit 0
fi

mark_failed_image "$new_image_id"
echo "A imagem nova não passou no health check." >&2
journalctl -u "$service" -n 100 --no-pager >&2 || true
if ! rollback; then
  echo "ATENÇÃO: o rollback também falhou; a aplicação pode estar indisponível." >&2
fi
exit 1
