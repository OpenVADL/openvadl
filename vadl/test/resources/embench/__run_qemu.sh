#!/usr/bin/env bash

# Capture the start time in nanoseconds
start_time=$(date +%s%N)

# Execute the command
"$@"
RET=$?

# Capture the end time in nanoseconds
end_time=$(date +%s%N)

# Calculate the elapsed time in seconds with millisecond precision
elapsed_ns=$((end_time - start_time))
elapsed_s=$(echo "scale=3; $elapsed_ns / 1000000000" | bc)

# Format the elapsed time to ensure a leading zero before the decimal point
formatted_time=$(printf "%0.3f" "$elapsed_s")

# Output the formatted elapsed time
echo "TIME=${formatted_time}s"

exit $RET