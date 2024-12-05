#include <string.h>

int memcmp(const void *vl, const void *vr, size_t n)
{
  const unsigned char *l = vl;
  const unsigned char *r = vr;

	for (; n; n--) {
    char cl = *l++;
    char cr = *r++;
    if (cl < cr)
      return -1;
    if (cl > cr)
      return 1;
  }

  return 0;
}
