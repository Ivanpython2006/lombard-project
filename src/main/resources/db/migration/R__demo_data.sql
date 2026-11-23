-- The demo profile enables this script. A populated database is left intact.
DO $demo$
DECLARE
    demo_client_user_id bigint;
    demo_loan_password constant text := '$2a$10$KOGfsRw2jBNCNKZVW09kX.kXahZI.Fh7QUrhwj.uQuDbwdpq87LTa';
BEGIN
    IF NOT ${seedDemoData} OR EXISTS (SELECT 1 FROM users)
        OR EXISTS (SELECT 1 FROM clients) OR EXISTS (SELECT 1 FROM loans)
        OR EXISTS (SELECT 1 FROM collateral) THEN
        RETURN;
    END IF;

    INSERT INTO users (username, password, role, user_number)
    VALUES ('admin', demo_loan_password, 'ADMIN', 'A-DEMO'),
           ('manager', demo_loan_password, 'MANAGER', 'M-DEMO'),
           ('client', demo_loan_password, 'CLIENT', 'U-DEMO');

    SELECT id INTO STRICT demo_client_user_id FROM users WHERE username = 'client';

    INSERT INTO clients (user_id, name, last_name, first_name, phone, email, client_number)
    SELECT CASE WHEN n = 1 THEN demo_client_user_id END,
           'Демо Клиент ' || n, 'Демо', 'Клиент ' || n,
           '+7000000000' || n, 'client' || n || '@example.test', 'DEMO-C' || n
    FROM generate_series(1, 6) AS series(n);

    INSERT INTO accounts (client_id, balance)
    SELECT id, CASE WHEN user_id = demo_client_user_id THEN 20000 ELSE 0 END
    FROM clients;

    WITH fixtures (n, item_name, amount, status, days_ago, due_in) AS (
        VALUES (1, 'Фотоаппарат', 12000, 'Выдан', 5, 25),
               (2, 'Ноутбук', 18000, 'В ожидании', 0, NULL),
               (3, 'Наручные часы', 4000, 'Закрыт', 35, -5),
               (4, 'Серебряный браслет', 5000, 'На складе', 40, -10),
               (5, 'Игровая приставка', 14000, 'На продаже', 45, -15),
               (6, 'Объектив', 8000, 'Куплен', 50, -20)
    ), inserted_loans AS (
        INSERT INTO loans (client_id, amount, rate, status, created_date, decision_date,
                           due_date, client_approved, agreed_amount, loan_number)
        SELECT c.id, f.amount, CASE WHEN f.due_in IS NOT NULL THEN 10 END, f.status,
               CURRENT_DATE - f.days_ago,
               CASE WHEN f.due_in IS NOT NULL THEN CURRENT_DATE - f.days_ago END,
               CURRENT_DATE + f.due_in, f.due_in IS NOT NULL,
               CASE WHEN f.due_in IS NOT NULL THEN f.amount END, 'DEMO-L' || f.n
        FROM fixtures f
        JOIN clients c ON c.client_number = 'DEMO-C' || f.n
        RETURNING id, loan_number
    )
    INSERT INTO collateral (name, description, estimated_price, loan_id, item_number)
    SELECT f.item_name, 'Демонстрационный залог', f.amount * 1.25,
           l.id, 'DEMO-I' || f.n
    FROM fixtures f
    JOIN inserted_loans l ON l.loan_number = 'DEMO-L' || f.n;
END
$demo$;
