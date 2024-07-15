package vadl.oop;


import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SymbolTableTest {
  @Test
  void shouldGetLetterA() {
    assertEquals("a", new SymbolTable().getNextVariable());
  }

  @Test
  void shouldGetLetterAA() {
    var symTable = new SymbolTable();

    for (int i = 0; i < 26; i++) {
      symTable.getNextVariable();
    }

    assertEquals("aa", symTable.getNextVariable());
  }

  @Test
  void shouldGetLetterAB() {
    var symTable = new SymbolTable();

    for (int i = 0; i <= 26; i++) {
      symTable.getNextVariable();
    }

    assertEquals("ab", symTable.getNextVariable());
  }

}