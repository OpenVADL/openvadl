int recursion( int a )
{
    if( a > 0 )
    {
        return a;
    }

    return recursion( a + 1 );
}