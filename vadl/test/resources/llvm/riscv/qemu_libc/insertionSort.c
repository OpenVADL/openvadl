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

/* n^2 sorting algorithm used to sort tiny chunks of the full array */
void
InsertionSort (Test array[], const Range range, const Comparison compare)
{
  long i;
  for (i = range.start + 1; i < range.end; i++)
    {
      const Test temp = array[i];
      long j;
      for (j = i; j > range.start && compare (temp, array[j - 1]); j--)
        array[j] = array[j - 1];
      array[j] = temp;
    }
}

bool IsSorted(const Test array[], const Range range, Comparison cmp) {
  for (long i = range.start + 1; i < range.end; ++i) {
    if (cmp(array[i], array[i - 1])) return 0;
  }
  return 1;
}

int main() {
        Comparison cmp = TestCompare;

        Test array1[] = {{1,0}, {2,1}, {3,2}};
        Range r1 = MakeRange(0, 3);
        InsertionSort(array1, r1, cmp);

        Test array2[] = {{3,0}, {2,1}, {1,2}};
        Range r2 = MakeRange(0, 3);
        InsertionSort(array2, r2, cmp);

        Test array3[] = {{2,0}, {1,1}, {2,2}, {1,3}};
        Range r3 = MakeRange(0, 4);
        InsertionSort(array3, r3, cmp);

        Test array4[] = {{5,0}};
        Range r4 = MakeRange(0, 1);
        InsertionSort(array4, r4, cmp);

        Test array5[] = {{5,0}, {4,1}};
        Range r5 = MakeRange(1, 1); // no sorting actually happens
        InsertionSort(array5, r5, cmp);

        return !(IsSorted(array1, r1, cmp) && IsSorted(array2, r2, cmp) && IsSorted(array3, r3, cmp) && IsSorted(array4, r4, cmp) && IsSorted(array5, r5, cmp));
}