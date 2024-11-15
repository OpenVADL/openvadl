void
jpegdct (short *d, short *r)
{
  long int t[12];
  short i, j, k, m, n, p;
  for (k = 1, m = 0, n = 13, p = 8; k <= 8;
       k += 7, m += 3, n += 3, p -= 7, d -= 64)
    {
      for (i = 0; i < 8; i++, d += p)
	{
	  for (j = 0; j < 4; j++)
	    {
	      t[j] = d[k * j] + d[k * (7 - j)];
	      t[7 - j] = d[k * j] - d[k * (7 - j)];
	    }
	  t[8] = t[0] + t[3];
	  t[9] = t[0] - t[3];
	  t[10] = t[1] + t[2];
	  t[11] = t[1] - t[2];
	  d[0] = (t[8] + t[10]) >> m;
	  d[4 * k] = (t[8] - t[10]) >> m;
	  t[8] = (short) (t[11] + t[9]) * r[10];
	  d[2 * k] = t[8] + (short) ((t[9] * r[9]) >> n);
	  d[6 * k] = t[8] + (short) ((t[11] * r[11]) >> n);
	  t[0] = (short) (t[4] + t[7]) * r[2];
	  t[1] = (short) (t[5] + t[6]) * r[0];
	  t[2] = t[4] + t[6];
	  t[3] = t[5] + t[7];
	  t[8] = (short) (t[2] + t[3]) * r[8];
	  t[2] = (short) t[2] * r[1] + t[8];
	  t[3] = (short) t[3] * r[3] + t[8];
	  d[7 * k] = (short) (t[4] * r[4] + t[0] + t[2]) >> n;
	  d[5 * k] = (short) (t[5] * r[6] + t[1] + t[3]) >> n;
	  d[3 * k] = (short) (t[6] * r[5] + t[1] + t[2]) >> n;
	  d[1 * k] = (short) (t[7] * r[7] + t[0] + t[3]) >> n;
	}
    }
}