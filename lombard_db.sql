--
-- PostgreSQL database dump
--

-- Dumped from database version 16.15
-- Dumped by pg_dump version 16.15

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: trg_set_updated_at(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.trg_set_updated_at() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: account_transactions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.account_transactions (
    id bigint NOT NULL,
    account_id bigint NOT NULL,
    amount numeric(19,2) NOT NULL,
    type character varying(50) NOT NULL,
    description character varying(300),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_tx_type CHECK (((type)::text = ANY (ARRAY[('DEPOSIT'::character varying)::text, ('WITHDRAW'::character varying)::text, ('PURCHASE'::character varying)::text, ('REFUND'::character varying)::text])))
);


--
-- Name: account_transactions_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.account_transactions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: account_transactions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.account_transactions_id_seq OWNED BY public.account_transactions.id;


--
-- Name: accounts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.accounts (
    id bigint NOT NULL,
    client_id bigint NOT NULL,
    balance numeric(19,2) DEFAULT 0 NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_accounts_balance CHECK (((balance)::double precision >= ((0)::numeric)::double precision))
);


--
-- Name: accounts_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.accounts_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: accounts_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.accounts_id_seq OWNED BY public.accounts.id;


--
-- Name: cart_items; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.cart_items (
    id bigint NOT NULL,
    client_id bigint NOT NULL,
    loan_id bigint NOT NULL,
    added_at timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: cart_items_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.cart_items_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: cart_items_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.cart_items_id_seq OWNED BY public.cart_items.id;


--
-- Name: clients; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.clients (
    id bigint NOT NULL,
    user_id bigint,
    phone character varying(50) NOT NULL,
    email character varying(100),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    name character varying(255) NOT NULL,
    last_name character varying(255),
    first_name character varying(255),
    middle_name character varying(255),
    client_number character varying(20),
    CONSTRAINT chk_clients_email CHECK (((email IS NULL) OR ((email)::text ~ '^[^@\s]+@[^@\s]+\.[^@\s]+$'::text))),
    CONSTRAINT chk_clients_phone CHECK (((phone)::text ~ '^\+7\d{10}$'::text))
);


--
-- Name: clients_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.clients_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: clients_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.clients_id_seq OWNED BY public.clients.id;


--
-- Name: collateral; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.collateral (
    id bigint NOT NULL,
    name character varying(255),
    description text,
    estimated_price numeric(19,2),
    loan_id bigint,
    category_id bigint,
    item_number character varying(20),
    photo_url character varying(500)
);


--
-- Name: collateral_categories; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.collateral_categories (
    id bigint NOT NULL,
    name character varying(80) NOT NULL,
    slug character varying(80) NOT NULL
);


--
-- Name: collateral_categories_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.collateral_categories_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: collateral_categories_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.collateral_categories_id_seq OWNED BY public.collateral_categories.id;


--
-- Name: collateral_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.collateral_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: collateral_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.collateral_id_seq OWNED BY public.collateral.id;


--
-- Name: collateral_photos; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.collateral_photos (
    collateral_id bigint NOT NULL,
    photo_url character varying(500),
    sort_order integer NOT NULL
);


--
-- Name: loan_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.loan_history (
    id bigint NOT NULL,
    loan_id bigint NOT NULL,
    action character varying(255) NOT NULL,
    changed_by character varying(50),
    changed_at timestamp without time zone DEFAULT now() NOT NULL,
    old_status character varying(50),
    new_status character varying(50),
    comment character varying(500)
);


--
-- Name: loan_history_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.loan_history_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: loan_history_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.loan_history_id_seq OWNED BY public.loan_history.id;


--
-- Name: loans; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.loans (
    id bigint NOT NULL,
    client_id bigint NOT NULL,
    amount numeric(19,2) NOT NULL,
    rate numeric(10,2),
    term_months integer DEFAULT 1 NOT NULL,
    status character varying(50) DEFAULT 'В ожидании'::character varying NOT NULL,
    created_date date DEFAULT CURRENT_DATE NOT NULL,
    decision_date date,
    due_date date,
    closed_date date,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    client_approved boolean,
    agreed_amount numeric(19,2),
    loan_number character varying(100),
    CONSTRAINT chk_loans_amount CHECK (((amount)::double precision >= ((1)::numeric)::double precision)),
    CONSTRAINT chk_loans_rate CHECK (((rate IS NULL) OR (((rate)::double precision >= ((0)::numeric)::double precision) AND ((rate)::double precision <= ((100)::numeric)::double precision)))),
    CONSTRAINT chk_loans_term CHECK (((term_months >= 1) AND (term_months <= 24)))
);


--
-- Name: loans_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.loans_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: loans_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.loans_id_seq OWNED BY public.loans.id;


--
-- Name: notifications; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.notifications (
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


--
-- Name: notifications_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.notifications_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: notifications_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.notifications_id_seq OWNED BY public.notifications.id;


--
-- Name: number_sequences; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.number_sequences (
    prefix character varying(10) NOT NULL,
    next_value bigint DEFAULT 1 NOT NULL
);


--
-- Name: order_items; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.order_items (
    id bigint NOT NULL,
    order_id bigint NOT NULL,
    loan_id bigint,
    item_name character varying(200),
    item_price numeric(19,2) NOT NULL
);


--
-- Name: order_items_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.order_items_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: order_items_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.order_items_id_seq OWNED BY public.order_items.id;


--
-- Name: orders; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.orders (
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


--
-- Name: orders_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.orders_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: orders_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.orders_id_seq OWNED BY public.orders.id;


--
-- Name: payments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.payments (
    id bigint NOT NULL,
    loan_id bigint NOT NULL,
    amount numeric(14,2) NOT NULL,
    payment_type character varying(50) NOT NULL,
    method character varying(50),
    paid_at timestamp without time zone DEFAULT now() NOT NULL,
    comment character varying(300),
    created_by character varying(50),
    CONSTRAINT chk_payments_amount CHECK ((amount > (0)::numeric)),
    CONSTRAINT chk_payments_method CHECK (((method IS NULL) OR ((method)::text = ANY (ARRAY[('Наличные'::character varying)::text, ('Карта'::character varying)::text, ('Перевод'::character varying)::text])))),
    CONSTRAINT chk_payments_type CHECK (((payment_type)::text = ANY (ARRAY[('Погашение'::character varying)::text, ('Проценты'::character varying)::text, ('Штраф'::character varying)::text, ('Досрочное'::character varying)::text])))
);


--
-- Name: payments_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.payments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: payments_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.payments_id_seq OWNED BY public.payments.id;


--
-- Name: users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users (
    id bigint NOT NULL,
    username character varying(50) NOT NULL,
    password character varying(255) NOT NULL,
    role character varying(50) NOT NULL,
    full_name character varying(150),
    email character varying(100),
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    last_login_at timestamp without time zone,
    user_number character varying(20),
    CONSTRAINT chk_users_email CHECK (((email IS NULL) OR ((email)::text ~ '^[^@\s]+@[^@\s]+\.[^@\s]+$'::text))),
    CONSTRAINT chk_users_role CHECK (((role)::text = ANY (ARRAY[('ADMIN'::character varying)::text, ('MANAGER'::character varying)::text, ('CLIENT'::character varying)::text]))),
    CONSTRAINT chk_users_username CHECK ((length((username)::text) >= 1))
);


--
-- Name: users_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: users_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.users_id_seq OWNED BY public.users.id;


--
-- Name: account_transactions id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.account_transactions ALTER COLUMN id SET DEFAULT nextval('public.account_transactions_id_seq'::regclass);


--
-- Name: accounts id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.accounts ALTER COLUMN id SET DEFAULT nextval('public.accounts_id_seq'::regclass);


--
-- Name: cart_items id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cart_items ALTER COLUMN id SET DEFAULT nextval('public.cart_items_id_seq'::regclass);


--
-- Name: clients id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.clients ALTER COLUMN id SET DEFAULT nextval('public.clients_id_seq'::regclass);


--
-- Name: collateral id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.collateral ALTER COLUMN id SET DEFAULT nextval('public.collateral_id_seq'::regclass);


--
-- Name: collateral_categories id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.collateral_categories ALTER COLUMN id SET DEFAULT nextval('public.collateral_categories_id_seq'::regclass);


--
-- Name: loan_history id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.loan_history ALTER COLUMN id SET DEFAULT nextval('public.loan_history_id_seq'::regclass);


--
-- Name: loans id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.loans ALTER COLUMN id SET DEFAULT nextval('public.loans_id_seq'::regclass);


--
-- Name: notifications id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notifications ALTER COLUMN id SET DEFAULT nextval('public.notifications_id_seq'::regclass);


--
-- Name: order_items id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.order_items ALTER COLUMN id SET DEFAULT nextval('public.order_items_id_seq'::regclass);


--
-- Name: orders id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orders ALTER COLUMN id SET DEFAULT nextval('public.orders_id_seq'::regclass);


--
-- Name: payments id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments ALTER COLUMN id SET DEFAULT nextval('public.payments_id_seq'::regclass);


--
-- Name: users id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users ALTER COLUMN id SET DEFAULT nextval('public.users_id_seq'::regclass);


--
-- Data for Name: account_transactions; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.account_transactions (id, account_id, amount, type, description, created_at) FROM stdin;
1	11	100000.00	DEPOSIT	Стартовое пополнение для теста	2026-09-16 23:14:26.544229
2	12	50000.00	DEPOSIT	Стартовое пополнение для теста	2026-09-16 23:14:26.544229
3	11	10.00	DEPOSIT	Пополнение счета	2026-09-16 23:35:04.284864
4	11	7.00	WITHDRAW	Вывод средств	2026-09-16 23:35:09.494825
5	11	55000.00	PURCHASE	Оплата заказа (1 тов.)	2026-09-17 14:34:16.900492
6	11	49.00	WITHDRAW	Вывод средств	2026-09-22 14:21:44.308714
7	11	25000.00	PURCHASE	Оплата заказа (1 тов.)	2026-09-22 15:29:26.34675
8	11	100000.00	DEPOSIT	Пополнение счета	2026-09-22 19:25:29.238921
9	11	27750.00	WITHDRAW	Погашение займа №37	2026-09-22 22:16:38.503233
10	11	1010.00	WITHDRAW	Погашение займа №52	2026-09-22 22:32:16.165874
11	11	5.00	WITHDRAW	Вывод средств	2026-09-23 14:36:57.744979
12	11	35000.00	PURCHASE	Оплата заказа (1 тов.)	2026-09-23 14:46:30.994976
13	11	25000.00	PURCHASE	Оплата заказа (1 тов.)	2026-09-27 11:55:30.360563
14	11	1.00	DEPOSIT	Пополнение счета	2026-10-02 12:53:17.213576
15	11	1.00	DEPOSIT	Пополнение счета	2026-10-02 12:53:19.881235
16	11	1.00	WITHDRAW	Вывод средств	2026-10-12 15:47:34.49013
17	11	100000.00	DEPOSIT	Пополнение счета	2026-10-20 15:07:43.766501
18	11	65000.00	PURCHASE	Оплата заказа (1 тов.)	2026-10-20 15:10:10.689481
19	11	33600.00	WITHDRAW	Погашение займа L98428	2026-10-20 18:56:12.247472
20	11	1.01	WITHDRAW	Погашение займа L98429	2026-10-21 10:58:15.355335
21	11	40000.00	DEPOSIT	Пополнение счета	2026-10-21 11:08:24.922236
22	11	300000.00	DEPOSIT	Пополнение счета	2026-10-21 11:10:24.901782
23	11	300000.00	DEPOSIT	Пополнение счета	2026-10-21 11:10:40.003823
\.


--
-- Data for Name: accounts; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.accounts (id, client_id, balance, created_at, updated_at) FROM stdin;
1	2	0.00	2026-09-16 23:14:26.544229	2026-09-16 23:14:26.544229
2	3	0.00	2026-09-16 23:14:26.544229	2026-09-16 23:14:26.544229
3	4	0.00	2026-09-16 23:14:26.544229	2026-09-16 23:14:26.544229
4	5	0.00	2026-09-16 23:14:26.544229	2026-09-16 23:14:26.544229
5	6	0.00	2026-09-16 23:14:26.544229	2026-09-16 23:14:26.544229
6	7	0.00	2026-09-16 23:14:26.544229	2026-09-16 23:14:26.544229
7	8	0.00	2026-09-16 23:14:26.544229	2026-09-16 23:14:26.544229
8	9	0.00	2026-09-16 23:14:26.544229	2026-09-16 23:14:26.544229
9	10	0.00	2026-09-16 23:14:26.544229	2026-09-16 23:14:26.544229
10	11	0.00	2026-09-16 23:14:26.544229	2026-09-16 23:14:26.544229
12	13	50000.00	2026-09-16 23:14:26.544229	2026-09-16 23:14:26.544229
11	12	672588.99	2026-09-16 23:14:26.544229	2026-10-21 11:10:40.003823
\.


--
-- Data for Name: cart_items; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.cart_items (id, client_id, loan_id, added_at) FROM stdin;
\.


--
-- Data for Name: clients; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.clients (id, user_id, phone, email, created_at, updated_at, name, last_name, first_name, middle_name, client_number) FROM stdin;
13	\N	+79002223344	client2@lombard.ru	2026-09-16 23:11:26.918277	2026-10-01 19:41:20.675667	Петрова Мария Сергеевна	Петрова	Мария	Сергеевна	C13
2	\N	+79151112233	petrova@mail.ru	2026-09-16 19:48:34.024472	2026-09-23 11:16:27.953528	Петрова Мария Сергеевна	Петрова	Мария	Сергеевна	C2
3	\N	+79162223344	sidorov@gmail.com	2026-09-16 19:48:34.024472	2026-09-23 11:16:27.953528	Сидоров Алексей Петрович	Сидоров	Алексей	Петрович	C3
4	\N	+79173334455	kuznetsova@yandex.ru	2026-09-16 19:48:34.024472	2026-09-23 11:16:27.953528	Кузнецова Анна Дмитриевна	Кузнецова	Анна	Дмитриевна	C4
5	\N	+79184445566	smirnov@mail.ru	2026-09-16 19:48:34.024472	2026-09-23 11:16:27.953528	Смирнов Дмитрий Александрович	Смирнов	Дмитрий	Александрович	C5
6	\N	+79195556677	volkova@gmail.com	2026-09-16 19:48:34.024472	2026-09-23 11:16:27.953528	Волкова Елена Викторовна	Волкова	Елена	Викторовна	C6
7	\N	+79206667788	morozov@yandex.ru	2026-09-16 19:48:34.024472	2026-09-23 11:16:27.953528	Морозов Андрей Сергеевич	Морозов	Андрей	Сергеевич	C7
8	\N	+79217778899	novikova@mail.ru	2026-09-16 19:48:34.024472	2026-09-23 11:16:27.953528	Новикова Ольга Игоревна	Новикова	Ольга	Игоревна	C8
9	\N	+79228889900	fedorov@gmail.com	2026-09-16 19:48:34.024472	2026-09-23 11:16:27.953528	Фёдоров Павел Николаевич	Фёдоров	Павел	Николаевич	C9
10	\N	+79239990011	romanova@yandex.ru	2026-09-16 19:48:34.024472	2026-09-23 11:16:27.953528	Романова Татьяна Алексеевна	Романова	Татьяна	Алексеевна	C10
11	\N	+79241112233	belov@mail.ru	2026-09-16 19:48:34.024472	2026-09-23 11:16:27.953528	Белов Артём Олегович	Белов	Артём	Олегович	C11
12	3	+79001112233	client@lombard.ru	2026-09-16 23:11:26.918277	2026-09-23 11:16:27.953528	Клиентов Клиент Клиентович	Клиентов	Клиент	Клиентович	C12
14	\N	+79991002030	john.smith@example.com	2026-09-18 17:03:51.161842	2026-09-23 11:16:27.953528	Smith John Robert	Smith	John	Robert	C14
15	\N	+79992003040	emma.j@example.com	2026-09-18 17:03:51.161842	2026-09-23 11:16:27.953528	Johnson Emma Louise	Johnson	Emma	Louise	C15
16	\N	+79993004050	michael.w@example.com	2026-09-18 17:03:51.161842	2026-09-23 11:16:27.953528	Williams Michael David	Williams	Michael	David	C16
17	\N	+79994005060	sophia.b@example.com	2026-09-18 17:03:51.161842	2026-09-23 11:16:27.953528	Brown Sophia Grace	Brown	Sophia	Grace	C17
18	\N	+79995006070	daniel.j@example.com	2026-09-18 17:03:51.161842	2026-09-23 11:16:27.953528	Jones Daniel Thomas	Jones	Daniel	Thomas	C18
19	\N	+79996007080	petr.ivanov@example.com	2026-09-18 17:03:51.161842	2026-09-23 11:16:27.953528	Иванов Пётр Сергеевич	Иванов	Пётр	Сергеевич	C19
20	\N	+79997008090	anna.k@example.com	2026-09-18 17:03:51.161842	2026-09-23 11:16:27.953528	Козлова Анна Игоревна	Козлова	Анна	Игоревна	C20
30	\N	+79991234567	ivaneivan@mail.ru	2026-09-22 14:58:38.500866	2026-10-01 19:41:14.294364	Иванов Иван	Иванов	Иван	\N	C30
\.


--
-- Data for Name: collateral; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.collateral (id, name, description, estimated_price, loan_id, category_id, item_number, photo_url) FROM stdin;
25	Автомобильные диски R18	Литые, оригинал BMW. 4 штуки, состояние новых.	80000.00	10	\N	M25	\N
19	Телевизор LG 55" 4K	Модель 55UN7300, Smart TV, WebOS. Пульт, коробка. 1.5 года использования.	30000.00	4	\N	M19	\N
18	Золотое кольцо 585 пробы	Вес 4.2 г, вставка — фианит. Есть клеймо, состояние идеальное.	18000.00	3	\N	M18	\N
17	MacBook Air M1 256GB	Silver, cycle count 120, battery 94%. Original, no repairs.	55000.00	2	\N	M17	\N
36	Gucci GG Marmont Shoulder Bag	Black leather, gold hardware. Original with dust bag and card.	95000.00	21	\N	M36	\N
38	Шуба норковая	Натуральный мех, длина 110 см, размер 46. Состояние отличное.	120000.00	23	\N	M38	\N
61	Гитара Fender Stratocaster	Американская сборка, кейс в комплекте	42000.00	46	\N	M61	\N
63	Электросамокат Xiaomi Pro 2	Пробег 500 км, состояние отличное	28000.00	48	\N	M63	\N
26	iPhone 14 Pro 256GB	Deep Purple, battery 98%, complete set with box and cable.	70000.00	11	\N	M26	\N
27	Rolex Submariner 116610LN	Stainless steel, black dial, 2018. Full set: box, papers, warranty.	450000.00	12	\N	M27	\N
30	Dyson V15 Detect Vacuum	Cordless, with all attachments. 8 months of use.	25000.00	15	\N	M30	\N
32	Ковёр персидский	Ручная работа, 2×3 м, натуральные красители. Возраст около 20 лет.	35000.00	17	\N	M32	\N
43	Смартфон Samsung Galaxy S23	Отличное состояние, полный комплект	65000.00	28	\N	M43	\N
44	Смартфон Samsung Galaxy S23	Отличное состояние, полный комплект	65000.00	29	\N	M44	\N
45	Ноутбук Lenovo ThinkPad X1	Рабочий, батарея держит 6 часов	45000.00	30	\N	M45	\N
47	Ноутбук Lenovo ThinkPad X1	Рабочий, батарея держит 6 часов	45000.00	32	\N	M47	\N
48	Часы Apple Watch Series 8	Использовались 3 месяца, гарантия	35000.00	33	\N	M48	\N
50	Часы Apple Watch Series 8	Использовались 3 месяца, гарантия	35000.00	35	\N	M50	\N
57	Кольцо с бриллиантом	Золото 585 пробы, 1 карат	85000.00	42	\N	M57	\N
20	PlayStation 5 Slim	Disc version, 1 TB. Two DualSense controllers included.	38000.00	5	\N	M20	\N
22	Часы Casio G-Shock GA-2100	Карбоновый корпус, оригинал. Небольшие царапины на стекле.	12000.00	7	\N	M22	\N
23	Ноутбук ASUS ROG Strix G15	RTX 3060, Ryzen 7, 16GB RAM, 512GB SSD. Игровой, 1 год использования.	65000.00	8	\N	M23	\N
24	Samsung Galaxy S22	Состояние отличное, царапин нет. Комплект полный.	35000.00	9	\N	M24	\N
65	Электросамокат Xiaomi Pro 2	Пробег 500 км, состояние отличное	28000.00	50	\N	M65	\N
66	Телевизор LG OLED 55"	Smart TV, WebOS, коробка	70000.00	51	\N	M66	\N
60	Гитара Fender Stratocaster	Американская сборка, кейс в комплекте	100000.00	45	\N	M60	\N
72	Микроволновка Samsung	Новая, не использовалась	12000.00	57	\N	M72	\N
71	Серьги золотые	Золото 585, 2 камня	25000.00	56	\N	M71	\N
69	Кофемашина DeLonghi	Италия, использовалась 1 год	40000.00	54	\N	M69	\N
70	Скутер Honda PCX	2022 год, пробег 3500 км	130000.00	55	\N	M70	\N
68	iPad Pro 12.9"	Планшет в отличном состоянии, стилус в комплекте	90000.00	53	\N	M68	\N
79	телефон poco x3 pro	состояние АКБ 67% экран не царапан. в комплекте черный чехол	\N	64	\N	M882	\N
80	samsung s21	акб 67 процентов экран целый	40000.00	65	\N	M883	\N
82	Xiaomi 12	АКБ 97%. менялся экран	30000.00	67	\N	M885	\N
83	dell XPS	менялся акб. состояние корпуса идеальное	140000.00	68	\N	M886	\N
\.


--
-- Data for Name: collateral_categories; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.collateral_categories (id, name, slug) FROM stdin;
1	Электроника	electronics
2	Ювелирные изделия	jewelry
3	Бытовая техника	appliances
4	Автомобили	cars
5	Недвижимость	realestate
6	Музыкальные инструменты	music
7	Другое	other
\.


--
-- Data for Name: collateral_photos; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.collateral_photos (collateral_id, photo_url, sort_order) FROM stdin;
70	/uploads/item_loan_55_0_c946c982.jpg	0
30	/uploads/item_loan_15_0_fb00667d.jpg	0
71	/uploads/item_loan_56_0_85bedb72.jpg	0
25	/uploads/item_loan_10_0_f50f8eed.jpg	0
72	/uploads/item_loan_57_0_1193e55d.jpg	0
69	/uploads/item_loan_54_0_980b5b1d.jpg	0
68	/uploads/item_68_0_5c6ecbc8.jpg	0
63	/uploads/item_loan_48_0_7da88ac8.jpg	0
24	/uploads/item_loan_9_0_cd531d08.jpg	0
18	/uploads/item_loan_3_0_f4b95eb8.jpg	0
19	/uploads/item_loan_4_0_2fc97530.jpg	0
43	/uploads/item_loan_28_0_c35a2dce.jpg	0
47	/uploads/item_loan_32_0_b8dc0cf0.jpg	0
22	/uploads/item_loan_7_0_2f1c64a1.jpg	0
60	/uploads/item_loan_45_0_6f89e559.jpg	0
32	/uploads/item_loan_17_0_b381f22d.jpg	0
20	/uploads/item_loan_5_0_5cc716bc.jpg	0
45	/uploads/item_45_0_324a2b62.jpg	0
44	/uploads/item_loan_29_0_78fea3e7.jpg	0
38	/uploads/item_loan_23_0_7b463240.jpg	0
36	/uploads/item_loan_21_0_4a6dc15a.jpg	0
27	/uploads/item_27_0_aa7cd336.jpg	0
26	/uploads/item_loan_11_0_63948d39.jpg	0
23	/uploads/item_loan_8_0_63555cc3.jpg	0
66	/uploads/item_loan_51_0_f52e5271.jpg	0
65	/uploads/item_loan_50_0_e978a070.jpg	0
57	/uploads/item_loan_42_0_17e1432d.jpg	0
17	/uploads/item_loan_2_0_87d1537c.jpg	0
50	/uploads/item_loan_35_0_129bbe41.jpg	0
48	/uploads/item_loan_33_0_544ce2d5.jpg	0
61	/uploads/item_61_0_6921360f.jpg	0
79	/uploads/item_79_0_4b33bd43.jpg	0
80	/uploads/item_80_0_771a23ae.jpg	0
82	/uploads/item_82_0_ca22cbf4.jpg	0
83	/uploads/item_83_0_03ad7d36.jpg	0
\.


--
-- Data for Name: loan_history; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.loan_history (id, loan_id, action, changed_by, changed_at, old_status, new_status, comment) FROM stdin;
1	4	Статус изменён автоматически	\N	2026-09-16 23:57:37.940057	Продан	На складе	\N
2	7	Статус изменён автоматически	\N	2026-09-17 14:33:11.144897	Продан	На складе	\N
3	2	Статус изменён автоматически	\N	2026-09-17 14:34:16.934886	Продан	Куплен	\N
4	7	Статус изменён автоматически	\N	2026-09-17 16:12:43.9312	На складе	Продан	\N
5	4	Статус изменён автоматически	\N	2026-09-17 16:14:27.980702	На складе	Продан	\N
6	5	Статус изменён автоматически	\N	2026-09-17 16:14:30.949756	Продан	На складе	\N
7	5	Статус изменён автоматически	\N	2026-09-17 21:04:54.53341	На складе	Продан	\N
8	7	Статус изменён автоматически	\N	2026-09-17 21:05:02.374048	Продан	На складе	\N
9	5	Статус изменён автоматически	\N	2026-09-20 20:00:20.154201	Продан	На складе	\N
10	7	Статус изменён автоматически	\N	2026-09-20 20:00:26.118241	На складе	Продан	\N
12	15	Статус изменён автоматически	\N	2026-09-22 15:29:26.371091	Продан	Куплен	\N
13	9	Статус изменён автоматически	\N	2026-09-22 19:41:10.786264	В ожидании	Одобрен	\N
14	9	Одобрено. Оценка: 35000.0 ₽, сумма: 25000.0 ₽, ставка: 10.0%	manager	2026-09-22 19:41:10.796191	\N	\N	\N
15	9	Одобрено. Оценка: 35000.0 ₽, сумма: 25000.0 ₽, ставка: 10.0%	manager	2026-09-22 19:41:43.251849	\N	\N	\N
16	5	Статус изменён автоматически	\N	2026-09-22 19:41:48.78147	На складе	Продан	\N
26	3	Статус изменён автоматически	\N	2026-09-22 20:29:01.409287	Продан	Выдан	\N
27	2	Статус изменён автоматически	\N	2026-09-22 20:29:01.409287	Куплен	Выдан	\N
28	4	Статус изменён автоматически	\N	2026-09-22 20:29:01.409287	Продан	Выдан	\N
29	5	Статус изменён автоматически	\N	2026-09-22 20:29:01.409287	Продан	Выдан	\N
31	2	Статус изменён автоматически	\N	2026-09-22 20:30:11.395093	Выдан	На складе	\N
32	45	Статус изменён автоматически	\N	2026-09-22 21:05:49.41657	В ожидании	Одобрен	\N
33	45	Одобрено. Оценка: 100000.0 ₽, сумма: 65000.0 ₽, ставка: 10.0%	admin	2026-09-22 21:05:49.417164	\N	\N	\N
40	5	Статус изменён автоматически	\N	2026-09-22 22:40:49.612422	Выдан	Продан	\N
41	5	Статус: «Выдан» → «Продан»	admin	2026-09-22 22:40:49.625723	\N	\N	\N
42	29	Статус изменён автоматически	\N	2026-09-22 22:44:17.39514	Продан	На складе	\N
48	53	Отредактирован менеджером: сумма=35000.0 ₽, ставка=null%, статус=В ожидании	admin	2026-09-25 14:55:57.757552	\N	\N	\N
49	56	Отредактирован менеджером: сумма=92000.0 ₽, ставка=null%, статус=В ожидании	admin	2026-09-25 15:06:06.143155	\N	\N	\N
50	54	Отредактирован менеджером: сумма=78000.0 ₽, ставка=null%, статус=В ожидании	admin	2026-09-25 15:07:14.655171	\N	\N	\N
51	55	Отредактирован менеджером: сумма=15000.0 ₽, ставка=null%, статус=В ожидании	admin	2026-09-25 15:09:12.575645	\N	\N	\N
52	53	Отредактирован менеджером: сумма=35000.0 ₽, ставка=null%, статус=В ожидании	admin	2026-09-25 15:13:33.312775	\N	\N	\N
53	53	Отредактирован менеджером: сумма=35000.0 ₽, ставка=null%, статус=В ожидании	admin	2026-09-25 15:13:36.973738	\N	\N	\N
54	53	Отредактирован менеджером: сумма=35000.0 ₽, ставка=null%, статус=В ожидании	admin	2026-09-25 15:13:40.256764	\N	\N	\N
55	53	Отредактирован менеджером: сумма=35000.0 ₽, ставка=null%, статус=В ожидании	admin	2026-09-25 15:17:14.640651	\N	\N	\N
56	53	Отредактирован менеджером: сумма=35000.0 ₽, ставка=null%, статус=В ожидании	admin	2026-09-25 15:20:19.215139	\N	\N	\N
57	53	Отредактирован менеджером: сумма=35000.0 ₽, ставка=null%, статус=В ожидании	admin	2026-09-25 15:28:58.492322	\N	\N	\N
58	53	Отредактирован менеджером: сумма=35000.0 ₽, ставка=null%, статус=В ожидании	admin	2026-09-25 15:29:18.181292	\N	\N	\N
59	53	Отредактирован менеджером: сумма=35000.0 ₽, ставка=null%, статус=В ожидании	admin	2026-09-25 16:08:39.176304	\N	\N	\N
60	53	Отредактирован менеджером: сумма=35000.0 ₽, ставка=null%, статус=В ожидании	admin	2026-09-25 16:08:43.769536	\N	\N	\N
61	53	Отредактирован менеджером: сумма=35000.0 ₽, ставка=null%, статус=В ожидании	admin	2026-09-25 16:13:13.782717	\N	\N	\N
62	53	Отредактирован менеджером: сумма=35000.0 ₽, ставка=null%, статус=В ожидании	admin	2026-09-25 16:13:27.761965	\N	\N	\N
63	53	Отредактирован менеджером: сумма=35000.0 ₽, ставка=null%, статус=В ожидании	admin	2026-09-25 16:20:12.996634	\N	\N	\N
64	53	Отредактирован менеджером: сумма=35000.0 ₽, ставка=null%, статус=В ожидании	admin	2026-09-25 16:24:54.347545	\N	\N	\N
65	53	Отредактирован менеджером: сумма=35000.0 ₽, ставка=null%, статус=В ожидании	admin	2026-09-25 16:27:23.438771	\N	\N	\N
66	53	Отредактирован менеджером: сумма=35000.0 ₽, ставка=null%, статус=В ожидании	admin	2026-09-25 16:33:48.876728	\N	\N	\N
67	53	Отредактирован менеджером: сумма=35000.0 ₽, ставка=null%, статус=В ожидании	admin	2026-09-25 16:33:58.408679	\N	\N	\N
68	53	Отредактирован менеджером: сумма=35000.0 ₽, ставка=null%, статус=В ожидании	admin	2026-09-25 16:48:12.026606	\N	\N	\N
69	53	Отредактирован менеджером: сумма=35000.0 ₽, ставка=null%, статус=В ожидании	admin	2026-09-25 16:54:19.389458	\N	\N	\N
70	53	Отредактирован менеджером: сумма=35000.0 ₽, ставка=null%, статус=В ожидании	admin	2026-09-25 16:54:28.965645	\N	\N	\N
71	53	Отредактирован менеджером: сумма=35000.0 ₽, ставка=null%, статус=В ожидании	admin	2026-09-25 19:53:47.27682	\N	\N	\N
72	53	Отредактирован менеджером: сумма=35000.0 ₽, ставка=null%, статус=В ожидании	admin	2026-09-25 19:53:51.016352	\N	\N	\N
73	53	Отредактирован менеджером: сумма=35000.0 ₽, ставка=null%, статус=В ожидании	admin	2026-09-25 19:54:12.470479	\N	\N	\N
74	53	Отредактирован менеджером: сумма=35000.0 ₽, ставка=null%, статус=В ожидании	admin	2026-09-25 20:02:31.572827	\N	\N	\N
75	46	Отредактирован менеджером: сумма=150000.0 ₽, ставка=null%, статус=Отклонен	admin	2026-09-30 19:40:40.790224	\N	\N	\N
76	46	Отредактирован менеджером: сумма=150000.0 ₽, ставка=null%, статус=Отклонен	admin	2026-09-30 19:41:23.121073	\N	\N	\N
77	46	Отредактирован менеджером: сумма=150000.0 ₽, ставка=null%, статус=Отклонен	admin	2026-09-30 19:41:34.233636	\N	\N	\N
78	8	Отредактирован менеджером: сумма=65000.0 ₽, ставка=14.0%, статус=На продаже	admin	2026-09-30 19:43:16.387186	\N	\N	\N
79	12	Отредактирован менеджером: сумма=450000.0 ₽, ставка=8.0%, статус=На продаже	admin	2026-09-30 19:43:33.759172	\N	\N	\N
83	46	Отредактирован менеджером: сумма=150000.0 ₽, ставка=null%, статус=Отклонен	admin	2026-10-01 14:51:27.132979	\N	\N	\N
84	55	Отредактирован менеджером: сумма=15000.0 ₽, ставка=null%, статус=В ожидании	admin	2026-10-01 14:51:46.25016	\N	\N	\N
85	15	Отредактирован менеджером: сумма=25000.0 ₽, ставка=12.0%, статус=Куплен	admin	2026-10-01 14:52:20.682358	\N	\N	\N
86	56	Отредактирован менеджером: сумма=92000.0 ₽, ставка=null%, статус=В ожидании	admin	2026-10-01 14:53:32.88431	\N	\N	\N
87	10	Отредактирован менеджером: сумма=80000.0 ₽, ставка=12.0%, статус=Одобрен	admin	2026-10-01 14:53:51.877008	\N	\N	\N
88	10	Отредактирован менеджером: сумма=80000.0 ₽, ставка=12.0%, статус=Одобрен	admin	2026-10-01 14:53:59.705024	\N	\N	\N
89	57	Отредактирован менеджером: сумма=44000.0 ₽, ставка=null%, статус=В ожидании	admin	2026-10-01 14:54:46.970731	\N	\N	\N
90	54	Отредактирован менеджером: сумма=78000.0 ₽, ставка=null%, статус=В ожидании	admin	2026-10-01 14:55:14.988361	\N	\N	\N
91	53	Отредактирован менеджером: сумма=35000.0 ₽, ставка=null%, статус=В ожидании	admin	2026-10-01 14:56:15.114903	\N	\N	\N
92	48	Отредактирован менеджером: сумма=30000.0 ₽, ставка=null%, статус=Отменена	admin	2026-10-01 15:06:01.707428	\N	\N	\N
93	9	Отредактирован менеджером: сумма=25000.0 ₽, ставка=10.0%, статус=Одобрен	admin	2026-10-01 15:10:08.853866	\N	\N	\N
94	3	Отредактирован менеджером: сумма=18000.0 ₽, ставка=8.0%, статус=Выдан	admin	2026-10-01 15:10:43.40141	\N	\N	\N
95	4	Отредактирован менеджером: сумма=30000.0 ₽, ставка=10.0%, статус=Выдан	admin	2026-10-01 15:11:30.648515	\N	\N	\N
97	28	Отредактирован менеджером: сумма=35000.0 ₽, ставка=8.0%, статус=На продаже	admin	2026-10-01 19:42:34.835566	\N	\N	\N
98	32	Отредактирован менеджером: сумма=15000.0 ₽, ставка=10.0%, статус=Куплен	admin	2026-10-01 19:52:36.056038	\N	\N	\N
99	7	Отредактирован менеджером: сумма=12000.0 ₽, ставка=10.0%, статус=На продаже	admin	2026-10-01 20:02:44.655535	\N	\N	\N
100	45	Отредактирован менеджером: сумма=65000.0 ₽, ставка=10.0%, статус=Одобрен	admin	2026-10-01 22:22:07.161304	\N	\N	\N
101	17	Отредактирован менеджером: сумма=35000.0 ₽, ставка=11.0%, статус=Куплен	admin	2026-10-01 22:22:38.042906	\N	\N	\N
102	5	Отредактирован менеджером: сумма=38000.0 ₽, ставка=15.0%, статус=На продаже	admin	2026-10-01 22:23:20.09515	\N	\N	\N
103	29	Отредактирован менеджером: сумма=75000.0 ₽, ставка=12.0%, статус=На складе	admin	2026-10-01 22:24:21.677379	\N	\N	\N
104	23	Отредактирован менеджером: сумма=120000.0 ₽, ставка=10.0%, статус=На продаже	admin	2026-10-01 22:25:04.554001	\N	\N	\N
105	21	Отредактирован менеджером: сумма=95000.0 ₽, ставка=9.0%, статус=На продаже	admin	2026-10-01 22:25:39.81553	\N	\N	\N
106	11	Отредактирован менеджером: сумма=70000.0 ₽, ставка=10.0%, статус=На продаже	admin	2026-10-01 22:26:39.218898	\N	\N	\N
107	8	Отредактирован менеджером: сумма=65000.0 ₽, ставка=14.0%, статус=На продаже	admin	2026-10-01 22:27:08.597734	\N	\N	\N
108	51	Отредактирован менеджером: сумма=33000.0 ₽, ставка=8.0%, статус=Закрыт	admin	2026-10-01 22:29:25.007769	\N	\N	\N
109	50	Отредактирован менеджером: сумма=20000.0 ₽, ставка=10.0%, статус=Закрыт	admin	2026-10-01 22:29:52.770709	\N	\N	\N
110	42	Отредактирован менеджером: сумма=23000.0 ₽, ставка=9.0%, статус=Одобрен	admin	2026-10-01 22:30:41.983177	\N	\N	\N
111	2	Отредактирован менеджером: сумма=55000.0 ₽, ставка=12.0%, статус=На складе	admin	2026-10-01 22:31:36.776303	\N	\N	\N
113	35	Отредактирован менеджером: сумма=32000.0 ₽, ставка=9.0%, статус=На складе	admin	2026-10-01 22:33:09.551998	\N	\N	\N
114	33	Отредактирован менеджером: сумма=22000.0 ₽, ставка=7.0%, статус=Куплен	admin	2026-10-01 22:33:23.673364	\N	\N	\N
115	65	Ожидает подтверждения. Оценка: 40000 ₽, сумма: 30000.00 ₽, ставка: 12%	admin	2026-10-20 14:49:37.226096	\N	\N	\N
117	3	Напоминание 1 дн. до срока возврата	SYSTEM	2026-10-21 10:58:42.232143	\N	\N	\N
118	4	Напоминание 1 дн. до срока возврата	SYSTEM	2026-10-21 10:58:42.276886	\N	\N	\N
119	67	Ожидает подтверждения. Оценка: 30000 руб., сумма: 15000.00 руб., ставка: 14%	admin	2026-10-21 11:37:30.290174	\N	\N	\N
120	68	Ожидает подтверждения. Оценка: 140000 руб., сумма: 120000.00 руб., ставка: 13%	admin	2026-10-21 11:55:08.364111	\N	\N	\N
121	68	Ожидает подтверждения. Оценка: 140000 руб., сумма: 120000.00 руб., ставка: 13%	admin	2026-10-21 11:55:21.382561	\N	\N	\N
122	68	Отредактирован менеджером: сумма=120000.00 руб., ставка=13.00%, статус=Ожидает подтверждения	admin	2026-10-21 11:55:54.790025	\N	\N	\N
123	3	Напоминание 0 дн. до срока возврата	SYSTEM	2026-10-22 12:24:14.637982	\N	\N	\N
124	4	Напоминание 0 дн. до срока возврата	SYSTEM	2026-10-22 12:24:14.71511	\N	\N	\N
\.


--
-- Data for Name: loans; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.loans (id, client_id, amount, rate, term_months, status, created_date, decision_date, due_date, closed_date, created_at, updated_at, client_approved, agreed_amount, loan_number) FROM stdin;
46	2	150000.00	\N	1	Отклонен	2026-09-07	2026-09-10	\N	\N	2026-09-22 21:02:49.581625	2026-09-23 10:14:30.580593	\N	\N	L74755
48	4	30000.00	\N	1	Отменена	2026-09-15	\N	\N	\N	2026-09-22 21:02:49.581625	2026-09-23 10:14:30.580593	\N	\N	L59765
53	3	35000.00	\N	1	В ожидании	2026-09-21	\N	2026-10-21	\N	2026-09-22 21:19:32.487334	2026-09-23 10:14:30.580593	\N	\N	L26154
54	4	78000.00	\N	1	В ожидании	2026-09-21	\N	2026-10-21	\N	2026-09-22 21:19:32.487334	2026-09-23 10:14:30.580593	\N	\N	L28850
55	5	15000.00	\N	1	В ожидании	2026-09-22	\N	2026-10-22	\N	2026-09-22 21:19:32.487334	2026-09-23 10:14:30.580593	\N	\N	L35453
56	6	92000.00	\N	1	В ожидании	2026-09-22	\N	2026-10-22	\N	2026-09-22 21:19:32.487334	2026-09-23 10:14:30.580593	\N	\N	L98006
57	7	44000.00	\N	1	В ожидании	2026-09-22	\N	2026-10-22	\N	2026-09-22 21:19:32.487334	2026-09-23 10:14:30.580593	\N	\N	L42679
10	11	80000.00	12.00	1	Одобрен	2026-09-13	\N	2026-10-13	\N	2026-09-16 19:48:34.024472	2026-09-23 12:49:12.352994	\N	80000.00	L54783
15	16	25000.00	12.00	1	Куплен	2026-08-29	2026-09-03	2026-09-28	\N	2026-09-18 17:03:51.161842	2026-09-23 12:49:12.352994	\N	25000.00	L74189
9	10	25000.00	10.00	1	Одобрен	2026-09-11	2026-09-22	2026-10-11	\N	2026-09-16 19:48:34.024472	2026-09-23 12:49:12.352994	\N	25000.00	L42467
3	4	18000.00	8.00	1	Выдан	2026-07-28	2026-08-02	2026-10-22	\N	2026-09-16 19:48:34.024472	2026-09-23 12:49:12.352994	\N	18000.00	L63882
4	5	30000.00	10.00	1	Выдан	2026-08-02	2026-08-07	2026-10-22	\N	2026-09-16 19:48:34.024472	2026-09-23 12:49:12.352994	\N	30000.00	L68916
2	3	55000.00	12.00	1	На складе	2026-07-23	2026-07-28	2026-09-21	\N	2026-09-16 19:48:34.024472	2026-09-23 12:49:12.352994	\N	55000.00	L81559
32	7	15000.00	10.00	1	Куплен	2026-07-24	2026-07-29	2026-08-23	\N	2026-09-22 21:02:49.581625	2026-09-23 12:49:12.352994	\N	15000.00	L62227
33	8	22000.00	7.00	1	Куплен	2026-07-29	2026-08-03	2026-08-28	\N	2026-09-22 21:02:49.581625	2026-09-23 12:49:12.352994	\N	22000.00	L98423
42	17	23000.00	9.00	1	Одобрен	2026-09-20	2026-09-21	2026-10-20	\N	2026-09-22 21:02:49.581625	2026-09-23 12:49:12.352994	\N	23000.00	L23784
29	4	75000.00	12.00	1	На складе	2026-08-28	2026-09-02	2026-11-26	\N	2026-09-22 21:02:49.581625	2026-09-23 12:49:12.352994	\N	75000.00	L42147
50	6	20000.00	10.00	1	Закрыт	2026-05-25	2026-05-30	2026-06-24	\N	2026-09-22 21:02:49.581625	2026-09-23 12:49:12.352994	\N	20000.00	L43198
51	7	33000.00	8.00	1	Закрыт	2026-06-14	2026-06-19	2026-07-14	\N	2026-09-22 21:02:49.581625	2026-09-23 12:49:12.352994	\N	33000.00	L13578
45	20	65000.00	10.00	1	Одобрен	2026-09-22	2026-09-22	2026-10-22	\N	2026-09-22 21:02:49.581625	2026-09-23 12:49:12.352994	\N	65000.00	L31186
11	14	70000.00	10.00	1	На продаже	2026-08-19	2026-08-24	2026-09-18	\N	2026-09-18 17:03:51.161842	2026-09-23 12:49:12.352994	\N	70000.00	L48286
12	15	450000.00	8.00	1	На продаже	2026-08-24	2026-08-29	2026-09-23	\N	2026-09-18 17:03:51.161842	2026-09-23 12:49:12.352994	\N	450000.00	L48998
21	18	95000.00	9.00	1	На продаже	2026-09-08	2026-09-13	2026-10-08	\N	2026-09-18 21:59:26.410199	2026-09-23 12:49:12.352994	\N	95000.00	L23601
23	19	120000.00	10.00	1	На продаже	2026-08-27	2026-08-31	2026-09-26	\N	2026-09-18 21:59:26.410199	2026-09-23 12:49:12.352994	\N	120000.00	L29734
7	8	12000.00	10.00	1	На продаже	2026-08-17	2026-08-22	2026-09-16	\N	2026-09-16 19:48:34.024472	2026-09-23 12:49:12.352994	\N	12000.00	L58421
30	5	42000.00	9.00	1	На продаже	2026-08-31	2026-09-04	2026-11-29	\N	2026-09-22 21:02:49.581625	2026-09-23 12:49:12.352994	\N	42000.00	L59689
5	6	38000.00	15.00	1	На продаже	2026-08-07	2026-08-12	2026-10-22	\N	2026-09-16 19:48:34.024472	2026-09-23 12:49:12.352994	\N	38000.00	L90515
17	20	35000.00	11.00	1	Куплен	2026-08-31	2026-09-04	2026-09-30	\N	2026-09-18 17:03:51.161842	2026-09-23 14:46:31.034576	\N	35000.00	L75650
28	3	35000.00	8.00	1	На складе	2026-08-25	2026-08-30	2026-11-23	\N	2026-09-22 21:02:49.581625	2026-10-02 12:55:44.184733	\N	35000.00	L42125
35	10	32000.00	9.00	1	На продаже	2026-06-29	2026-07-04	2026-07-29	\N	2026-09-22 21:02:49.581625	2026-10-02 14:02:27.492032	\N	32000.00	L79255
64	12	10000.00	\N	1	Отменена	2026-10-20	\N	2027-03-20	\N	2026-10-20 14:23:45.007878	2026-10-20 14:33:12.942964	\N	\N	L98427
8	9	65000.00	14.00	1	Куплен	2026-08-22	2026-08-27	2026-09-21	\N	2026-09-16 19:48:34.024472	2026-10-20 15:10:10.667325	\N	65000.00	L35980
65	12	30000.00	12.00	1	Закрыт	2026-10-20	2026-10-20	2027-03-20	\N	2026-10-20 14:34:16.192016	2026-10-20 18:56:12.36284	t	30000.00	L98428
67	12	15000.00	14.00	1	Ожидает подтверждения	2026-10-21	2026-10-21	2027-01-21	\N	2026-10-21 11:30:15.989578	2026-10-21 11:37:30.29137	\N	15000.00	L98430
68	12	120000.00	13.00	1	Выдан	2026-10-21	2026-10-21	2026-11-21	\N	2026-10-21 11:54:46.40725	2026-10-21 11:56:31.868065	t	120000.00	L98431
\.


--
-- Data for Name: notifications; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.notifications (id, created_at, is_read, type, user_id, event_type, arg1, arg2, arg3, arg4, arg5) FROM stdin;
48	2026-09-30 10:11:06.0646	t	SUCCESS	3	LOAN_ISSUED	L98425	iPhone 14 Pro	48600,00	2026-10-15	\N
49	2026-09-29 10:11:06.0646	t	WARNING	3	LOAN_REMINDER	L98425	iPhone 14 Pro	осталось 3 дня	48600,00	2026-10-15
50	2026-09-28 10:11:06.0646	t	SUCCESS	3	LOAN_REPAID	L98425	iPhone 14 Pro	48600,00	\N	\N
51	2026-09-27 10:11:06.0646	t	DANGER	3	LOAN_OVERDUE	L11111	Скутер Honda PCX	2026-09-01	\N	\N
52	2026-09-26 10:11:06.0646	t	SUCCESS	3	ACCOUNT_DEPOSIT	10000,00	Комментарий: пополнение с карты	\N	\N	\N
53	2026-09-25 10:11:06.0646	t	INFO	3	ACCOUNT_WITHDRAW	5000,00	\N	\N	\N	\N
54	2026-09-24 10:11:06.0646	t	WARNING	3	LOAN_CANCELLED	L22222	Кофемашина DeLonghi	\N	\N	\N
55	2026-09-23 10:11:06.0646	t	WARNING	3	LOAN_DECLINED	L33333	Телевизор Samsung	\N	\N	\N
56	2026-09-22 10:11:06.0646	t	WARNING	3	LOAN_CHANGED	L98425	Название: «айфон» → «iPhone 14 Pro».	\N	\N	\N
57	2026-09-21 10:11:06.0646	t	INFO	3	LOAN_EXTENDED	L98425	iPhone 14 Pro	1	2026-11-15	\N
58	2026-10-02 08:11:06.0646	f	SUCCESS	1	ORDER_PLACED	M73384	25000,00	M37	Иванов Иван (C5). Доставка по адресу: г. Москва, ул. Пушкина, д. 15.	\N
59	2026-10-02 05:11:06.0646	f	INFO	1	LOAN_CREATED_STAFF	L98425	Петров Пётр	C5	iPhone 14 Pro	50000
60	2026-10-01 10:11:06.0646	f	SUCCESS	1	LOAN_AGREED_STAFF	Петров Пётр	C5	L98425	iPhone 14 Pro	45000
61	2026-09-28 10:11:06.0646	t	SUCCESS	1	LOAN_REPAID_STAFF	Петров Пётр	C5	L98425	iPhone 14 Pro	48600,00
62	2026-09-27 10:11:06.0646	t	DANGER	1	LOAN_OVERDUE_STAFF	L11111	Скутер Honda PCX	Смирнов Дмитрий	\N	\N
63	2026-09-23 10:11:06.0646	t	WARNING	1	LOAN_DECLINED_STAFF	Сидоров Алексей	C3	L33333	Телевизор Samsung	\N
64	2026-10-02 08:11:06.0646	f	SUCCESS	2	ORDER_PLACED	M73384	25000,00	M37	Иванов Иван (C5). Доставка по адресу: г. Москва, ул. Пушкина, д. 15.	\N
65	2026-10-02 05:11:06.0646	f	INFO	2	LOAN_CREATED_STAFF	L98425	Петров Пётр	C5	iPhone 14 Pro	50000
66	2026-09-27 10:11:06.0646	t	DANGER	2	LOAN_OVERDUE_STAFF	L11111	Скутер Honda PCX	Смирнов Дмитрий	\N	\N
44	2026-10-02 08:11:06.0646	t	SUCCESS	3	ORDER_PLACED	M73384	25000,00	M37	Доставка по адресу: г. Москва, ул. Пушкина, д. 15.	\N
45	2026-10-02 05:11:06.0646	t	INFO	3	LOAN_CREATED	L98425	iPhone 14 Pro	50000	\N	\N
46	2026-10-01 10:11:06.0646	t	WARNING	3	LOAN_APPROVED	iPhone 14 Pro	50000	45000	8.0	48600,00
47	2026-10-01 10:11:06.0646	t	SUCCESS	3	LOAN_AGREED	L98425	iPhone 14 Pro	Выдан	\N	\N
67	2026-10-02 12:53:17.230587	f	SUCCESS	3	ACCOUNT_DEPOSIT	1		\N	\N	\N
68	2026-10-02 12:53:19.89117	f	SUCCESS	3	ACCOUNT_DEPOSIT	1		\N	\N	\N
69	2026-10-02 12:54:18.926539	f	INFO	3	LOAN_CREATED	L98426	1	100	\N	\N
71	2026-10-02 12:54:18.934058	f	INFO	1	LOAN_CREATED_STAFF	L98426	Клиентов Клиент Клиентович	C12	1	100
72	2026-10-02 12:54:24.258243	f	WARNING	3	LOAN_CANCELLED	L98426	1	\N	\N	\N
70	2026-10-02 12:54:18.93263	t	INFO	2	LOAN_CREATED_STAFF	L98426	Клиентов Клиент Клиентович	C12	1	100
73	2026-10-02 12:55:58.207246	f	INFO	3	ORDER_ASSEMBLED	M42925	Собран	доставка	\N	\N
74	2026-10-02 12:56:00.969759	f	SUCCESS	3	ORDER_DELIVERED	M42925	Доставлен	доставка	\N	\N
75	2026-10-02 14:02:34.07908	f	DANGER	3	ORDER_CANCELLED	M13321	Отменён	доставка	\N	\N
76	2026-10-12 15:47:34.535202	f	INFO	3	ACCOUNT_WITHDRAW	1		\N	\N	\N
77	2026-10-20 14:23:45.570744	f	INFO	3	LOAN_CREATED	L98427	телефон poco x3 pro	10000	\N	\N
78	2026-10-20 14:23:45.592199	f	INFO	2	LOAN_CREATED_STAFF	L98427	Клиентов Клиент Клиентович	C12	телефон poco x3 pro	10000
79	2026-10-20 14:23:45.597586	f	INFO	1	LOAN_CREATED_STAFF	L98427	Клиентов Клиент Клиентович	C12	телефон poco x3 pro	10000
80	2026-10-20 14:33:20.687523	f	WARNING	3	LOAN_CANCELLED	L98427	телефон poco x3 pro	\N	\N	\N
81	2026-10-20 14:33:20.700182	f	WARNING	3	LOAN_CANCELLED	L98427	телефон poco x3 pro	\N	\N	\N
82	2026-10-20 14:33:20.716309	f	WARNING	3	LOAN_CANCELLED	L98427	телефон poco x3 pro	\N	\N	\N
83	2026-10-20 14:34:16.379679	f	INFO	3	LOAN_CREATED	L98428	samsung s21 	30000	\N	\N
84	2026-10-20 14:34:16.393683	f	INFO	2	LOAN_CREATED_STAFF	L98428	Клиентов Клиент Клиентович	C12	samsung s21 	30000
85	2026-10-20 14:34:16.395683	f	INFO	1	LOAN_CREATED_STAFF	L98428	Клиентов Клиент Клиентович	C12	samsung s21 	30000
86	2026-10-20 14:49:37.285083	f	WARNING	3	LOAN_APPROVED	samsung s21	30000.00	30000.00	12	33600.00
87	2026-10-20 14:49:37.291609	f	WARNING	3	LOAN_CHANGED	L98428	Название: «samsung s21 » → «samsung s21».	\N	\N	\N
88	2026-10-20 14:50:18.694361	f	SUCCESS	3	LOAN_AGREED	L98428	samsung s21	Выдан	\N	\N
89	2026-10-20 14:50:18.711975	f	SUCCESS	2	LOAN_AGREED_STAFF	Клиентов Клиент Клиентович	C12	L98428	samsung s21	30000.00
90	2026-10-20 14:50:18.711975	f	SUCCESS	1	LOAN_AGREED_STAFF	Клиентов Клиент Клиентович	C12	L98428	samsung s21	30000.00
92	2026-10-20 15:10:10.731888	f	SUCCESS	3	ORDER_PLACED	M73385	65000.00	M23	Доставка по адресу: г. Москва, ул. Пушкина, д. 67	\N
93	2026-10-20 15:10:10.751781	f	SUCCESS	2	ORDER_PLACED	M73385	65000.00	M23	Клиентов Клиент Клиентович (C12). Доставка по адресу: г. Москва, ул. Пушкина, д. 67	\N
94	2026-10-20 15:10:10.751781	f	SUCCESS	1	ORDER_PLACED	M73385	65000.00	M23	Клиентов Клиент Клиентович (C12). Доставка по адресу: г. Москва, ул. Пушкина, д. 67	\N
91	2026-10-20 15:07:43.831542	t	SUCCESS	3	ACCOUNT_DEPOSIT	100000		\N	\N	\N
95	2026-10-20 18:56:12.364778	f	SUCCESS	3	LOAN_REPAID	L98428	samsung s21	33600.00	\N	\N
96	2026-10-20 18:56:12.381769	f	SUCCESS	2	LOAN_REPAID_STAFF	Клиентов Клиент Клиентович	C12	L98428	samsung s21	33600.00
97	2026-10-20 18:56:12.381769	f	SUCCESS	1	LOAN_REPAID_STAFF	Клиентов Клиент Клиентович	C12	L98428	samsung s21	33600.00
98	2026-10-20 18:57:27.10092	f	INFO	3	LOAN_CREATED	L98429	t	1	\N	\N
99	2026-10-20 18:57:27.115484	f	INFO	2	LOAN_CREATED_STAFF	L98429	Клиентов Клиент Клиентович	C12	t	1
100	2026-10-20 18:57:27.115484	f	INFO	1	LOAN_CREATED_STAFF	L98429	Клиентов Клиент Клиентович	C12	t	1
101	2026-10-20 18:57:49.281904	f	WARNING	3	LOAN_APPROVED	t	1.00	1.00	1	1.01
102	2026-10-20 18:58:10.064859	f	SUCCESS	3	LOAN_AGREED	L98429	t	Выдан	\N	\N
103	2026-10-20 18:58:10.081431	f	SUCCESS	2	LOAN_AGREED_STAFF	Клиентов Клиент Клиентович	C12	L98429	t	1.00
104	2026-10-20 18:58:10.081431	f	SUCCESS	1	LOAN_AGREED_STAFF	Клиентов Клиент Клиентович	C12	L98429	t	1.00
105	2026-10-20 19:30:35.028202	f	INFO	3	ORDER_ASSEMBLED	M73384	Собран	доставка	\N	\N
106	2026-10-21 10:58:15.485454	f	SUCCESS	3	LOAN_REPAID	L98429	t	1.01	\N	\N
107	2026-10-21 10:58:15.529255	f	SUCCESS	2	LOAN_REPAID_STAFF	Клиентов Клиент Клиентович	C12	L98429	t	1.01
108	2026-10-21 10:58:15.532309	f	SUCCESS	1	LOAN_REPAID_STAFF	Клиентов Клиент Клиентович	C12	L98429	t	1.01
109	2026-10-21 11:08:24.931326	f	SUCCESS	3	ACCOUNT_DEPOSIT	40000		\N	\N	\N
110	2026-10-21 11:10:24.908432	f	SUCCESS	3	ACCOUNT_DEPOSIT	300000		\N	\N	\N
111	2026-10-21 11:10:40.003823	f	SUCCESS	3	ACCOUNT_DEPOSIT	300000		\N	\N	\N
112	2026-10-21 11:30:16.321491	f	INFO	3	LOAN_CREATED	L98430	Xiaomi 12	15000	\N	\N
113	2026-10-21 11:30:16.337684	f	INFO	2	LOAN_CREATED_STAFF	L98430	Клиентов Клиент Клиентович	C12	Xiaomi 12	15000
114	2026-10-21 11:30:16.339698	f	INFO	1	LOAN_CREATED_STAFF	L98430	Клиентов Клиент Клиентович	C12	Xiaomi 12	15000
115	2026-10-21 11:37:30.322755	f	WARNING	3	LOAN_APPROVED	Xiaomi 12	15000.00	15000.00	14	17100.00
116	2026-10-21 11:54:46.596671	f	INFO	3	LOAN_CREATED	L98431	dell XPS	120000	\N	\N
117	2026-10-21 11:54:46.611235	f	INFO	2	LOAN_CREATED_STAFF	L98431	Клиентов Клиент Клиентович	C12	dell XPS	120000
118	2026-10-21 11:54:46.611235	f	INFO	1	LOAN_CREATED_STAFF	L98431	Клиентов Клиент Клиентович	C12	dell XPS	120000
119	2026-10-21 11:55:08.377872	f	WARNING	3	LOAN_APPROVED	dell XPS	120000.00	120000.00	13	135600.00
120	2026-10-21 11:55:21.38536	f	WARNING	3	LOAN_APPROVED	dell XPS	120000.00	120000.00	13	135600.00
121	2026-10-21 11:56:31.876232	f	SUCCESS	3	LOAN_AGREED	L98431	dell XPS	Выдан	\N	\N
122	2026-10-21 11:56:31.885749	f	SUCCESS	2	LOAN_AGREED_STAFF	Клиентов Клиент Клиентович	C12	L98431	dell XPS	120000.00
123	2026-10-21 11:56:31.892666	f	SUCCESS	1	LOAN_AGREED_STAFF	Клиентов Клиент Клиентович	C12	L98431	dell XPS	120000.00
\.


--
-- Data for Name: number_sequences; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.number_sequences (prefix, next_value) FROM stdin;
C	31
A	2
M_USER	3
U_C	1
O	73386
L	98432
M_ITEM	887
\.


--
-- Data for Name: order_items; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.order_items (id, order_id, loan_id, item_name, item_price) FROM stdin;
1	1	2	MacBook Air M1 256GB	55000.00
2	2	15	Dyson V15 Detect Vacuum	25000.00
3	3	17	Ковёр персидский	35000.00
4	4	\N	Dyson V15 Detect Vacuum	25000.00
5	5	8	Ноутбук ASUS ROG Strix G15	65000.00
\.


--
-- Data for Name: orders; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.orders (id, client_id, total_amount, status, delivery_type, address_city, address_street, address_building, address_apartment, pickup_address, contact_phone, comment, created_at, order_number) FROM stdin;
1	12	55000.00	Оплачен	DELIVERY	Москва	Пушкина	Колотушкина	67	\N	+79001112233		2026-09-17 14:34:16.930658	M73383
3	12	35000.00	Доставлен	DELIVERY	Москва	Пушкина	Колотушкина	67	\N	+79001112233		2026-09-23 14:46:31.029613	M42925
2	12	25000.00	Отменён	DELIVERY	Москва	Пушкина	Колотушкина	67	\N	+79001112233		2026-09-22 15:29:26.366198	M13321
5	12	65000.00	Оплачен	DELIVERY	Москва	Пушкина	67	512	\N	+79001112233		2026-10-20 15:10:10.715051	M73385
4	12	25000.00	Собран	DELIVERY	Москва	Пушкина	Колотушкина	67	\N	+79001112233		2026-09-27 11:55:30.384514	M73384
\.


--
-- Data for Name: payments; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.payments (id, loan_id, amount, payment_type, method, paid_at, comment, created_by) FROM stdin;
\.


--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.users (id, username, password, role, full_name, email, is_active, created_at, last_login_at, user_number) FROM stdin;
2	manager	$2a$10$KOGfsRw2jBNCNKZVW09kX.kXahZI.Fh7QUrhwj.uQuDbwdpq87LTa	MANAGER	\N	\N	t	2026-09-16 22:16:18.45623	\N	M2
3	client	$2a$10$KOGfsRw2jBNCNKZVW09kX.kXahZI.Fh7QUrhwj.uQuDbwdpq87LTa	CLIENT	\N	\N	t	2026-09-16 22:16:18.45623	\N	C3
1	admin	$2a$10$KOGfsRw2jBNCNKZVW09kX.kXahZI.Fh7QUrhwj.uQuDbwdpq87LTa	ADMIN	Администратор Системы	admin@lombard.ru	t	2026-09-16 19:37:40.816779	\N	A1
\.


--
-- Name: account_transactions_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.account_transactions_id_seq', 23, true);


--
-- Name: accounts_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.accounts_id_seq', 12, true);


--
-- Name: cart_items_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.cart_items_id_seq', 19, true);


--
-- Name: clients_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.clients_id_seq', 30, true);


--
-- Name: collateral_categories_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.collateral_categories_id_seq', 31, true);


--
-- Name: collateral_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.collateral_id_seq', 83, true);


--
-- Name: loan_history_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.loan_history_id_seq', 124, true);


--
-- Name: loans_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.loans_id_seq', 68, true);


--
-- Name: notifications_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.notifications_id_seq', 123, true);


--
-- Name: order_items_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.order_items_id_seq', 5, true);


--
-- Name: orders_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.orders_id_seq', 5, true);


--
-- Name: payments_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.payments_id_seq', 1, false);


--
-- Name: users_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.users_id_seq', 8, true);


--
-- Name: account_transactions account_transactions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.account_transactions
    ADD CONSTRAINT account_transactions_pkey PRIMARY KEY (id);


--
-- Name: accounts accounts_client_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.accounts
    ADD CONSTRAINT accounts_client_id_key UNIQUE (client_id);


--
-- Name: accounts accounts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.accounts
    ADD CONSTRAINT accounts_pkey PRIMARY KEY (id);


--
-- Name: cart_items cart_items_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cart_items
    ADD CONSTRAINT cart_items_pkey PRIMARY KEY (id);


--
-- Name: clients clients_phone_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.clients
    ADD CONSTRAINT clients_phone_key UNIQUE (phone);


--
-- Name: clients clients_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.clients
    ADD CONSTRAINT clients_pkey PRIMARY KEY (id);


--
-- Name: clients clients_user_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.clients
    ADD CONSTRAINT clients_user_id_key UNIQUE (user_id);


--
-- Name: collateral_categories collateral_categories_name_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.collateral_categories
    ADD CONSTRAINT collateral_categories_name_key UNIQUE (name);


--
-- Name: collateral_categories collateral_categories_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.collateral_categories
    ADD CONSTRAINT collateral_categories_pkey PRIMARY KEY (id);


--
-- Name: collateral_categories collateral_categories_slug_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.collateral_categories
    ADD CONSTRAINT collateral_categories_slug_key UNIQUE (slug);


--
-- Name: collateral_photos collateral_photos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.collateral_photos
    ADD CONSTRAINT collateral_photos_pkey PRIMARY KEY (collateral_id, sort_order);


--
-- Name: collateral collateral_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.collateral
    ADD CONSTRAINT collateral_pkey PRIMARY KEY (id);


--
-- Name: loan_history loan_history_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.loan_history
    ADD CONSTRAINT loan_history_pkey PRIMARY KEY (id);


--
-- Name: loans loans_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.loans
    ADD CONSTRAINT loans_pkey PRIMARY KEY (id);


--
-- Name: notifications notifications_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_pkey PRIMARY KEY (id);


--
-- Name: number_sequences number_sequences_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.number_sequences
    ADD CONSTRAINT number_sequences_pkey PRIMARY KEY (prefix);


--
-- Name: order_items order_items_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.order_items
    ADD CONSTRAINT order_items_pkey PRIMARY KEY (id);


--
-- Name: orders orders_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT orders_pkey PRIMARY KEY (id);


--
-- Name: payments payments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT payments_pkey PRIMARY KEY (id);


--
-- Name: cart_items uknp77qoud7glipig5en419flvv; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cart_items
    ADD CONSTRAINT uknp77qoud7glipig5en419flvv UNIQUE (client_id, loan_id);


--
-- Name: cart_items uq_cart_client_loan; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cart_items
    ADD CONSTRAINT uq_cart_client_loan UNIQUE (client_id, loan_id);


--
-- Name: clients uq_client_number; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.clients
    ADD CONSTRAINT uq_client_number UNIQUE (client_number);


--
-- Name: collateral uq_item_number; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.collateral
    ADD CONSTRAINT uq_item_number UNIQUE (item_number);


--
-- Name: loans uq_loan_number; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.loans
    ADD CONSTRAINT uq_loan_number UNIQUE (loan_number);


--
-- Name: orders uq_order_number; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT uq_order_number UNIQUE (order_number);


--
-- Name: users uq_user_number; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT uq_user_number UNIQUE (user_number);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: users users_username_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_username_key UNIQUE (username);


--
-- Name: idx_accounts_client; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_accounts_client ON public.accounts USING btree (client_id);


--
-- Name: idx_cart_client; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_cart_client ON public.cart_items USING btree (client_id);


--
-- Name: idx_clients_email; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_clients_email ON public.clients USING btree (email);


--
-- Name: idx_clients_phone; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_clients_phone ON public.clients USING btree (phone);


--
-- Name: idx_clients_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_clients_user_id ON public.clients USING btree (user_id);


--
-- Name: idx_history_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_history_date ON public.loan_history USING btree (changed_at DESC);


--
-- Name: idx_history_loan; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_history_loan ON public.loan_history USING btree (loan_id);


--
-- Name: idx_loans_client; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_loans_client ON public.loans USING btree (client_id);


--
-- Name: idx_loans_created; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_loans_created ON public.loans USING btree (created_date DESC);


--
-- Name: idx_loans_due; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_loans_due ON public.loans USING btree (due_date);


--
-- Name: idx_loans_overdue; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_loans_overdue ON public.loans USING btree (due_date) WHERE ((status)::text = 'Выдан'::text);


--
-- Name: idx_loans_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_loans_status ON public.loans USING btree (status);


--
-- Name: idx_order_items_order; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_order_items_order ON public.order_items USING btree (order_id);


--
-- Name: idx_orders_client; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_orders_client ON public.orders USING btree (client_id);


--
-- Name: idx_orders_created; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_orders_created ON public.orders USING btree (created_at DESC);


--
-- Name: idx_payments_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payments_date ON public.payments USING btree (paid_at DESC);


--
-- Name: idx_payments_loan; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payments_loan ON public.payments USING btree (loan_id);


--
-- Name: idx_payments_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payments_type ON public.payments USING btree (payment_type);


--
-- Name: idx_tx_account; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_tx_account ON public.account_transactions USING btree (account_id);


--
-- Name: idx_tx_created; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_tx_created ON public.account_transactions USING btree (created_at DESC);


--
-- Name: idx_users_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_users_active ON public.users USING btree (is_active);


--
-- Name: idx_users_role; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_users_role ON public.users USING btree (role);


--
-- Name: idx_users_username; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_users_username ON public.users USING btree (username);


--
-- Name: clients clients_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER clients_updated_at BEFORE UPDATE ON public.clients FOR EACH ROW EXECUTE FUNCTION public.trg_set_updated_at();


--
-- Name: loans loans_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER loans_updated_at BEFORE UPDATE ON public.loans FOR EACH ROW EXECUTE FUNCTION public.trg_set_updated_at();


--
-- Name: account_transactions account_transactions_account_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.account_transactions
    ADD CONSTRAINT account_transactions_account_id_fkey FOREIGN KEY (account_id) REFERENCES public.accounts(id) ON DELETE CASCADE;


--
-- Name: accounts accounts_client_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.accounts
    ADD CONSTRAINT accounts_client_id_fkey FOREIGN KEY (client_id) REFERENCES public.clients(id) ON DELETE CASCADE;


--
-- Name: cart_items cart_items_client_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cart_items
    ADD CONSTRAINT cart_items_client_id_fkey FOREIGN KEY (client_id) REFERENCES public.clients(id) ON DELETE CASCADE;


--
-- Name: cart_items cart_items_loan_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cart_items
    ADD CONSTRAINT cart_items_loan_id_fkey FOREIGN KEY (loan_id) REFERENCES public.loans(id) ON DELETE CASCADE;


--
-- Name: clients clients_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.clients
    ADD CONSTRAINT clients_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: collateral collateral_category_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.collateral
    ADD CONSTRAINT collateral_category_id_fkey FOREIGN KEY (category_id) REFERENCES public.collateral_categories(id) ON DELETE SET NULL;


--
-- Name: collateral collateral_loan_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.collateral
    ADD CONSTRAINT collateral_loan_id_fkey FOREIGN KEY (loan_id) REFERENCES public.loans(id) ON DELETE CASCADE;


--
-- Name: notifications fk9y21adhxn0ayjhfocscqox7bh; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT fk9y21adhxn0ayjhfocscqox7bh FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: collateral_photos fkmtq4mahgo02s9t1nl658x9pe8; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.collateral_photos
    ADD CONSTRAINT fkmtq4mahgo02s9t1nl658x9pe8 FOREIGN KEY (collateral_id) REFERENCES public.collateral(id);


--
-- Name: loan_history loan_history_loan_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.loan_history
    ADD CONSTRAINT loan_history_loan_id_fkey FOREIGN KEY (loan_id) REFERENCES public.loans(id) ON DELETE CASCADE;


--
-- Name: loans loans_client_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.loans
    ADD CONSTRAINT loans_client_id_fkey FOREIGN KEY (client_id) REFERENCES public.clients(id) ON DELETE CASCADE;


--
-- Name: order_items order_items_loan_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.order_items
    ADD CONSTRAINT order_items_loan_id_fkey FOREIGN KEY (loan_id) REFERENCES public.loans(id) ON DELETE SET NULL;


--
-- Name: order_items order_items_order_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.order_items
    ADD CONSTRAINT order_items_order_id_fkey FOREIGN KEY (order_id) REFERENCES public.orders(id) ON DELETE CASCADE;


--
-- Name: orders orders_client_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT orders_client_id_fkey FOREIGN KEY (client_id) REFERENCES public.clients(id) ON DELETE CASCADE;


--
-- Name: payments payments_loan_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT payments_loan_id_fkey FOREIGN KEY (loan_id) REFERENCES public.loans(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--


