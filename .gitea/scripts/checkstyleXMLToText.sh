#!/bin/bash
# This script transform the checkstyle xml report to a plain text
# representation that can be send to a pull request on evaluation

# Check for input argument
if [ $# -eq 0 ]; then
    echo "Usage: $0 path_to_checkstyle_xml"
    exit 1
fi

# Path to Checkstyle XML result
INPUT_PATH="$1"

# Check if the path exists
if [ ! -f "$INPUT_PATH" ]; then
    echo "Error: File does not exist at the specified path."
    exit 1
fi

echo "Checkstyle Report:"
xmlstarlet sel -T -t \
    -m "//file" -v "concat('File: ', @name)" -n \
    -m ".//error" -v "concat('    Line ',@line, ':	',@severity, ': ',  @message)" -n \
    -b -n "$INPUT_PATH"
