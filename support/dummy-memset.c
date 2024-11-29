#include <string.h>
#include <stdint.h>

void *memset(void *dest, int c, size_t n)
{
	unsigned char *d = dest;

	for (; n; n--) *d++ = c;

	return dest;
}
