#ifndef _VADL_RANDOM_H_
#define _VADL_RANDOM_H_

#define MAX_RANDOM_NUMS 1024

extern int random_numbers[MAX_RANDOM_NUMS];

void srand_precomputed(unsigned int seed);

#endif
