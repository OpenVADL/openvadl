#!/usr/bin/env python3

# Python module to run programs natively.

# Copyright (C) 2019 Clemson University
#
# Contributor: Ola Jeppsson <ola.jeppsson@gmail.com>
#
# This file is part of Embench.

# SPDX-License-Identifier: GPL-3.0-or-later

"""
Embench module to run benchmark programs.

This version is suitable for running programs using a simulator.
All arguments passed to the script will be forwarded to the given simulator program.
The benchmark program will be passed as a final argument. In case your simulator requires
additional arguments after the benchmark argument, you can provide a double-dash ('--') to pass
on any further parameters.

The script expects the execution time to be printed on stdout in the format 'TIME={seconds}.{milliseconds}'.
"""

__all__ = [
    'get_target_args',
    'build_benchmark_cmd',
    'decode_results',
]

import argparse
from itertools import groupby
import re
import os

from embench_core import log


def get_target_args(remnant):
    """Parse left over arguments"""
    parser = argparse.ArgumentParser(description='Get target specific args')
    
    parser.add_argument("remaining", nargs=argparse.REMAINDER)

    # No target arguments
    return parser.parse_args(remnant)


def build_benchmark_cmd(bench, args):
    """Construct the command to run the benchmark.  "args" is a
       namespace with target specific arguments"""

    # Due to way the target interface currently works we need to construct
    # a command that records both the return value and execution time to
    # stdin/stdout. Obviously using time will not be very precise.
    sep_args = [list(group) for k, group in groupby(args.remaining, lambda x: x == "--") if not k]   
    before_args = sep_args[0]
    if len(sep_args) == 2:
        after_args = sep_args[1]
    else:
        after_args = []

    arg = before_args + [bench] + after_args
    return arg


def decode_results(stdout_str, stderr_str):
    """Extract the results from the output string of the run. Return the
       elapsed time in milliseconds or zero if the run failed."""
    # Match "real s.mm?m?"
    time = re.search(r'^TIME=(\d+)\.(\d+)', stdout_str, re.M)
    if time:
        ms_elapsed = int(time.group(1)) * 1000 + \
                     int(time.group(2).ljust(3,'0')) # 0-pad
        # Return value cannot be zero (will be interpreted as error)
        return max(float(ms_elapsed), 0.001)
    
    # We must have failed to find a time
    log.debug('Warning: Failed to find timing')
    return 0.0
