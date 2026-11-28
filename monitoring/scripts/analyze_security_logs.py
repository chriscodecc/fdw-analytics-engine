import sys
from collections import Counter
import subprocess

WARN_PER_IP = 2
CRIT_PER_IP = 5

def main():
    lines = read_log_lines()
    log_list = []

    for line in lines:
        parsed_line = parse_log_line(line)
        if parsed_line:
            log_list.append(parsed_line)

    failed_attepts_per_ip = Counter (
        d.get("remoteAddr")
        for d in log_list
    )

    attempts_per_target = Counter(
        d.get("uri")
        for d in log_list
    )

    if not failed_attepts_per_ip:
        print("OK - No auth failures found in logs.")
        sys.exit(0)

    top_ip, hits = failed_attepts_per_ip.most_common(1)[0]
    top_uri, uri_hits = (
        attempts_per_target.most_common(1)[0]
        if attempts_per_target
        else ("none", 0)
    )

    print(f"Top IP: {top_ip} ({hits} hits), Top URI: {top_uri} ({uri_hits} hits)")

    if hits > 5:
        sys.exit(2)
    elif hits > 2:
        sys.exit(1)
    else:
        sys.exit(0)


def parse_log_line(line: str) -> dict[str, str]:
    if "event=AUTH_FAILURE" not in line:
        return {}
    
    fields: dict[str, str] = {}

    for token in line.split():
        if "=" in token:
            key, value = token.split("=", 1)
            fields[key] = value

    return fields

def read_log_lines() -> list[str]:
    if not sys.stdin.isatty():
        return list(sys.stdin)

    result = subprocess.run(
        ["docker", "compose", "logs", "--no-color", "backend"],
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
        check=True,
    )
    return result.stdout.splitlines()


if __name__ == "__main__":
    main()