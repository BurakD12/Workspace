insert into categories (id, name, description) values
    (9001, 'Fasteners', 'Bolts, nuts, screws'),
    (9002, 'Tools', 'Hand tools');

insert into suppliers (id, name, email, phone) values
    (9001, 'Acme Industrial', 'sales@acme.example', '+90 212 000 00 00');

insert into products (id, sku, name, description, unit_price, quantity_on_hand, 
reorder_level, category_id, supplier_id) values
    (9001, 'SKU-001', 'Hex Bolt M8', 'Zinc plated, 40 mm', 0.25, 500, 100, 9001, 9001), 
    (9002, 'SKU-002', 'Hex Nut M8', 'Zinc plated', 0.10, 40, 100, 9001, null), 
    (9003, 'SKU-003', 'Claw Hammer', '450 g, fibreglass', 12.50, 3, 5, 9002, 9001), 
    (9004, 'SKU-004', 'Screwdriver Set', '6 pieces', 18.00, 25, 5, 9002, null), 
    (9005, 'SKU-005', 'Wood Screw 4x40', 'Box of 200', 0.05, 100, 100, 9001, 9001);