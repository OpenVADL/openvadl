typedef int (*func_t)( int, int );

static int driver(int a, int b);

int simple(int a, int b) {
	return a <= b ? a : b;
}

int main( void )
{
    return !(driver( 0, 1 ) == 0 && driver(2, 3) == 2); // => 0
}

static int driver( int a, int b )
{
	func_t f_sub = &simple;
  return f_sub(a, b);
}