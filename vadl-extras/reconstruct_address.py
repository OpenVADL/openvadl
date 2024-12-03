#!/usr/bin/env python3

# This script allows reconstructing the virtual addressed based
# on the accessed first- and second-level page tables.
# It also calculates the page table entry in case the virtual address should
# match the physical one.
def reconstruct(l1addr, l2addr):
    r1 = (l1addr >> 2) & 0x3FF
    r2 = (l2addr >> 2) & 0x3FF
    
    og_addr = (r1 << 10) | r2
    pte = og_addr << 2
    print("Original address:", hex(og_addr))
    print("PTE:", hex(pte))
    print("MEM<4>( 0x{:x} ) := 0x{:X}0F".format(l2addr, pte))
