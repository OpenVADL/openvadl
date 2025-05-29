#include <string.h>
#include <stdint.h>

size_t strlen(const char *s) {
    size_t len = 0;
    while (*s++) len++;
    return len;
}

char *strchr(const char *s, int c) {
    while (*s) {
        if (*s == (char)c) return (char *)s;
        s++;
    }
    return NULL;
}

const unsigned char _ctype_[256] = { /* initialize as needed */ };