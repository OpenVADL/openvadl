// Exercise RV32 quad-precision libcalls and incoming/outgoing indirect arguments.
// Volatile inputs and noinline keep the operations and calls present at O3.
__attribute__((noinline)) long double multiply(long double a, long double b) {
  return a * b;
}

// Exhaust the argument registers so the pointer to the wide argument is passed
// on the stack. Check both the wide value and the surrounding narrow values.
__attribute__((noinline)) int stacked(int a, int b, int c, int d,
                                     int e, int f, int g, int h,
                                     long double x, int tail) {
  return a + b + c + d + e + f + g + h == 36 && x == 6.0L && tail == 9;
}

__attribute__((noinline)) double narrow(double a, double b) {
  return a + b;
}

// va_arg must consume one pointer slot for the wide value, then read the tail.
__attribute__((noinline)) int variadic(int tag, ...) {
  __builtin_va_list ap;
  __builtin_va_start(ap, tag);
  long double value = __builtin_va_arg(ap, long double);
  int tail = __builtin_va_arg(ap, int);
  __builtin_va_end(ap);
  return tag == 1 && value == 6.0L && tail == 9;
}

int main(void) {
  volatile long double a = 2.0L;
  volatile long double b = 3.0L;
  volatile double c = 2.0;
  volatile double d = 3.0;
  long double product = multiply(a, b);
  return product != 6.0L
         || !stacked(1, 2, 3, 4, 5, 6, 7, 8, product, 9)
         || !variadic(1, product, 9)
         || narrow(c, d) != 5.0;
}
