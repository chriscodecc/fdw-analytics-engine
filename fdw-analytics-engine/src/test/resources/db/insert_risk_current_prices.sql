-- 1. Dim Company
INSERT INTO dim_company (company_id, name, symbol, country, industry)
VALUES (1, 'Nikkei225', 'Nikkei225', 'Global', 'Unknown')
ON CONFLICT (company_id) DO NOTHING;

-- 2. Dim Date (last 31 days relative to CURRENT_DATE)
INSERT INTO dim_date (date_id, full_date, year, month, day)
SELECT 
    to_char(d, 'YYYYMMDD')::integer,
    d::date,
    EXTRACT(YEAR FROM d)::integer,
    EXTRACT(MONTH FROM d)::integer,
    EXTRACT(DAY FROM d)::integer
FROM generate_series(CURRENT_DATE - INTERVAL '30 days', CURRENT_DATE, INTERVAL '1 day') AS d
ON CONFLICT (date_id) DO NOTHING;

-- 3. Fact Prices (30 days baseline 100.00, today spiked to 400.00)
INSERT INTO fact_prices (price_id, date_id, company_id, close_price, high_price, low_price, open_price, volume)
SELECT 
    (1000 + row_number() OVER ())::integer AS price_id,
    to_char(d, 'YYYYMMDD')::integer AS date_id,
    1 AS company_id,
    CASE WHEN d::date = CURRENT_DATE THEN 400.0000 ELSE 100.0000 END AS close_price,
    CASE WHEN d::date = CURRENT_DATE THEN 400.0000 ELSE 100.0000 END AS high_price,
    CASE WHEN d::date = CURRENT_DATE THEN 400.0000 ELSE 100.0000 END AS low_price,
    CASE WHEN d::date = CURRENT_DATE THEN 400.0000 ELSE 100.0000 END AS open_price,
    1000 AS volume
FROM generate_series(CURRENT_DATE - INTERVAL '30 days', CURRENT_DATE, INTERVAL '1 day') AS d
ON CONFLICT (price_id) DO NOTHING;