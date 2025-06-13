void
mulul64 (unsigned long long u, unsigned long long v, unsigned long long * whi, unsigned long long * wlo)
{
  unsigned long long u0, u1, v0, v1, k, t;
  unsigned long long w0, w1, w2;

  u1 = u >> 32;
  u0 = u & 0xFFFFFFFF;
  v1 = v >> 32;
  v0 = v & 0xFFFFFFFF;

  t = u0 * v0;
  w0 = t & 0xFFFFFFFF;
  k = t >> 32;

  t = u1 * v0 + k;
  w1 = t & 0xFFFFFFFF;
  w2 = t >> 32;

  t = u0 * v1 + w1;
  k = t >> 32;

  *wlo = (t << 32) + w0;
  *whi = u1 * v1 + w2 + k;

  return;
}

int main() {
  unsigned long long whi;
  unsigned long long wlo;

  mulul64(0xfae849273928f89fLL, 0x14736defb9330573LL, &whi, &wlo);

  return !(whi == 1444327063251828292 && wlo == 1544293776228338285);
}