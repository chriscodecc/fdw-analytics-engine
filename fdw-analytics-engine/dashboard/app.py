import os
from typing import Final

import streamlit as st
import requests
import pandas as pd
import plotly.graph_objects as go
from plotly.subplots import make_subplots
#.\\.venv\Scripts\Activate.ps1

BASE_STR = "/api/v1"
BASE_URL_RISK = st.secrets["backend"]["base_url"] + BASE_STR + "/riskEvaluation"
BASE_URL_ANALYTICS = st.secrets["backend"]["base_url"] + BASE_STR + "/analytics"

HEADERS = {
    "Content-Type": "application/json; charset=utf-8",
    "API_KEY": st.secrets["backend"]["api_key"]
}

def get_response(base_url: str, path: str, params: dict | None = None) -> dict | list | float | None:
    """Performs HTTP GET requests with custom user error messaging."""
    url = base_url + path
    try:
        response = requests.get(
                url,
                headers=HEADERS,
                params=params,
                verify=False 
            )

        if response.status_code == 404:
            st.error(f"Resource not found (404) at `{path}`. Check symbol or endpoint.")
            st.stop()
        elif response.status_code == 401 or response.status_code == 403:
            st.error("Authentication failed. Check your API key.")
            st.stop()
        elif response.status_code >= 500:
            st.error(f"Backend service error ({response.status_code}). Check backend engine logs.")
            st.stop()

        response.raise_for_status()
        return response.json()

    except requests.exceptions.ConnectionError:
        st.error(f"Failed to connect to backend at `{base_url}`. Is the Spring Boot service running?")
        st.stop()
    except requests.exceptions.Timeout:
        st.error("Request timed out waiting for the backend analytics engine.")
        st.stop()
    except Exception as e:
        st.error(f"Unexpected error: {str(e)}")
        st.stop()

def render_metrics(closing_prices: list, sma_volume: float, period: int, risk_level: str, volume: float):
    """Renders the top summary metric cards."""    
    r1_col1, r1_col2 = st.columns(2)
    with r1_col1:
        st.metric("Latest Close Price", f"${closing_prices[-1]:,.2f}")
    with r1_col2:
        st.metric(f"{period}-Day Rolling Average", f"${sma_volume:,.2f}")

    r2_col1, r2_col2 = st.columns(2)
    with r2_col1:
        st.metric("Risk Level", risk_level)
    with r2_col2:
        st.metric("Latest Trading Volume", f"{volume / 1_000_000:.2f}M")

def build_chart(fact_prices_df: pd.DataFrame, rolling_avg_list: list, show_sma: bool, show_volume: bool):
    """Assembles and formats the dual-axis Plotly financial chart."""
    fig = make_subplots(
        rows=2,
        cols=1,
        shared_xaxes=True,
        vertical_spacing=0.03,
        row_heights=[0.7, 0.3],
    )

    # Price Line
    fig.add_trace(
        go.Scatter(
            x=fact_prices_df["fullDate"],
            y=fact_prices_df["closePrice"],
            name="Closing Price",
            line=dict(color="#00E5FF", width=2),
        ),
        row=1,
        col=1,
    )

    # Rolling Average Line
    if show_sma:
        fig.add_trace(
            go.Scatter(
                x=fact_prices_df["fullDate"],
                y=rolling_avg_list,
                name="Rolling Average",
                line=dict(color="#FF5252", width=2),
            ),
            row=1,
            col=1,
        )

    # Volume Bars
    if show_volume:
        fig.add_trace(
            go.Bar(
                x=fact_prices_df["fullDate"],
                y=fact_prices_df["volume"],
                name="Volume",
                marker_color="#00E5FF",
                opacity=0.7,
            ),
            row=2,
            col=1,
        )

    fig.update_layout(
        template="plotly_dark",
        paper_bgcolor="rgba(0,0,0,0)",
        plot_bgcolor="rgba(0,0,0,0)",
        margin=dict(l=20, r=20, t=20, b=20),
        hovermode="x unified",
        legend=dict(orientation="h", yanchor="bottom", y=1.02, xanchor="right", x=1),
    )

    fig.update_yaxes(autorange=True, rangemode="normal", row=1, col=1)
    fig.update_yaxes(rangemode="tozero", row=2, col=1)
    fig.update_xaxes(rangebreaks=[dict(bounds=["sat", "mon"])])
    fig.update_traces(connectgaps=True, selector=dict(type="scatter"))

    st.plotly_chart(fig, use_container_width=True)

def main():
    st.set_page_config(layout="wide")

    # 1. Fetch available symbols
    companies_data = get_response(BASE_URL_ANALYTICS, "/all")
    if not companies_data:
        st.warning("No companies returned from the analytics engine.")
        st.stop()

    name_list = [item["symbol"] for item in companies_data]

    # 2. Sidebar controls
    with st.sidebar:
        st.title("Configuration")
        company = st.selectbox("Company", name_list)
        period = st.selectbox("Period", [30, 90, 120])
        simple_mavg_on = st.checkbox("SMA", value=True)
        volume_on = st.checkbox("Volume", value=True)

    # 3. Fetch analytics data
    fact_prices = get_response(BASE_URL_ANALYTICS, "/factPrices", {"companySymbol": company, "period": period})
    if not fact_prices:
        st.warning(f"No price history found for {company}.")
        st.stop()

    sma_data = get_response(BASE_URL_ANALYTICS, "/sma", {"companySymbol": company})
    risk_data = get_response(BASE_URL_RISK, "/risklevel", {"companySymbol": company})
    rolling_data = get_response(BASE_URL_ANALYTICS, "/avg30", {"companySymbol": company, "period": period})

    # 4. Extract series
    closing_prices = [item["closePrice"] for item in fact_prices]
    rolling_avg_list = [item.get("rollingAvg30") for item in rolling_data]
    fact_prices_df = pd.DataFrame(fact_prices)
    latest_volume = fact_prices[-1].get("volume", 0)
    risk_level = risk_data.get("overallRiskLevel", "UNKNOWN")

    # 5. Render dashboard
    render_metrics(closing_prices, sma_data, period, risk_level, latest_volume)
    build_chart(fact_prices_df, rolling_avg_list, simple_mavg_on, volume_on)


if __name__ == "__main__":
    main()