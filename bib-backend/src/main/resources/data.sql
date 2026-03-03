-- Test user voor development (enkel als nog niet aanwezig)
INSERT INTO users (id, smartschool_id, display_name)
SELECT 1, 'dev-user', 'Dev Gebruiker'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE id = 1);
