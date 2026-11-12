/* Copyright (C) 2019 Clemson University

   Contributor Ola Jeppsson <ola.jeppsson@gmail.com>

   This file is part of Embench.

   SPDX-License-Identifier: GPL-3.0-or-later */

#include <support.h>
#include <stdio.h>

/* picolibc's stdio needs the board to provide the standard streams. The simulated board has no
   console, so all output is discarded. */
static int
board_putc (char c, FILE *file)
{
  (void) file;
  return (unsigned char) c;
}

static FILE board_stdio = FDEV_SETUP_STREAM (board_putc, NULL, NULL, _FDEV_SETUP_WRITE);
FILE *const stdin = &board_stdio;
FILE *const stdout = &board_stdio;
FILE *const stderr = &board_stdio;

void
initialise_board ()
{
}

void __attribute__ ((noinline)) __attribute__ ((externally_visible))
start_trigger ()
{
}

void __attribute__ ((noinline)) __attribute__ ((externally_visible))
stop_trigger ()
{
}

// HTIF exit device, like in init.S.
extern volatile unsigned int tohost[2];

// Called by picolibc's exit() and abort().
void __attribute__ ((noreturn))
_exit (int code)
{
  // Same encoding as init.S: (code << 1) | 1 signals exit to the host.
  tohost[0] = ((unsigned int) code << 1) | 1;
  tohost[1] = 0;
  while (1);
}
