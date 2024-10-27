int arg_spilling
( int a0
, int a1
, int a2
, int a3
, int a4
, int a5
, int a6
, int a7
, int a8 /* --> needs to be spilled */
)
{
    return a0 + a1 + a2 + a3 + a4 + a5 + a6 + a7 + a8;
}

int arg_spilling_call()
{
    return arg_spilling( 1, 2, 3, 4, 5, 6, 7, 8, 9 /* --> needs spilling */);
}