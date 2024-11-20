int main()
{
    volatile int a = 1;
    return !((a + 2147483640) == 2147483641);
}