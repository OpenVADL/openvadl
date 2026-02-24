/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation, or (at your option) any
 * later version. See the COPYING file in the top-level directory.
 */

#ifndef MYTARGET_TARGET_ELF_H
#define MYTARGET_TARGET_ELF_H

#define ELF_MACHINE  EM_RISCV

#define ELF_CLASS    ELFCLASS64

/* * for reporting CPU features to ELF loader
 * Set to 0 for now
 */
#define HAVE_ELF_HWCAP 0

#endif
