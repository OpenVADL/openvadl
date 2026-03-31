int main() {
    unsigned int value = 0;
    unsigned int *addr = &value;

    rv32im_SW(addr, 42, 0);
    return !(value == 42);
}
