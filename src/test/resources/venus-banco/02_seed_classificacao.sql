SET search_path TO venus, public;

INSERT INTO scoring_models (name, version, description, is_active)
VALUES ('Recomendação Geral', '1.0', 'Modelo base dos testes da classificação', TRUE);

INSERT INTO profile_tags (name, slug, category)
VALUES ('Pele acneica', 'pele-acneica', 'skin');

INSERT INTO ingredient_categories (name)
VALUES ('Ativos');

INSERT INTO ingredients (fk_ingredient_category_id, inci_name, common_name)
SELECT ingredient_category_id, 'NIACINAMIDE', 'Niacinamida'
FROM ingredient_categories
WHERE name = 'Ativos';

INSERT INTO brands (name, country)
VALUES ('Marca Teste', 'BR');

INSERT INTO product_categories (name)
VALUES ('Sérum');

INSERT INTO products (fk_brand_id, fk_product_category_id, name, slug)
SELECT b.brand_id, c.product_category_id, p.name, p.slug
FROM brands b
CROSS JOIN product_categories c
CROSS JOIN (VALUES
        ('Sérum Niacinamida', 'serum-niacinamida'),
        ('Sérum em Revisão', 'serum-em-revisao'),
        ('Sérum sem Ingrediente', 'serum-sem-ingrediente'),
        ('Sérum Reformulado', 'serum-reformulado')) AS p(name, slug)
WHERE b.name = 'Marca Teste'
  AND c.name = 'Sérum';

INSERT INTO product_versions (fk_product_id, version_name, display_name, status, is_current, formula_signature)
SELECT p.product_id, v.version_name, v.version_name, v.status::version_status_enum, v.is_current, v.signature
FROM products p
JOIN (VALUES
        ('serum-niacinamida', 'v1', 'verified', TRUE, 'niacinamida-v1'),
        ('serum-em-revisao', 'v1', 'needs_review', TRUE, 'em-revisao-v1'),
        ('serum-sem-ingrediente', 'v1', 'verified', TRUE, 'sem-ingrediente-v1'),
        ('serum-reformulado', 'v1', 'verified', FALSE, 'reformulado-v1'),
        ('serum-reformulado', 'v2', 'verified', TRUE, 'reformulado-v2')) AS v(slug, version_name, status, is_current, signature)
  ON v.slug = p.slug;

INSERT INTO product_ingredients (fk_product_version_id, fk_ingredient_id, position)
SELECT pv.product_version_id, i.ingredient_id, 1
FROM product_versions pv
CROSS JOIN ingredients i
WHERE pv.formula_signature IN ('niacinamida-v1', 'em-revisao-v1', 'reformulado-v1', 'reformulado-v2')
  AND i.inci_name = 'NIACINAMIDE';

INSERT INTO ingredient_effects (fk_ingredient_id, fk_profile_tag_id, effect_category, effect_name)
SELECT i.ingredient_id, t.profile_tag_id, 'benefit'::effect_category_enum, 'controle-da-acne'
FROM ingredients i
CROSS JOIN profile_tags t
WHERE i.inci_name = 'NIACINAMIDE'
  AND t.slug = 'pele-acneica';

INSERT INTO compatibility_rules (fk_ingredient_effect_id, fk_scoring_model_id, effect_type, score_delta, weight, reason)
SELECT e.ingredient_effect_id, m.scoring_model_id, 'bonus'::effect_type_enum, 5, 1.00, 'Niacinamida ajuda a pele com acne.'
FROM ingredient_effects e
CROSS JOIN scoring_models m
WHERE e.effect_name = 'controle-da-acne'
  AND m.name = 'Recomendação Geral';
