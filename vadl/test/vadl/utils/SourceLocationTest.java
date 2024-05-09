package vadl.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests the {@link SourceLocation} class. Especially the toSourceString() method.
 */
public class SourceLocationTest {

  private static URI miniVadlUri;

  /**
   * Loads the URI of the VADL test file from the bundled resources.
   */
  @BeforeAll
  public static void setup() throws URISyntaxException {
    miniVadlUri =
        Objects.requireNonNull(SourceLocationTest.class.getResource("/testFiles/mini.vadl"))
            .toURI();
  }


  @Test
  public void testToSourceString_singleLine() {
    SourceLocation location = new SourceLocation(miniVadlUri, 12);
    String expected =
        "  constant MLen   = $ArchSize()           // MLen = 32 or 64 depending on ArchSize";
    assertEquals(expected, location.toSourceString());
  }

  @Test
  public void testToSourceString_multipleLines() {
    SourceLocation location = new SourceLocation(miniVadlUri, 14, 16);
    String expected = "  using Inst     = Bits<32>               // instruction word is 32 bit\n"
        + "  using Regs     = Bits<MLen>             // untyped register word type\n"
        + "  using Bits3    = Bits< 3>               // 3 bit type";
    assertEquals(expected, location.toSourceString());
  }

  @Test
  public void testToSourceString_withColumn() {
    SourceLocation.SourcePosition start = new SourceLocation.SourcePosition(23, 10);
    SourceLocation.SourcePosition end = new SourceLocation.SourcePosition(23, 15);
    SourceLocation location = new SourceLocation(miniVadlUri, start, end);
    String expected = "Rtype";
    assertEquals(expected, location.toSourceString());
  }

  @Test
  public void testToSourceString_multipleLinesWithColumn() {
    SourceLocation.SourcePosition start = new SourceLocation.SourcePosition(33, 3);
    SourceLocation.SourcePosition end = new SourceLocation.SourcePosition(34, 61);
    SourceLocation location = new SourceLocation(miniVadlUri, start, end);
    String expected =
        "instruction ADD : Rtype =                        // 3 register operand instructions\n"
            + "      X(rd) := ((X(rs1) as Bits) + (X(rs2) as Bits)) as Regs";
    assertEquals(expected, location.toSourceString());
  }

  @Test
  public void testToSourceString_whenBeginLineIsZero() {
    SourceLocation location = new SourceLocation(miniVadlUri, 0);
    assert (location.toSourceString().contains("Invalid source location"));
  }

}