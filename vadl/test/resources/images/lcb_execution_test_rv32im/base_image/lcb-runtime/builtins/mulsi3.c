/*
 * 32-bit multiplication for targets without a multiply instruction.
 *
 * The compiler calls __mulsi3 for every 32-bit multiplication it cannot select. compiler-rt only
 * provides it as RISC-V assembly (riscv/mulsi3.S), which the generated assembler cannot parse.
 * The loop only uses shifts and additions, so it does not call itself.
 */

unsigned int __mulsi3(unsigned int a, unsigned int b);

unsigned int __mulsi3(unsigned int a, unsigned int b) {
  unsigned int result = 0;
  while (a) {
    if (a & 1)
      result += b;
    a >>= 1;
    b <<= 1;
  }
  return result;
}

