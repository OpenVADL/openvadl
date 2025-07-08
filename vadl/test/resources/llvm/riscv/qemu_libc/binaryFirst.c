#include <stdint.h>
typedef uint8_t bool;

/* structure to test stable sorting (index will contain its original index in the array, to make sure it doesn't switch places with other items) */
typedef struct
{
  int value;
  int index;
} Test;

bool
TestCompare (Test item1, Test item2)
{
  return (item1.value < item2.value);
}

typedef bool (*Comparison) (Test, Test);

/* structure to represent ranges within the array */
typedef struct
{
  long start;
  long end;
} Range;

long
Range_length (Range range)
{
  return range.end - range.start;
}

Range
MakeRange (const long start, const long end)
{
  Range range;
  range.start = start;
  range.end = end;
  return range;
}

typedef long int (* TestCasePtr)(long int, long int);

/* toolbox functions used by the sorter */

/* swap value1 and value2 */
#define Swap(value1, value2, type) { \
        Var(a, &(value1), type*); \
        Var(b, &(value2), type*); \
        \
        Var(c, *a, type); \
        *a = *b; \
        *b = c; \
}

/* 63 -> 32, 64 -> 64, etc. */
/* apparently this comes from Hacker's Delight? */
long
FloorPowerOfTwo (const long value)
{
  long x = value;
  x = x | (x >> 1);
  x = x | (x >> 2);
  x = x | (x >> 4);
  x = x | (x >> 8);
  x = x | (x >> 16);
#if __LP64__
  x = x | (x >> 32);
#endif
  return x - (x >> 1);
}

long
BinaryFirst (const Test array[], const long index, const Range range,
             const Comparison compare)
{
long start = range.start, end = range.end - 1;
  while (start < end)
    {
      long mid = start + (end - start) / 2;
      if (compare (array[mid], array[index]))
        start = mid + 1;
      else
        end = mid;
    }
  if (start == range.end - 1 && compare (array[start], array[index]))
    start++;
  return start;
}

int main() {
        Comparison cmp = TestCompare;

        // Test 1: Value is found in the middle
        Test array1[] = {{1, 0}, {2, 1}, {3, 2}, {3, 3}, {4, 4}};
        Range r1 = MakeRange(0, 5);
        long result1 = BinaryFirst(array1, 3, r1, cmp);
        // printf("Test 1: Expected 2, Got %ld\n", result1);

        // Test 2: All elements are less than the target
        Test array2[] = {{1, 0}, {2, 1}, {3, 2}};
        Range r2 = MakeRange(0, 3);
        long result2 = BinaryFirst(array2, 2, r2, cmp);
        // printf("Test 2: Expected 2, Got %ld\n", result2);

        // Test 3: All elements are greater than the target
        Test array3[] = {{3, 0}, {4, 1}, {5, 2}};
        Range r3 = MakeRange(0, 3);
        long result3 = BinaryFirst(array3, 0, r3, cmp);
        // printf("Test 3: Expected 0, Got %ld\n", result3);

        // Test 4: Target is at the beginning of the range
        Test array4[] = {{2, 0}, {3, 1}, {4, 2}};
        Range r4 = MakeRange(0, 3);
        long result4 = BinaryFirst(array4, 0, r4, cmp);
        // printf("Test 4: Expected 0, Got %ld\n", result4);

        // Test 5: Target value occurs multiple times
        Test array5[] = {{1, 0}, {2, 1}, {3, 2}, {3, 3}, {3, 4}, {4, 5}};
        Range r5 = MakeRange(0, 6);
        long result5 = BinaryFirst(array5, 4, r5, cmp);
        // printf("Test 5: Expected 2, Got %ld\n", result5);

        // Test 6: Single-element range
        Test array6[] = {{5, 0}};
        Range r6 = MakeRange(0, 1);
        long result6 = BinaryFirst(array6, 0, r6, cmp);
        // printf("Test 6: Expected 0, Got %ld\n", result6);

        // Test 7: Range with identical elements
        Test array7[] = {{3, 0}, {3, 1}, {3, 2}, {3, 3}};
        Range r7 = MakeRange(0, 4);
        long result7 = BinaryFirst(array7, 2, r7, cmp);
        // printf("Test 7: Expected 0, Got %ld\n", result7);

        return !(result1 == 2 && result2 == 2 && result3 == 0 && result4 == 0 && result5 == 2 && result6 == 0 && result7 == 0);
}