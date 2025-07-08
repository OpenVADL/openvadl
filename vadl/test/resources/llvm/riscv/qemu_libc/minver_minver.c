
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

int main() {
  float eps = 0.0001;
  minver (3, 3, eps);

  /*
  for(int i = 0; i < 3; i++) {
    for(int j = 0; j < 3; j++) {
      printf("(%d,%d): %f\n", i, j, a[i][j]);
    }
  }
  */

  return !(
    minver_fabs(a[0][0] - 0.133333) <= eps &&
    minver_fabs(a[0][1] - -0.2) <= eps &&
    minver_fabs(a[0][2] - 0.266667) <= eps &&
    minver_fabs(a[1][0] - -0.52) <= eps &&
    minver_fabs(a[1][1] - 0.113333) <= eps &&
    minver_fabs(a[1][2] - 0.526667) <= eps &&
    minver_fabs(a[2][0] - 0.48) <= eps &&
    minver_fabs(a[2][1] - -0.36) <= eps &&
    minver_fabs(a[2][2] - 0.04) <= eps &&
    minver_fabs(det - -16.6666718) <= eps
  );
}