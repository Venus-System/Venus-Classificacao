#!/usr/bin/env bash
set -Eeuo pipefail

remote_image="${VENUS_CLASSIFICACAO_REMOTE_IMAGE:-ghcr.io/venus-system/venus-classificacao:staging}"
registry_user="${VENUS_CLASSIFICACAO_REGISTRY_USER:-github-poller}"
token_file="${VENUS_CLASSIFICACAO_GHCR_TOKEN_FILE:-/etc/venus-classificacao-ghcr.token}"

if [[ ! -r "$token_file" || ! -s "$token_file" ]]; then
  echo "Token do GHCR não encontrado ou vazio: $token_file" >&2
  exit 1
fi

exec /usr/local/sbin/venus-classificacao-deploy "$remote_image" "$registry_user" < "$token_file"
