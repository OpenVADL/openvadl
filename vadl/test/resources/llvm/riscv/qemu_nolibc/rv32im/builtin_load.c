int main() {
    int value = 42;
    int addr = (int) &value;
    return !(rv32im_LW(addr, 0) == 42);
}