#include <stdint.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
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

bool ArrayEquals(const Test a[], const Test b[], Range range) {
  for (long i = range.start; i < range.end; i++) {
    if (a[i].value != b[i].value || a[i].index != b[i].index)
      return 0;
  }
  return 1;
}

/* rotate the values in an array ([0 1 2 3] becomes [1 2 3 0] if we rotate by 1) */
void
Rotate (Test array[], const long amount, const Range range, Test cache[],
	const long cache_size)
{
  long split;
  Range range1, range2;

  if (Range_length (range) == 0)
    return;

  if (amount >= 0)
    split = range.start + amount;
  else
    split = range.end + amount;

  range1 = MakeRange (range.start, split);
  range2 = MakeRange (split, range.end);

  /* if the smaller of the two ranges fits into the cache, it's *slightly* faster copying it there and shifting the elements over */
  if (Range_length (range1) <= Range_length (range2))
    {
      if (Range_length (range1) <= cache_size)
	{
	  memcpy (&cache[0], &array[range1.start],
		  Range_length (range1) * sizeof (array[0]));
	  memmove (&array[range1.start], &array[range2.start],
		   Range_length (range2) * sizeof (array[0]));
	  memcpy (&array[range1.start + Range_length (range2)], &cache[0],
		  Range_length (range1) * sizeof (array[0]));
	  return;
	}
    }
  else
    {
      if (Range_length (range2) <= cache_size)
	{
	  memcpy (&cache[0], &array[range2.start],
		  Range_length (range2) * sizeof (array[0]));
	  memmove (&array[range2.end - Range_length (range1)],
		   &array[range1.start],
		   Range_length (range1) * sizeof (array[0]));
	  memcpy (&array[range1.start], &cache[0],
		  Range_length (range2) * sizeof (array[0]));
	  return;
	}
    }

  Reverse (array, range1);
  Reverse (array, range2);
  Reverse (array, range);
}

int main() {
        Comparison cmp = TestCompare;
        Test cache[10];

        Test array1[] = {{0,0}, {1,1}, {2,2}, {3,3}};
        Test expected1[] = {{1,1}, {2,2}, {3,3}, {0,0}};
        Range r1 = MakeRange(0, 4);
        Rotate(array1, 1, r1, cache, 10);
        //PrintArray(array1, 4);
        //printf("Correct: %s\n\n", ArrayEquals(array1, expected1, r1) ? "Yes" : "No");

        Test array2[] = {{0,0}, {1,1}, {2,2}, {3,3}};
        Test expected2[] = {{3,3}, {0,0}, {1,1}, {2,2}};
        Range r2 = MakeRange(0, 4);
        Rotate(array2, -1, r2, cache, 10);
        //PrintArray(array2, 4);
        //printf("Correct: %s\n\n", ArrayEquals(array2, expected2, r2) ? "Yes" : "No");

        Test array3[] = {{0,0}, {1,1}, {2,2}, {3,3}};
        Test expected3[] = {{0,0}, {1,1}, {2,2}, {3,3}};
        Range r3 = MakeRange(0, 4);
        Rotate(array3, 0, r3, cache, 10);
        //PrintArray(array3, 4);
        //printf("Correct: %s\n\n", ArrayEquals(array3, expected3, r3) ? "Yes" : "No");

        Test array4[] = {{0,0}, {1,1}, {2,2}, {3,3}};
        Test expected4[] = {{0,0}, {1,1}, {2,2}, {3,3}};
        Range r4 = MakeRange(0, 4);
        Rotate(array4, 4, r4, cache, 10);
        //PrintArray(array4, 4);
        //printf("Correct: %s\n\n", ArrayEquals(array4, expected4, r4) ? "Yes" : "No");

        return !(ArrayEquals(array1, expected1, r1) && ArrayEquals(array2, expected2, r2) && ArrayEquals(array3, expected3, r3) && ArrayEquals(array4, expected4, r4));
}