.text
  LUI X1, 0x10
  ADDI X1, X1, 4095
  LUI X2, 0xff0
  ADDI X2, X2, 255
  LUI X3, 0xf0f1
  ADDI X3, X3, 3855
  LUI X4, 0x33333
  ADDI X4, X4, 819
  PMOVX p0, X1
  PMOVX p1, X2
  PMOVX p2, X3
  PMOVX p3, X4
  LUI X28, 0x6
  ADDI X28, X28, 2649
loop:
  PNEXT p0, p1, p0
  PNEXT p1, p2, p1
  PNEXT p2, p3, p2
  PNEXT p3, p0, p3
  PNEXT p0, p1, p0
  PNEXT p1, p2, p1
  PNEXT p2, p3, p2
  PNEXT p3, p0, p3
  PNEXT p0, p1, p0
  PNEXT p1, p2, p1
  PNEXT p2, p3, p2
  PNEXT p3, p0, p3
  PNEXT p0, p1, p0
  PNEXT p1, p2, p1
  PNEXT p2, p3, p2
  PNEXT p3, p0, p3
  PNEXT p0, p1, p0
  PNEXT p1, p2, p1
  PNEXT p2, p3, p2
  PNEXT p3, p0, p3
  PNEXT p0, p1, p0
  PNEXT p1, p2, p1
  PNEXT p2, p3, p2
  PNEXT p3, p0, p3
  PNEXT p0, p1, p0
  PNEXT p1, p2, p1
  PNEXT p2, p3, p2
  PNEXT p3, p0, p3
  PNEXT p0, p1, p0
  PNEXT p1, p2, p1
  PNEXT p2, p3, p2
  PNEXT p3, p0, p3
  PNEXT p0, p1, p0
  PNEXT p1, p2, p1
  PNEXT p2, p3, p2
  PNEXT p3, p0, p3
  PNEXT p0, p1, p0
  PNEXT p1, p2, p1
  PNEXT p2, p3, p2
  PNEXT p3, p0, p3
  PNEXT p0, p1, p0
  PNEXT p1, p2, p1
  PNEXT p2, p3, p2
  PNEXT p3, p0, p3
  PNEXT p0, p1, p0
  PNEXT p1, p2, p1
  PNEXT p2, p3, p2
  PNEXT p3, p0, p3
  PNEXT p0, p1, p0
  PNEXT p1, p2, p1
  PNEXT p2, p3, p2
  PNEXT p3, p0, p3
  PNEXT p0, p1, p0
  PNEXT p1, p2, p1
  PNEXT p2, p3, p2
  PNEXT p3, p0, p3
  PNEXT p0, p1, p0
  PNEXT p1, p2, p1
  PNEXT p2, p3, p2
  PNEXT p3, p0, p3
  PNEXT p0, p1, p0
  PNEXT p1, p2, p1
  PNEXT p2, p3, p2
  PNEXT p3, p0, p3
  PNEXT p0, p1, p0
  PNEXT p1, p2, p1
  PNEXT p2, p3, p2
  PNEXT p3, p0, p3
  PNEXT p0, p1, p0
  PNEXT p1, p2, p1
  PNEXT p2, p3, p2
  PNEXT p3, p0, p3
  PNEXT p0, p1, p0
  PNEXT p1, p2, p1
  PNEXT p2, p3, p2
  PNEXT p3, p0, p3
  PNEXT p0, p1, p0
  PNEXT p1, p2, p1
  PNEXT p2, p3, p2
  PNEXT p3, p0, p3
  PNEXT p0, p1, p0
  PNEXT p1, p2, p1
  PNEXT p2, p3, p2
  PNEXT p3, p0, p3
  PNEXT p0, p1, p0
  PNEXT p1, p2, p1
  PNEXT p2, p3, p2
  PNEXT p3, p0, p3
  PNEXT p0, p1, p0
  PNEXT p1, p2, p1
  PNEXT p2, p3, p2
  PNEXT p3, p0, p3
  PNEXT p0, p1, p0
  PNEXT p1, p2, p1
  PNEXT p2, p3, p2
  PNEXT p3, p0, p3
  PNEXT p0, p1, p0
  PNEXT p1, p2, p1
  PNEXT p2, p3, p2
  PNEXT p3, p0, p3
  PNEXT p0, p1, p0
  PNEXT p1, p2, p1
  PNEXT p2, p3, p2
  PNEXT p3, p0, p3
  PNEXT p0, p1, p0
  PNEXT p1, p2, p1
  PNEXT p2, p3, p2
  PNEXT p3, p0, p3
  PNEXT p0, p1, p0
  PNEXT p1, p2, p1
  PNEXT p2, p3, p2
  PNEXT p3, p0, p3
  PNEXT p0, p1, p0
  PNEXT p1, p2, p1
  PNEXT p2, p3, p2
  PNEXT p3, p0, p3
  PNEXT p0, p1, p0
  PNEXT p1, p2, p1
  PNEXT p2, p3, p2
  PNEXT p3, p0, p3
  PNEXT p0, p1, p0
  PNEXT p1, p2, p1
  PNEXT p2, p3, p2
  PNEXT p3, p0, p3
  PNEXT p0, p1, p0
  PNEXT p1, p2, p1
  PNEXT p2, p3, p2
  PNEXT p3, p0, p3
  PNEXT p0, p1, p0
  PNEXT p1, p2, p1
  PNEXT p2, p3, p2
  PNEXT p3, p0, p3
  PNEXT p0, p1, p0
  PNEXT p1, p2, p1
  PNEXT p2, p3, p2
  PNEXT p3, p0, p3
  PNEXT p0, p1, p0
  PNEXT p1, p2, p1
  PNEXT p2, p3, p2
  PNEXT p3, p0, p3
  PNEXT p0, p1, p0
  PNEXT p1, p2, p1
  PNEXT p2, p3, p2
  PNEXT p3, p0, p3
  PNEXT p0, p1, p0
  PNEXT p1, p2, p1
  PNEXT p2, p3, p2
  PNEXT p3, p0, p3
  PNEXT p0, p1, p0
  PNEXT p1, p2, p1
  PNEXT p2, p3, p2
  PNEXT p3, p0, p3
  PNEXT p0, p1, p0
  PNEXT p1, p2, p1
  PNEXT p2, p3, p2
  PNEXT p3, p0, p3
  PNEXT p0, p1, p0
  PNEXT p1, p2, p1
  PNEXT p2, p3, p2
  PNEXT p3, p0, p3
  PNEXT p0, p1, p0
  PNEXT p1, p2, p1
  PNEXT p2, p3, p2
  PNEXT p3, p0, p3
  PNEXT p0, p1, p0
  PNEXT p1, p2, p1
  PNEXT p2, p3, p2
  PNEXT p3, p0, p3
  PNEXT p0, p1, p0
  PNEXT p1, p2, p1
  PNEXT p2, p3, p2
  PNEXT p3, p0, p3
  PNEXT p0, p1, p0
  PNEXT p1, p2, p1
  PNEXT p2, p3, p2
  PNEXT p3, p0, p3
  PNEXT p0, p1, p0
  PNEXT p1, p2, p1
  PNEXT p2, p3, p2
  PNEXT p3, p0, p3
  PNEXT p0, p1, p0
  PNEXT p1, p2, p1
  PNEXT p2, p3, p2
  PNEXT p3, p0, p3
  PNEXT p0, p1, p0
  PNEXT p1, p2, p1
  PNEXT p2, p3, p2
  PNEXT p3, p0, p3
  PNEXT p0, p1, p0
  PNEXT p1, p2, p1
  PNEXT p2, p3, p2
  PNEXT p3, p0, p3
  PNEXT p0, p1, p0
  PNEXT p1, p2, p1
  PNEXT p2, p3, p2
  PNEXT p3, p0, p3
  PNEXT p0, p1, p0
  PNEXT p1, p2, p1
  PNEXT p2, p3, p2
  PNEXT p3, p0, p3
  PNEXT p0, p1, p0
  PNEXT p1, p2, p1
  PNEXT p2, p3, p2
  PNEXT p3, p0, p3
  PNEXT p0, p1, p0
  PNEXT p1, p2, p1
  PNEXT p2, p3, p2
  PNEXT p3, p0, p3
  PNEXT p0, p1, p0
  PNEXT p1, p2, p1
  PNEXT p2, p3, p2
  PNEXT p3, p0, p3
  PNEXT p0, p1, p0
  PNEXT p1, p2, p1
  PNEXT p2, p3, p2
  PNEXT p3, p0, p3
  PNEXT p0, p1, p0
  PNEXT p1, p2, p1
  PNEXT p2, p3, p2
  PNEXT p3, p0, p3
  PNEXT p0, p1, p0
  PNEXT p1, p2, p1
  PNEXT p2, p3, p2
  PNEXT p3, p0, p3
  PNEXT p0, p1, p0
  PNEXT p1, p2, p1
  PNEXT p2, p3, p2
  PNEXT p3, p0, p3
  PNEXT p0, p1, p0
  PNEXT p1, p2, p1
  PNEXT p2, p3, p2
  PNEXT p3, p0, p3
  PNEXT p0, p1, p0
  PNEXT p1, p2, p1
  PNEXT p2, p3, p2
  PNEXT p3, p0, p3
  PNEXT p0, p1, p0
  PNEXT p1, p2, p1
  PNEXT p2, p3, p2
  PNEXT p3, p0, p3
  PNEXT p0, p1, p0
  PNEXT p1, p2, p1
  PNEXT p2, p3, p2
  PNEXT p3, p0, p3
  PNEXT p0, p1, p0
  PNEXT p1, p2, p1
  PNEXT p2, p3, p2
  PNEXT p3, p0, p3
  PNEXT p0, p1, p0
  PNEXT p1, p2, p1
  PNEXT p2, p3, p2
  PNEXT p3, p0, p3
  PNEXT p0, p1, p0
  PNEXT p1, p2, p1
  PNEXT p2, p3, p2
  PNEXT p3, p0, p3
  ADDI X28, X28, 4095
  BEQ X28, X0, 4
  JAL X0, 1048060
loop_done:
  LUI X20, 0x80010
  ADDI X20, X20, 0
  PST p0, (X20)
  LUI X20, 0x80010
  ADDI X20, X20, 4
  PST p1, (X20)
  LUI X20, 0x80010
  ADDI X20, X20, 8
  PST p2, (X20)
  LUI X20, 0x80010
  ADDI X20, X20, 12
  PST p3, (X20)
  LUI X20, 0x80010
  ADDI X20, X20, 0
  ADDI X21, X0, 2
  LUI X22, 0x80010
  ADDI X22, X22, 24
  LD X22, 0(X22)
  LUI X23, 0x9e378
  ADDI X23, X23, 2481
checksum_loop:
  LD X24, 0(X20)
  MUL X22, X22, X23
  ADD X22, X22, X24
  ADDI X20, X20, 8
  ADDI X21, X21, 4095
  BNE X21, X0, 4086
  LUI X25, 0x80010
  ADDI X25, X25, 16
  LD X26, 0(X25)
  BNE X22, X26, 12
  LUI X27, 0x80020
  ADDI X27, X27, 0
  ADDI X24, X0, 1
  SD X24, 0(X27)
spin_success:
  JAL X0, 0
checksum_fail:
  LUI X27, 0x80020
  ADDI X27, X27, 0
  ADDI X24, X0, 3
  SD X24, 0(X27)
spin_fail:
  JAL X0, 0
