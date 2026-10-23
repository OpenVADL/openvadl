_Static_assert(sizeof(__PTRDIFF_TYPE__) == sizeof(void *), "ptrdiff_t width");
_Static_assert(sizeof(__INTPTR_TYPE__) == sizeof(void *), "intptr_t width");
_Static_assert(sizeof(__UINTPTR_TYPE__) == sizeof(void *), "uintptr_t width");

// A conditional combining an int with a pointer difference exposed mismatched
// AST and LLVM integer types on RV64 (the reduced slre frontend crash).
__attribute__((noinline)) __PTRDIFF_TYPE__
difference(int use_fallback, int fallback, const char *begin, const char *end) {
  return use_fallback ? fallback : end - begin;
}

int main(void) {
  char data[16];
  volatile int choose = 0;
  __INTPTR_TYPE__ address = (__INTPTR_TYPE__)data;
  return difference(choose, 123, data, data + 12) != 12
         || difference(choose, 123, data + 12, data) != -12
         || difference(!choose, 123, data, data + 12) != 123
         || (char *)address != data;
}
