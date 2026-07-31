from flask import Flask, render_template, request, redirect, url_for
import requests
import os
import json
from typing import Final

app = Flask(__name__)
API_KEY: Final = "l`_Ggpsg[h8eGZZOPCK-;r(p1MQa+aCT"
BACKEND_URL: Final = os.getenv("BACKEND_URL", "https://")