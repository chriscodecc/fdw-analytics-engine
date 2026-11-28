import json
import urllib.parse
import urllib.request
import sys

def main(auth_failure_query, request_rate_query):

    try:
        auth_failure_prom = query_prom(auth_failure_query)
        request_rate_prom = query_prom(request_rate_query)
        code = exit_code(auth_failure_prom, request_rate_prom)

        status = {0: "OK", 1: "WARNING", 2: "CRITICAL"}.get(code, "UNKNOWN")
        print(f"{status} - Auth Failures: {auth_failure_prom}, Req Rate: {request_rate_prom}")

    except Exception as e:
        print(f"UNKNOWN - Prometheus query failed: {e}")
        code = 3

    sys.exit(code)


AUTH_FAILURE_QUERY = "sum(increase(security_auth_failures_total[5m]))"
REQUEST_RATE_QUERY = 'sum(rate(http_server_requests_seconds_count{uri!~"/actuator.*"}[1m]))'

if __name__ == "__main__":
    main(AUTH_FAILURE_QUERY, REQUEST_RATE_QUERY)
    


def query_prom(query: str) -> float:
    params = urllib.parse.urlencode({"query": query})
    url = f"http://prometheus:9090/api/v1/query?{params}"

    with urllib.request.urlopen(url, timeout=5) as response:
        raw_json = response.read().decode("utf-8")

    payload = json.loads(raw_json)
    results = payload.get("data", {}).get("result", [])
    if not results:
        return 0.0

    return float(results[0]["value"][1])


def exit_code(auth_failures: float, requests_rate: float) -> int:
   
    if auth_failures > 5 or requests_rate > 10:
        return 2
    elif auth_failures > 2 or requests_rate > 5:
        return 1
    else:
        return 0


 

     