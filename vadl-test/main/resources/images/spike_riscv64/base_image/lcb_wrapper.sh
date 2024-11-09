#!/usr/bin/env bash

set -x

/opt/riscv-cross/bin/riscv64-unknown-linux-gnu-gcc -static -nostartfiles -T/helper/link_lcbw.ld /tmp/main.s /helper/init.s /helper/trap.s /helper/vars.spike.s /helper/common.s -o /tmp/main

echo "Running spike..."
/opt/spike/bin/spike --isa=rv64im /tmp/main