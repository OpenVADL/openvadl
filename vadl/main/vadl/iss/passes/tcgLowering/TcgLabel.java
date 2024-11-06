package vadl.iss.passes.tcgLowering;

public record TcgLabel(String varName) {

  @Override
  public String toString() {
    return varName;
  }
}