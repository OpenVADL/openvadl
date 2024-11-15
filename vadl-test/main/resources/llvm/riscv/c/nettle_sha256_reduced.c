#define READ_UINT32(p)                          \
	(  (((int) (p)[0]) << 24)                  \
	   | (((int) (p)[1]) << 16)                  \
	   | (((int) (p)[2]) << 8)                   \
	   |  ((int) (p)[3]))

#define SHA256_DATA_LENGTH 16

void
_nettle_sha256_compress (int * state, const int * input,
		const int * k)
{
	int data[SHA256_DATA_LENGTH];
	int A, B, C, D, E, F, G, H;	/* Local vars */
	unsigned i; unsigned int  *d;

	for (i = 0; i < SHA256_DATA_LENGTH; i++, input += 4)
	{
		data[i] = READ_UINT32 (input);
	}

	int i1 = 0 & 15;
	int i2 = ((0)-15) & 15;

	data[i1] += (((data[i2])<<(25)) | ((data[i2])>>((-(25)&31))));
}