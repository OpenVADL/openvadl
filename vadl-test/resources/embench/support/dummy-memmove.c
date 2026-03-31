#include <stdint.h>
#include <stddef.h>

void *memmove(void *dest, const void *src, size_t n)
{
    unsigned char *d = dest;
    const unsigned char *s = src;

    if ((uintptr_t)d < (uintptr_t)s) {          /* safe comparison */
        while (n--) *d++ = *s++;
    } else if ((uintptr_t)d > (uintptr_t)s) {
        d += n;  s += n;
        while (n--) *--d = *--s;
    }
    return dest;
}