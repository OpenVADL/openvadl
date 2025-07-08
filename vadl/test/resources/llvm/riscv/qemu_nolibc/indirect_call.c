typedef int (*func_t)( int );

static int driver( int a);

int simple(int a) {
	return a;
}

int main( void )
{
    return !(driver( 0 ) == 0 && driver(2) == 2); // => 0
}

static int driver( int a )
{
	func_t f_sub = &simple;
  return f_sub(a);
}