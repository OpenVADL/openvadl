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

/* find the index of the last value within the range that is equal to array[index], plus 1 */
long
BinaryLast (const Test array[], const long index, const Range range,
	    const Comparison compare)
{
  long start = range.start, end = range.end - 1;
  while (start < end)
    {
      long mid = start + (end - start) / 2;
      if (!compare (array[index], array[mid]))
	start = mid + 1;
      else
	end = mid;
    }
  if (start == range.end - 1 && !compare (array[index], array[start]))
    start++;
  return start;
}

int main() {
        Comparison cmp = TestCompare;

        // Test 1: Basic case - element with greater value exists
        Test array1[] = {{1, 0}, {2, 1}, {3, 2}, {4, 3}};
        Range r1 = MakeRange(0, 4);
        long result1 = BinaryLast(array1, 1, r1, cmp); // Looking for > 2
        // printf("Test 1: Expected 2, Got %ld\n", result1);

        // Test 2: All values are less than or equal to target
        Test array2[] = {{1, 0}, {2, 1}, {2, 2}};
        Range r2 = MakeRange(0, 3);
        long result2 = BinaryLast(array2, 2, r2, cmp); // No value > 2
        // printf("Test 2: Expected 3, Got %ld\n", result2);

        // Test 3: All values are greater than the target
        Test array3[] = {{3, 0}, {4, 1}, {5, 2}};
        Range r3 = MakeRange(0, 3);
        long result3 = BinaryLast(array3, 0, r3, cmp); // Looking for > 3
        // printf("Test 3: Expected 1, Got %ld\n", result3);

        // Test 4: Target is the maximum in the array
        Test array4[] = {{1, 0}, {2, 1}, {3, 2}};
        Range r4 = MakeRange(0, 3);
        long result4 = BinaryLast(array4, 2, r4, cmp); // Looking for > 3
        // printf("Test 4: Expected 3, Got %ld\n", result4);

        // Test 5: Repeated values in range
        Test array5[] = {{1, 0}, {2, 1}, {2, 2}, {2, 3}, {3, 4}};
        Range r5 = MakeRange(0, 5);
        long result5 = BinaryLast(array5, 1, r5, cmp); // Looking for > 2
        // printf("Test 5: Expected 4, Got %ld\n", result5);

        // Test 6: Single-element range
        Test array6[] = {{5, 0}};
        Range r6 = MakeRange(0, 1);
        long result6 = BinaryLast(array6, 0, r6, cmp); // Looking for > 5
        // printf("Test 6: Expected 1, Got %ld\n", result6);

        // Test 7: All values equal to target
        Test array7[] = {{3, 0}, {3, 1}, {3, 2}};
        Range r7 = MakeRange(0, 3);
        long result7 = BinaryLast(array7, 1, r7, cmp); // Looking for > 3
        // printf("Test 7: Expected 3, Got %ld\n", result7);

        return !(result1 == 2 && result2 == 3 && result3 == 1 && result4 == 3 && result5 == 4 && result6 == 1 && result7 == 3);
}