ALTER TABLE order_items
    ADD COLUMN item_number varchar(20),
    ADD COLUMN item_description text,
    ADD COLUMN item_photo varchar(500);

WITH first_collateral AS (
    SELECT DISTINCT ON (loan_id) id, loan_id, item_number, description
    FROM collateral
    WHERE loan_id IS NOT NULL
    ORDER BY loan_id, id
), first_photo AS (
    SELECT DISTINCT ON (collateral_id) collateral_id, photo_url
    FROM collateral_photos
    ORDER BY collateral_id, sort_order
)
UPDATE order_items i
SET item_number = c.item_number,
    item_description = c.description,
    item_photo = p.photo_url
FROM first_collateral c
LEFT JOIN first_photo p ON p.collateral_id = c.id
WHERE i.loan_id = c.loan_id;

-- Recover the known historical demo order without changing its loan relationship.
UPDATE order_items i
SET item_number = c.item_number,
    item_description = c.description,
    item_photo = p.photo_url
FROM orders o, collateral c
JOIN collateral_photos p ON p.collateral_id = c.id AND p.sort_order = 0
WHERE i.order_id = o.id
  AND o.order_number = 'M73384'
  AND i.loan_id IS NULL
  AND i.item_name = 'Dyson V15 Detect Vacuum'
  AND i.item_price = 25000.00
  AND c.item_number = 'M30'
  AND c.name = i.item_name
  AND p.photo_url = '/uploads/item_loan_15_0_fb00667d.jpg';
