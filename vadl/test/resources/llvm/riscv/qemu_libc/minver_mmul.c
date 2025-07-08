
static float a[3][3] = {
  {3.0, -6.0, 7.0},
  {9.0, 0.0, -5.0},
  {5.0, -8.0, 6.0},
};

static float b[3][3] = {
  {-3.0, 0.0, 2.0},
  {3.0, -2.0, 0.0},
  {0.0, 2.0, -3.0},
};

static float a[3][3], c[3][3], d[3][3], det;

int mmul (int row_a, int col_a, int row_b, int col_b)
{
  int i, j, k, row_c, col_c;
  float w;

  row_c = row_a;
  col_c = col_b;

  if (row_c < 1 || row_b < 1 || col_c < 1 || col_a != row_b)
    return (999);
  for (i = 0; i < row_c; i++)
    {
      for (j = 0; j < col_c; j++)
        {
          w = 0.0;
          for (k = 0; k < row_b; k++)
            w += a[i][k] * b[k][j];
          c[i][j] = w;
        }
    }

  return (0);
}

int main() {
  mmul (3, 3, 3, 3);

  /*
  for(int i = 0; i < 3; i++) {
    for(int j = 0; j < 3; j++) {
      printf("(%d,%d): %f\n", i, j, a[i][j]);
    }
  }
  */

  return !(
    a[0][0] == 3.0 &&
    a[0][1] == -6.0 &&
    a[0][2] == 7.0 &&
    a[1][0] == 9.0 &&
    a[1][1] == 0.0 &&
    a[1][2] == -5.0 &&
    a[2][0] == 5.0 &&
    a[2][1] == -8.0 &&
    a[2][2] == 6.0
  );
}