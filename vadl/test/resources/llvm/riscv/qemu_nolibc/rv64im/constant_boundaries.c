// Expected values are loaded from data so a faulty instruction sequence cannot
// produce the same wrong constant on both sides of a comparison.
volatile unsigned long input = ~0UL;
volatile unsigned long zero = 0;
volatile unsigned long expected[] = {
  0x7ffff7ffUL, 0x7ffff800UL, 0x7fffffffUL, 0x80000000UL, 0xffffffffUL
};

__attribute__((noinline)) unsigned long mask31(unsigned long x) {
  return x & 0x7fffffffUL;
}
__attribute__((noinline)) unsigned long boundary_below(unsigned long x) {
  return x ^ 0x7ffff7ffUL;
}
__attribute__((noinline)) unsigned long boundary_carry(unsigned long x) {
  return x ^ 0x7ffff800UL;
}
__attribute__((noinline)) unsigned long mask32(unsigned long x) {
  return x & 0xffffffffUL;
}
__attribute__((noinline)) unsigned long unsigned_high_bit(unsigned long x) {
  return x ^ 0x80000000UL;
}

int main(void) {
  return boundary_below(zero) != expected[0]
         || boundary_carry(zero) != expected[1]
         || mask31(input) != expected[2]
         || unsigned_high_bit(zero) != expected[3]
         || mask32(input) != expected[4];
}
