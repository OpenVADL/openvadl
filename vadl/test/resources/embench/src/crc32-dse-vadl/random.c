#include "random.h"

int random_numbers[MAX_RANDOM_NUMS] = {0};

void srand_precomputed(unsigned int seed) {
  for(int i = 0; i < MAX_RANDOM_NUMS; i++) {
    seed = (seed * 1103515245L + 12345) & ((1UL << 31) - 1);
    random_numbers[i] = (int) (seed >> 16);
  }
}
