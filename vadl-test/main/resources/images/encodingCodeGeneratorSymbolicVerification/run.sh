#!/bin/bash

for FILE in /inputs/*; do
    python3 "$FILE"
    STATUS_CODE=$?

    # Write the file name and status code to the CSV file
    echo "$(basename "$FILE"),$STATUS_CODE" >> /work/output.csv
done