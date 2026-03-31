int main() {
    unsigned int value = 42;
    unsigned int *addr = &value;
    return !(rv32im_LW(addr, 0) == 42);
}
