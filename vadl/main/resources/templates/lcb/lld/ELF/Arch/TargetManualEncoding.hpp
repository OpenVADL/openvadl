// drop bits outside the range (R, L) == [R, L]
template<std::size_t R, std::size_t L, std::size_t N>
std::bitset<N> project_range(std::bitset<N> b)
{
    static_assert(R <= L && L <= N, "invalid bitrange");
    b >>= R;            // drop R rightmost bits
    b <<= (N - L + R + 1);  // drop L-1 leftmost bits
    b >>= (N - L);      // shift back into place
    return b;
}


[# th:each="function : ${functions}" ]
[(${function})]
[/]
