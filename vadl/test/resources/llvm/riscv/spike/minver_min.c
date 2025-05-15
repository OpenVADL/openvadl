#include <math.h>
#include <string.h>

#define VERIFY_FLOAT_EPS 1.0e-5
#define float_eq_beebs(exp, actual) (fabsf(exp - actual) < VERIFY_FLOAT_EPS)
#define float_neq_beebs(exp, actual) !float_eq_beebs(exp, actual)

static float a_ref[3][3] = {
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

static float
minver_fabs (float n)
{
  float f;

  if (n >= 0)
    f = n;
  else
    f = -n;
  return f;
}

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

int
minver (int row, int col, float eps)
{
  int work[500], i, j, k, r, iw, u, v;
  float w, wmax, pivot, api, w1;

  r = w = 0;
  if (row < 2 || row > 500 || eps <= 0.0)
    return (999);
  w1 = 1.0;
  for (i = 0; i < row; i++)
    work[i] = i;
  for (k = 0; k < row; k++)
  {
    wmax = 0.0;
    for (i = k; i < row; i++)
    {
      w = minver_fabs (a[i][k]);
      if (w > wmax)
      {
        wmax = w;
        r = i;
      }
    }
    pivot = a[r][k];
    api = minver_fabs (pivot);

    if (api <= eps)
    {
      det = w1;
      return (1);
    }
    w1 *= pivot;
    u = k * col;
    v = r * col;
    if (r != k)
    {
      w1 = -w;
      iw = work[k];
      work[k] = work[r];
      work[r] = iw;
      for (j = 0; j < row; j++)
      {
          w = a[k][j];
          a[k][j] = a[r][j];
          a[r][j] = w;
      }
    }

    for (i = 0; i < row; i++)
      a[k][i] /= pivot;

    for (i = 0; i < row; i++)
    {
      if (i != k)
      {
          v = i * col;
          w = a[i][k];
          if (w != 0.0)
          {
            for (j = 0; j < row; j++)
              if (j != k)
                a[i][j] -= w * a[k][j];
              a[i][k] = -w / pivot;
          }
      }
    }

    a[k][k] = 1.0 / pivot;
  }

  for (i = 0; i < row; i++)
  {
    while (1)
    {
      k = work[i];
      if (k == i)
        break;
      iw = work[k];
      work[k] = work[i];
      work[i] = iw;
      for (j = 0; j < row; j++)
        {
          u = j * col;
          w = a[k][i];
          a[k][i] = a[k][k];
          a[k][k] = w;
        }
    }
  }

  det = w1;

  return (0);
}

int
verify_benchmark ()
{
  int i, j;
  float eps = 1.0e-6;

  static float c_exp[3][3] = {
    {-27.0, 26.0, -15.0},
    {-27.0, -10.0, 33.0},
    {-39.0, 28.0, -8.0}
  };

  static float d_exp[3][3] = {
    {0.133333325, -0.199999958, 0.2666665910},
    {-0.519999862, 0.113333330, 0.5266665220},
    {0.479999840, -0.359999895, 0.0399999917}
  };

  /* Allow small errors in floating point */

  for (i = 0; i < 3; i++)
    for (j = 0; j < 3; j++)
      if (float_neq_beebs(c[i][j], c_exp[i][j]) || float_neq_beebs(d[i][j], d_exp[i][j]))
	      return 0;

  return float_eq_beebs(det, -16.6666718);
}


int main() {

  /*
  for(int i = 0; i < 3; i++) {
    for(int j = 0; j < 3; j++) {
      printf("(%d,%d): %f\n", i, j, a[i][j]);
    }
  }
  */

  float eps = 1.0e-6;
  memcpy (a, a_ref, 3 * 3 * sizeof (a[0][0]));
  minver (3, 3, eps);
  memcpy (d, a, 3 * 3 * sizeof (a[0][0]));
  memcpy (a, a_ref, 3 * 3 * sizeof (a[0][0]));
  mmul (3, 3, 3, 3);

  return verify_benchmark();
}