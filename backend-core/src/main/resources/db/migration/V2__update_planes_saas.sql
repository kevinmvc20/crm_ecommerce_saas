-- Actualización de los planes SaaS base con los nuevos límites y precios
UPDATE plan_saas
SET precio_mensual = 15.00,
    limite_usuarios = 5,
    limite_productos = 500
WHERE nombre = 'Plan Básico' OR id = 1;

UPDATE plan_saas
SET precio_mensual = 45.00,
    limite_usuarios = 12,
    limite_productos = 2500
WHERE nombre = 'Plan Pro' OR id = 2;

UPDATE plan_saas
SET precio_mensual = 89.00,
    limite_usuarios = 25,
    limite_productos = -1
WHERE nombre = 'Plan Business' OR id = 3;