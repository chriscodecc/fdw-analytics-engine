#!/bin/sh
WARN=80
CRIT=90
QUERY='sum(jvm_memory_used_bytes{area="heap"})/sum(jvm_memory_max_bytes{area="heap"})*100'

RESPONSE=$(curl -sG "http://prometheus:9090/api/v1/query" --data-urlencode "query=${QUERY}")
VAL=$(echo "$RESPONSE" | grep -o '"value":\[[^]]*\]' | awk -F'"' '{print $4}')

if [ -z "$VAL" ]; then
  echo "UNKNOWN - Could not read JVM heap metric from Prometheus"
  exit 3
fi

STATUS=$(awk -v v="$VAL" -v w="$WARN" -v c="$CRIT" 'BEGIN {
  if (v+0 >= c+0) print 2;
  else if (v+0 >= w+0) print 1;
  else print 0;
}')

case "$STATUS" in
  0) echo "OK - JVM Heap usage is ${VAL}% | heap_used_pct=${VAL}%;${WARN};${CRIT};0;100"; exit 0 ;;
  1) echo "WARNING - JVM Heap usage is ${VAL}% | heap_used_pct=${VAL}%;${WARN};${CRIT};0;100"; exit 1 ;;
  2) echo "CRITICAL - JVM Heap usage is ${VAL}% | heap_used_pct=${VAL}%;${WARN};${CRIT};0;100"; exit 2 ;;
esac