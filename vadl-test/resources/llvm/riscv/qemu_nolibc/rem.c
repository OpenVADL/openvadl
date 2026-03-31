int volatile a = 7;

int main() {
  return !(a % 2 == 1);
}