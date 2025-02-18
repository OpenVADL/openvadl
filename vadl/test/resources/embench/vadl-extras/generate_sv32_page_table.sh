#!/usr/bin/env bash

cat page-table.txt | sed -E "s/.*0x([0-9a-f]*)'([0-9a-f]*).*0x([0-9a-f]*)'([0-9a-f]*).*/\tli a0, 0x\1\2\n\tli a1, 0x\3\4\n\tsw a1, \(a0\)/I"
