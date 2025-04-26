#!/bin/bash

# Default settings
NUM_REQUESTS=1000
MAX_PARALLEL=10
TIMEOUT=10

# Parse command line arguments
while getopts "n:p:t:" opt; do
  case $opt in
    n) NUM_REQUESTS=$OPTARG ;;
    p) MAX_PARALLEL=$OPTARG ;;
    t) TIMEOUT=$OPTARG ;;
    \?) echo "Invalid option: -$OPTARG" >&2; exit 1 ;;
  esac
done

# Create a temporary file to store the token (safer than having it in the script)
TOKEN_FILE=$(mktemp)
echo "eyJhbGciOiJIUzI1NiIsImtpZCI6IkR0Ulp6Um9JQm5YdlJnL3IiLCJ0eXAiOiJKV1QifQ.eyJpc3MiOiJodHRwczovL2tsc3l2Y2ZwcWdjZ3hpcm9wanZlLnN1cGFiYXNlLmNvL2F1dGgvdjEiLCJzdWIiOiIwODA4OWJjYy1hOTczLTRiMDItYmY3Mi00YzE4MWE0NDYyNDYiLCJhdWQiOiJhdXRoZW50aWNhdGVkIiwiZXhwIjoxNzQ1NjgyNTQ4LCJpYXQiOjE3NDU2Nzg5NDgsImVtYWlsIjoicGxhdGZvcm1vd25lckBza3V0dGVyLmFpIiwicGhvbmUiOiIiLCJhcHBfbWV0YWRhdGEiOnsicHJvdmlkZXIiOiJlbWFpbCIsInByb3ZpZGVycyI6WyJlbWFpbCJdLCJza3V0dGVyX3JvbGUiOiJQTEFURk9STV9PV05FUiJ9LCJ1c2VyX21ldGFkYXRhIjp7ImVtYWlsX3ZlcmlmaWVkIjp0cnVlfSwicm9sZSI6ImF1dGhlbnRpY2F0ZWQiLCJhYWwiOiJhYWwxIiwiYW1yIjpbeyJtZXRob2QiOiJwYXNzd29yZCIsInRpbWVzdGFtcCI6MTc0NTY3ODk0OH1dLCJzZXNzaW9uX2lkIjoiM2Y4ZjU1ZTUtNzQxNS00YjhlLTg5ZWMtYWM1MWJlZDhmNDdmIiwiaXNfYW5vbnltb3VzIjpmYWxzZX0.UpcziYn_pYZuwnUvJVIXmYV7rpTV6VPPi5M53YXC1vs" > "$TOKEN_FILE"

# URL to call
URL="https://localhost:8443/1.0/skutter-project-service/api/logging"

# Results file
RESULTS_FILE=$(mktemp)

# Function for a single curl request
make_request() {
  local request_id=$1
  local start_time=$(date +%s.%N)
  
  # Make the request and capture status code
  status_code=$(curl -X 'GET' \
    "$URL" \
    -H 'accept: application/json' \
    -H "Authorization: Bearer $(cat $TOKEN_FILE)" \
    -k \
    -s \
    -o /dev/null \
    -w "%{http_code}" \
    -m $TIMEOUT)
  
  local end_time=$(date +%s.%N)
  local duration=$(echo "$end_time - $start_time" | bc)
  
  # Log the result
  echo "$request_id,$status_code,$duration" >> "$RESULTS_FILE"
}

# Print start message
echo "Starting $NUM_REQUESTS requests to $URL ($MAX_PARALLEL at a time)"
echo "Press Ctrl+C to stop"

# Record start time
TOTAL_START=$(date +%s.%N)

# Launch requests in parallel with a maximum limit
active_processes=0
for ((i=1; i<=$NUM_REQUESTS; i++)); do
  # Wait if we have too many active processes
  while [ $active_processes -ge $MAX_PARALLEL ]; do
    active_processes=$(jobs -p | wc -l)
    sleep 0.1
  done
  
  # Start a new request in the background
  make_request $i &
  
  # Update count of active processes
  active_processes=$(jobs -p | wc -l)
  
  # Show progress
  if [ $((i % 10)) -eq 0 ] || [ $i -eq $NUM_REQUESTS ]; then
    echo -ne "Progress: $i/$NUM_REQUESTS requests launched\r"
  fi
done

# Wait for all background processes to finish
wait

# Record end time
TOTAL_END=$(date +%s.%N)
TOTAL_TIME=$(echo "$TOTAL_END - $TOTAL_START" | bc)

# Process results
SUCCESS_COUNT=$(grep -c ",200," "$RESULTS_FILE" || true)
TOO_MANY_COUNT=$(grep -c ",429," "$RESULTS_FILE" || true)
ERROR_COUNT=$((NUM_REQUESTS - SUCCESS_COUNT))
REQUESTS_PER_SECOND=$(echo "scale=2; $NUM_REQUESTS / $TOTAL_TIME" | bc)

# Calculate statistics if bc is available
if command -v bc &> /dev/null; then
  # Calculate average, min, and max times
  if [ -s "$RESULTS_FILE" ]; then
    AVG_TIME=$(awk -F, '{sum+=$3} END {print sum/NR}' "$RESULTS_FILE")
    MIN_TIME=$(sort -t, -k3,3n "$RESULTS_FILE" | head -1 | cut -d, -f3)
    MAX_TIME=$(sort -t, -k3,3n "$RESULTS_FILE" | tail -1 | cut -d, -f3)
    
    AVG_TIME=$(printf "%.3f" $AVG_TIME)
    MIN_TIME=$(printf "%.3f" $MIN_TIME)
    MAX_TIME=$(printf "%.3f" $MAX_TIME)
  else
    AVG_TIME="N/A"
    MIN_TIME="N/A"
    MAX_TIME="N/A"
  fi
else
  AVG_TIME="N/A (bc command not available)"
  MIN_TIME="N/A"
  MAX_TIME="N/A"
fi

# Print summary
echo
echo "===== Summary ====="
echo "Total requests: $NUM_REQUESTS"
echo "Successful (HTTP 200): $SUCCESS_COUNT"
echo "Too Many Requests (HTTP 429): $TOO_MANY_COUNT"
echo "Failed: $ERROR_COUNT"
echo "Total time: $TOTAL_TIME seconds"
echo "Requests per second: $REQUESTS_PER_SECOND"
echo "Average response time: $AVG_TIME seconds"
echo "Minimum response time: $MIN_TIME seconds"
echo "Maximum response time: $MAX_TIME seconds"

# Clean up
rm -f "$TOKEN_FILE" "$RESULTS_FILE"

echo "Done!"
