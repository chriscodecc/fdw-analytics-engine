CREATE TABLE IF NOT EXISTS dim_date (
    date_id INTEGER PRIMARY KEY,
    full_date DATE NOT NULL,
    day INTEGER NOT NULL,
    month INTEGER NOT NULL,
    year INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS dim_company (
    company_id INTEGER PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    symbol VARCHAR(50) NOT NULL,
    country VARCHAR(100),
    industry VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS fact_prices (
    price_id INTEGER PRIMARY KEY,
    date_id INTEGER NOT NULL,
    company_id INTEGER NOT NULL,
    close_price NUMERIC(12, 4) NOT NULL,
    high_price NUMERIC(12, 4),
    low_price NUMERIC(12, 4),
    open_price NUMERIC(12, 4),
    volume BIGINT,
    CONSTRAINT fk_fact_prices_date FOREIGN KEY (date_id) REFERENCES dim_date (date_id),
    CONSTRAINT fk_fact_prices_company FOREIGN KEY (company_id) REFERENCES dim_company (company_id)
);