int main() {
    unsigned long long A = (2 << 30) + 1337;
    unsigned long long B = (2 << 30) + 1337;
    return !(A == B);
}