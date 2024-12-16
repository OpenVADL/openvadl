int option1() {
  return 0;
}
int option2() {
  return 1;
}
int option3() {
  return 2;
}

// Function pointer type
typedef int (*JumpFunction)();

int main() {
    // Array of function pointers (jump table)
    JumpFunction jumpTable[] = {
        option1,   // Index 0
        option2,   // Index 1
        option3    // Index 2
    };

    return !(jumpTable[0] == 0 && jumpTable[1] == 1 && jumpTable[2] == 2);
}