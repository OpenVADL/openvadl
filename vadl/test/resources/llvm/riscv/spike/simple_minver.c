#include <math.h>
// #include <string.h>

#define VERIFY_FLOAT_EPS 1.0e-5
#define float_eq_beebs(exp, actual) (fabsf(exp - actual) < VERIFY_FLOAT_EPS)
#define float_neq_beebs(exp, actual) !float_eq_beebs(exp, actual)

static float a_ref[3][3] = {
  {3.0, -6.0, 7.0},
  {9.0, 0.0, -5.0},
  {5.0, -8.0, 6.0},
};

static float a[3][3];

void *memcpy(float *dest, float *src, int n) {
  int i;

  for(i = 0; i < n / sizeof(dest[0][0]); i++) {
    *dest = *src;
    dest++;
    src++;
  }
}

int main() {
  memcpy (a, a_ref, 3 * 3 * sizeof (a[0][0]));
  a[0][0] = fabsf (a[0][0]);
  return !float_eq_beebs(3.0, a[0][0]);
}