#ifndef STUB_STDIO_H
#define STUB_STDIO_H
#include <stddef.h>
#ifndef NULL
#define NULL ((void *)0)
#endif
int printf(const char *, ...);
int puts(const char *);
#endif

