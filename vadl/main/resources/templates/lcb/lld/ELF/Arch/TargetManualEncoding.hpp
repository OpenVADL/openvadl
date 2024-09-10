#include <cstdint>
#include <iostream>
#include <bitset>
#include <vector>
#include <tuple>

template<int start, int end, std::size_t N>
std::bitset<N> project_range(std::bitset<N> bits)
{
    std::bitset<N> result;
    size_t result_index = 0; // Index for the new bitset

    // Extract bits from the range [start, end]
    for (size_t i = start; i <= end; ++i) {
      result[result_index] = bits[i];
    result_index++;
    }

    return result;
}

template<std::size_t N, std::size_t M>
std::bitset<N> set_bits(std::bitset<N> dest, const std::bitset<M> source, std::vector<int> bits) {
    auto target = 0;
    for (int i = bits.size() - 1; i >= 0 ; --i) {
        auto j = bits[target];
        dest.set(j, source[i]);
        target++;
    }

    return dest;
}


[# th:each="function : ${functions}" ]
[(${function})]
[/]
