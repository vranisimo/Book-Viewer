--
-- PostgreSQL database dump
--

-- Dumped from database version 14.0 (Debian 14.0-1.pgdg110+1)
-- Dumped by pg_dump version 14.0

-- Started on 2021-11-07 03:44:50

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


-- TOC entry 3331 (class 1262 OID 16385)
-- Name: book_viewer; Type: DATABASE; Schema: -; Owner: postgres
--

CREATE DATABASE book_viewer WITH TEMPLATE = template0 ENCODING = 'UTF8' LOCALE = 'en_US.utf8';


ALTER DATABASE book_viewer OWNER TO postgres;

\connect book_viewer

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
-- TOC entry 209 (class 1259 OID 16386)
-- Name: book_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.book_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    MAXVALUE 2147483647
    CACHE 1;


ALTER TABLE public.book_id_seq OWNER TO postgres;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- TOC entry 210 (class 1259 OID 16387)
-- Name: book; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.book (
    id integer DEFAULT nextval('public.book_id_seq'::regclass) NOT NULL,
    isbn character varying(13) NOT NULL,
    page_count integer NOT NULL,
    processed_page_count integer NOT NULL,
    is_processed boolean GENERATED ALWAYS AS ((page_count = processed_page_count)) STORED NOT NULL
);


ALTER TABLE public.book OWNER TO postgres;

--
-- TOC entry 213 (class 1259 OID 16418)
-- Name: user_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.user_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    MAXVALUE 2147483647
    CACHE 1;


ALTER TABLE public.user_id_seq OWNER TO postgres;

--
-- TOC entry 212 (class 1259 OID 16411)
-- Name: user; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public."user" (
    id integer DEFAULT nextval('public.user_id_seq'::regclass) NOT NULL,
    username character varying(64) NOT NULL,
    email character(128) NOT NULL,
    password character varying(64) NOT NULL
);


ALTER TABLE public."user" OWNER TO postgres;

--
-- TOC entry 3178 (class 2606 OID 16400)
-- Name: book book_id_unique; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.book
    ADD CONSTRAINT book_id_unique UNIQUE (id);


--
-- TOC entry 3180 (class 2606 OID 16402)
-- Name: book book_isbn_unique; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.book
    ADD CONSTRAINT book_isbn_unique UNIQUE (isbn);


--
-- TOC entry 3182 (class 2606 OID 16404)
-- Name: book book_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.book
    ADD CONSTRAINT book_pkey PRIMARY KEY (id);


--
-- TOC entry 3175 (class 2606 OID 16405)
-- Name: book isbn_length; Type: CHECK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE public.book
    ADD CONSTRAINT isbn_length CHECK ((length((isbn)::text) = 13)) NOT VALID;


--
-- TOC entry 3184 (class 2606 OID 16415)
-- Name: user user_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."user"
    ADD CONSTRAINT user_pkey PRIMARY KEY (id);


--
-- TOC entry 3186 (class 2606 OID 16417)
-- Name: user username_unique; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."user"
    ADD CONSTRAINT username_unique UNIQUE (username);


-- Completed on 2021-11-07 03:44:50

--
-- PostgreSQL database dump complete
--

