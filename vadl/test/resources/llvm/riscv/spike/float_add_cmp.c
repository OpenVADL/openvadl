#include <math.h>

#define VERIFY_FLOAT_EPS 1.0e-5
#define float_eq_beebs(exp, actual) (fabsf(exp - actual) < VERIFY_FLOAT_EPS)
#define float_neq_beebs(exp, actual) !float_eq_beebs(exp, actual)

int main() {
  float a = 1.0/3.0;
  float b = 1.0/3.0;
    return !(float_eq_beebs(a + b, 2.0/3.0));
}