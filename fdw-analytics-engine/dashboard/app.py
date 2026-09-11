import os
from typing import Final

import streamlit as st
import requests

base_url_riskEvalu = st.secrets["backend"]["base_url"] + "/api/v1/riskEvaluation"

headers = {
    "Content-Type": "application/json; charset=utf-8",
    "API_KEY": st.secrets["backend"]["api_key"]
}

response = requests.get(
        base_url_riskEvalu + "/risklevel",
        headers=headers,
        params={"companySymbol": "DAX"},
        verify=False 
)
st.text(base_url_riskEvalu)
st.text(response.json())






