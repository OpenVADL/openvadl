#!/usr/bin/env python3

"""Copy a GraalVM JFR recording for IntelliJ parsers that do not recognize AOT frames.

Only the constant-pool frame label changes: AOT compiled -> JIT compiled. This is
a viewer compatibility workaround, not an indication that the code was JIT compiled.
Samples, timestamps, method names, descriptors, and line numbers are preserved.
"""

import argparse
from pathlib import Path


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("recording", type=Path)
    parser.add_argument("output", type=Path, nargs="?",
                        help="defaults to <recording stem>-intellij.jfr; must not exist")
    args = parser.parse_args()
    output = args.output or args.recording.with_name(args.recording.stem + "-intellij.jfr")
    data = args.recording.read_bytes()
    if not data.startswith(b"FLR\x00"):
        parser.error("input is not a JFR recording")

    # JFR UTF-8 string encoding (3), byte length (12), then the frame type label.
    # Equal-length replacement preserves chunk sizes and constant-pool offsets.
    old = b"\x03\x0cAOT compiled"
    new = b"\x03\x0cJIT compiled"
    count = data.count(old)
    if not count:
        parser.error("no AOT frame type labels found; no output written")
    try:
        with output.open("xb") as destination:
            destination.write(data.replace(old, new))
    except FileExistsError:
        parser.error(f"output already exists: {output}")
    print(f"Wrote {output} ({count} AOT frame type label(s) adapted for IntelliJ)")


if __name__ == "__main__":
    main()
