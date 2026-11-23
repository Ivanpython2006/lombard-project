CREATE TABLE account_transactions (
    id bigint NOT NULL,
    account_id bigint NOT NULL,
    amount numeric(19,2) NOT NULL,
    type character varying(50) NOT NULL,
    description character varying(300),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_tx_type CHECK (((type)::text = ANY (ARRAY[('DEPOSIT'::character varying)::text, ('WITHDRAW'::character varying)::text, ('PURCHASE'::character varying)::text, ('REFUND'::character varying)::text])))
);
CREATE SEQUENCE account_transactions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
ALTER SEQUENCE account_transactions_id_seq OWNED BY account_transactions.id;
CREATE TABLE accounts (
    id bigint NOT NULL,
    client_id bigint NOT NULL,
    balance numeric(19,2) DEFAULT 0 NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_accounts_balance CHECK (((balance)::double precision >= ((0)::numeric)::double precision))
);
CREATE SEQUENCE accounts_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
ALTER SEQUENCE accounts_id_seq OWNED BY accounts.id;
CREATE TABLE cart_items (
    id bigint NOT NULL,
    client_id bigint NOT NULL,
    loan_id bigint NOT NULL,
    added_at timestamp without time zone DEFAULT now() NOT NULL
);
CREATE SEQUENCE cart_items_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
ALTER SEQUENCE cart_items_id_seq OWNED BY cart_items.id;
CREATE TABLE clients (
    id bigint NOT NULL,
    user_id bigint,
    phone character varying(50) NOT NULL,
    email character varying(100),
    name character varying(255) NOT NULL,
    last_name character varying(255),
    first_name character varying(255),
    middle_name character varying(255),
    client_number character varying(20),
    CONSTRAINT chk_clients_email CHECK (((email IS NULL) OR ((email)::text ~ '^[^@\s]+@[^@\s]+\.[^@\s]+$'::text))),
    CONSTRAINT chk_clients_phone CHECK (((phone)::text ~ '^\+7\d{10}$'::text))
);
CREATE SEQUENCE clients_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
ALTER SEQUENCE clients_id_seq OWNED BY clients.id;
CREATE TABLE collateral (
    id bigint NOT NULL,
    name character varying(255),
    description text,
    estimated_price numeric(19,2),
    loan_id bigint,
    item_number character varying(20)
);
CREATE SEQUENCE collateral_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
ALTER SEQUENCE collateral_id_seq OWNED BY collateral.id;
CREATE TABLE collateral_photos (
    collateral_id bigint NOT NULL,
    photo_url character varying(500),
    sort_order integer NOT NULL
);
CREATE TABLE loan_history (
    id bigint NOT NULL,
    loan_id bigint NOT NULL,
    action character varying(255) NOT NULL,
    changed_by character varying(50),
    changed_at timestamp without time zone DEFAULT now() NOT NULL
);
CREATE SEQUENCE loan_history_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
ALTER SEQUENCE loan_history_id_seq OWNED BY loan_history.id;
CREATE TABLE loans (
    id bigint NOT NULL,
    client_id bigint NOT NULL,
    amount numeric(19,2) NOT NULL,
    rate numeric(10,2),
    status character varying(50) DEFAULT 'В ожидании'::character varying NOT NULL,
    created_date date DEFAULT CURRENT_DATE NOT NULL,
    decision_date date,
    due_date date,
    client_approved boolean,
    agreed_amount numeric(19,2),
    loan_number character varying(100),
    CONSTRAINT chk_loans_amount CHECK (((amount)::double precision >= ((1)::numeric)::double precision)),
    CONSTRAINT chk_loans_rate CHECK (((rate IS NULL) OR (((rate)::double precision >= ((0)::numeric)::double precision) AND ((rate)::double precision <= ((100)::numeric)::double precision))))
);
CREATE SEQUENCE loans_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
ALTER SEQUENCE loans_id_seq OWNED BY loans.id;
CREATE TABLE notifications (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    is_read boolean NOT NULL,
    type character varying(30),
    user_id bigint,
    event_type character varying(50),
    arg1 character varying(300),
    arg2 character varying(300),
    arg3 character varying(300),
    arg4 character varying(300),
    arg5 character varying(300)
);
CREATE SEQUENCE notifications_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
ALTER SEQUENCE notifications_id_seq OWNED BY notifications.id;
CREATE TABLE number_sequences (
    prefix character varying(10) NOT NULL,
    next_value bigint DEFAULT 1 NOT NULL
);
CREATE TABLE order_items (
    id bigint NOT NULL,
    order_id bigint NOT NULL,
    loan_id bigint,
    item_name character varying(200),
    item_price numeric(19,2) NOT NULL
);
CREATE SEQUENCE order_items_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
ALTER SEQUENCE order_items_id_seq OWNED BY order_items.id;
CREATE TABLE orders (
    id bigint NOT NULL,
    client_id bigint NOT NULL,
    total_amount numeric(19,2) NOT NULL,
    status character varying(50) DEFAULT 'Оплачен'::character varying NOT NULL,
    delivery_type character varying(50) NOT NULL,
    address_city character varying(100),
    address_street character varying(200),
    address_building character varying(50),
    address_apartment character varying(50),
    pickup_address character varying(300),
    contact_phone character varying(50),
    comment character varying(500),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    order_number character varying(50),
    CONSTRAINT chk_orders_amount CHECK (((total_amount)::double precision >= ((0)::numeric)::double precision)),
    CONSTRAINT chk_orders_delivery CHECK (((delivery_type)::text = ANY (ARRAY[('DELIVERY'::character varying)::text, ('PICKUP'::character varying)::text]))),
    CONSTRAINT chk_orders_status CHECK (((status)::text = ANY (ARRAY[('Оплачен'::character varying)::text, ('Собран'::character varying)::text, ('Доставлен'::character varying)::text, ('Отменён'::character varying)::text])))
);
CREATE SEQUENCE orders_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
ALTER SEQUENCE orders_id_seq OWNED BY orders.id;
CREATE TABLE users (
    id bigint NOT NULL,
    username character varying(50) NOT NULL,
    password character varying(255) NOT NULL,
    role character varying(50) NOT NULL,
    user_number character varying(20),
    CONSTRAINT chk_users_role CHECK (((role)::text = ANY (ARRAY[('ADMIN'::character varying)::text, ('MANAGER'::character varying)::text, ('CLIENT'::character varying)::text]))),
    CONSTRAINT chk_users_username CHECK ((length((username)::text) >= 1))
);
CREATE SEQUENCE users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
ALTER SEQUENCE users_id_seq OWNED BY users.id;
ALTER TABLE ONLY account_transactions ALTER COLUMN id SET DEFAULT nextval('account_transactions_id_seq'::regclass);
ALTER TABLE ONLY accounts ALTER COLUMN id SET DEFAULT nextval('accounts_id_seq'::regclass);
ALTER TABLE ONLY cart_items ALTER COLUMN id SET DEFAULT nextval('cart_items_id_seq'::regclass);
ALTER TABLE ONLY clients ALTER COLUMN id SET DEFAULT nextval('clients_id_seq'::regclass);
ALTER TABLE ONLY collateral ALTER COLUMN id SET DEFAULT nextval('collateral_id_seq'::regclass);
ALTER TABLE ONLY loan_history ALTER COLUMN id SET DEFAULT nextval('loan_history_id_seq'::regclass);
ALTER TABLE ONLY loans ALTER COLUMN id SET DEFAULT nextval('loans_id_seq'::regclass);
ALTER TABLE ONLY notifications ALTER COLUMN id SET DEFAULT nextval('notifications_id_seq'::regclass);
ALTER TABLE ONLY order_items ALTER COLUMN id SET DEFAULT nextval('order_items_id_seq'::regclass);
ALTER TABLE ONLY orders ALTER COLUMN id SET DEFAULT nextval('orders_id_seq'::regclass);
ALTER TABLE ONLY users ALTER COLUMN id SET DEFAULT nextval('users_id_seq'::regclass);
ALTER TABLE ONLY account_transactions
    ADD CONSTRAINT account_transactions_pkey PRIMARY KEY (id);
ALTER TABLE ONLY accounts
    ADD CONSTRAINT accounts_client_id_key UNIQUE (client_id);
ALTER TABLE ONLY accounts
    ADD CONSTRAINT accounts_pkey PRIMARY KEY (id);
ALTER TABLE ONLY cart_items
    ADD CONSTRAINT cart_items_pkey PRIMARY KEY (id);
ALTER TABLE ONLY clients
    ADD CONSTRAINT clients_phone_key UNIQUE (phone);
ALTER TABLE ONLY clients
    ADD CONSTRAINT clients_pkey PRIMARY KEY (id);
ALTER TABLE ONLY clients
    ADD CONSTRAINT clients_user_id_key UNIQUE (user_id);
ALTER TABLE ONLY collateral_photos
    ADD CONSTRAINT collateral_photos_pkey PRIMARY KEY (collateral_id, sort_order);
ALTER TABLE ONLY collateral
    ADD CONSTRAINT collateral_pkey PRIMARY KEY (id);
ALTER TABLE ONLY loan_history
    ADD CONSTRAINT loan_history_pkey PRIMARY KEY (id);
ALTER TABLE ONLY loans
    ADD CONSTRAINT loans_pkey PRIMARY KEY (id);
ALTER TABLE ONLY notifications
    ADD CONSTRAINT notifications_pkey PRIMARY KEY (id);
ALTER TABLE ONLY number_sequences
    ADD CONSTRAINT number_sequences_pkey PRIMARY KEY (prefix);
ALTER TABLE ONLY order_items
    ADD CONSTRAINT order_items_pkey PRIMARY KEY (id);
ALTER TABLE ONLY orders
    ADD CONSTRAINT orders_pkey PRIMARY KEY (id);
ALTER TABLE ONLY cart_items
    ADD CONSTRAINT uknp77qoud7glipig5en419flvv UNIQUE (client_id, loan_id);
ALTER TABLE ONLY cart_items
    ADD CONSTRAINT uq_cart_client_loan UNIQUE (client_id, loan_id);
ALTER TABLE ONLY clients
    ADD CONSTRAINT uq_client_number UNIQUE (client_number);
ALTER TABLE ONLY collateral
    ADD CONSTRAINT uq_item_number UNIQUE (item_number);
ALTER TABLE ONLY loans
    ADD CONSTRAINT uq_loan_number UNIQUE (loan_number);
ALTER TABLE ONLY orders
    ADD CONSTRAINT uq_order_number UNIQUE (order_number);
ALTER TABLE ONLY users
    ADD CONSTRAINT uq_user_number UNIQUE (user_number);
ALTER TABLE ONLY users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);
ALTER TABLE ONLY users
    ADD CONSTRAINT users_username_key UNIQUE (username);
CREATE INDEX idx_accounts_client ON accounts USING btree (client_id);
CREATE INDEX idx_cart_client ON cart_items USING btree (client_id);
CREATE INDEX idx_clients_email ON clients USING btree (email);
CREATE INDEX idx_clients_phone ON clients USING btree (phone);
CREATE INDEX idx_clients_user_id ON clients USING btree (user_id);
CREATE INDEX idx_history_date ON loan_history USING btree (changed_at DESC);
CREATE INDEX idx_history_loan ON loan_history USING btree (loan_id);
CREATE INDEX idx_loans_client ON loans USING btree (client_id);
CREATE INDEX idx_loans_created ON loans USING btree (created_date DESC);
CREATE INDEX idx_loans_due ON loans USING btree (due_date);
CREATE INDEX idx_loans_overdue ON loans USING btree (due_date) WHERE ((status)::text = 'Выдан'::text);
CREATE INDEX idx_loans_status ON loans USING btree (status);
CREATE INDEX idx_order_items_order ON order_items USING btree (order_id);
CREATE INDEX idx_orders_client ON orders USING btree (client_id);
CREATE INDEX idx_orders_created ON orders USING btree (created_at DESC);
CREATE INDEX idx_tx_account ON account_transactions USING btree (account_id);
CREATE INDEX idx_tx_created ON account_transactions USING btree (created_at DESC);
CREATE INDEX idx_users_role ON users USING btree (role);
CREATE INDEX idx_users_username ON users USING btree (username);
ALTER TABLE ONLY account_transactions
    ADD CONSTRAINT account_transactions_account_id_fkey FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE;
ALTER TABLE ONLY accounts
    ADD CONSTRAINT accounts_client_id_fkey FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE;
ALTER TABLE ONLY cart_items
    ADD CONSTRAINT cart_items_client_id_fkey FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE;
ALTER TABLE ONLY cart_items
    ADD CONSTRAINT cart_items_loan_id_fkey FOREIGN KEY (loan_id) REFERENCES loans(id) ON DELETE CASCADE;
ALTER TABLE ONLY clients
    ADD CONSTRAINT clients_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE ONLY collateral
    ADD CONSTRAINT collateral_loan_id_fkey FOREIGN KEY (loan_id) REFERENCES loans(id) ON DELETE CASCADE;
ALTER TABLE ONLY notifications
    ADD CONSTRAINT fk9y21adhxn0ayjhfocscqox7bh FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE ONLY collateral_photos
    ADD CONSTRAINT fkmtq4mahgo02s9t1nl658x9pe8 FOREIGN KEY (collateral_id) REFERENCES collateral(id);
ALTER TABLE ONLY loan_history
    ADD CONSTRAINT loan_history_loan_id_fkey FOREIGN KEY (loan_id) REFERENCES loans(id) ON DELETE CASCADE;
ALTER TABLE ONLY loans
    ADD CONSTRAINT loans_client_id_fkey FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE;
ALTER TABLE ONLY order_items
    ADD CONSTRAINT order_items_loan_id_fkey FOREIGN KEY (loan_id) REFERENCES loans(id) ON DELETE SET NULL;
ALTER TABLE ONLY order_items
    ADD CONSTRAINT order_items_order_id_fkey FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE;
ALTER TABLE ONLY orders
    ADD CONSTRAINT orders_client_id_fkey FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE;
