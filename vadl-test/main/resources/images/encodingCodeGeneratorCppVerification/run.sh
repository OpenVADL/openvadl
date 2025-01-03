#!/bin/bash

set -e
set -x

for FILE in /inputs/*; do
    g++ -Wall "$FILE" -o /tmp/a.out && /tmp/a.out
    STATUS_CODE=$?

    # Write the file name and status code to the CSV file
    echo "$(basename "$FILE"),$STATUS_CODE" >> /work/output.csv
done