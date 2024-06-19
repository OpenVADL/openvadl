package vadl.viam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;


public class ConstantTests {


  // Bitslice tests

  @Test
  public void bitSliceSize() {
    Constant.BitSlice.Part part = new Constant.BitSlice.Part(5, 1);
    assertEquals(5, part.size());
  }

  @Test
  public void bitSliceIsIndex() {
    Constant.BitSlice.Part part = new Constant.BitSlice.Part(1, 1);
    assertTrue(part.isIndex());
  }

  @Test
  public void bitSliceIsRange() {
    Constant.BitSlice.Part part = new Constant.BitSlice.Part(5, 1);
    assertTrue(part.isRange());
  }

  @Test
  public void bitSliceJoin() {
    Constant.BitSlice.Part part1 = new Constant.BitSlice.Part(5, 1);
    Constant.BitSlice.Part part2 = new Constant.BitSlice.Part(3, 0);
    assertEquals(new Constant.BitSlice.Part(5, 0), part1.join(part2));
  }

  @Test
  public void bitSliceWithin() {
    Constant.BitSlice.Part part1 = new Constant.BitSlice.Part(5, 1);
    Constant.BitSlice.Part part2 = new Constant.BitSlice.Part(3, 2);
    assertTrue(part2.isSurroundedBy(part1));
  }

  @Test
  public void bitSliceNotWithin() {
    Constant.BitSlice.Part part1 = new Constant.BitSlice.Part(5, 1);
    Constant.BitSlice.Part part2 = new Constant.BitSlice.Part(6, 2);
    assertFalse(part2.isSurroundedBy(part1));
  }

  @Test
  public void bitSliceJoinNonOverlapping() {
    Constant.BitSlice.Part part1 = new Constant.BitSlice.Part(5, 3);
    Constant.BitSlice.Part part2 = new Constant.BitSlice.Part(2, 0);
    assertEquals(new Constant.BitSlice.Part(5, 0), part1.join(part2));
  }

  @Test
  public void bitSliceOverlappingOverlapping() {
    Constant.BitSlice.Part part1 = new Constant.BitSlice.Part(5, 3);
    Constant.BitSlice.Part part2 = new Constant.BitSlice.Part(3, 0);
    assertTrue(part1.isOverlapping(part2));
  }

  @Test
  public void bitSliceOverlappingOverlapping2() {
    Constant.BitSlice.Part part1 = new Constant.BitSlice.Part(5, 0);
    Constant.BitSlice.Part part2 = new Constant.BitSlice.Part(3, 1);
    assertTrue(part1.isOverlapping(part2));
  }

  @Test
  public void bitSliceOverlappingNonOverlapping() {
    Constant.BitSlice.Part part1 = new Constant.BitSlice.Part(5, 3);
    Constant.BitSlice.Part part2 = new Constant.BitSlice.Part(2, 0);
    assertFalse(part1.isOverlapping(part2));
  }

  @Test
  public void bitSliceSingleIndexSize() {
    Constant.BitSlice.Part part = new Constant.BitSlice.Part(1, 1);
    assertEquals(1, part.size());
  }

  @Test
  public void bitSliceCreationInvalidArguments() {
    assertThrows(ViamError.class, () -> new Constant.BitSlice.Part(1, 5));
    assertThrows(ViamError.class, () -> new Constant.BitSlice.Part(-1, 0));
  }

  @Test
  public void bitSliceCreationNormalized() {
    Constant.BitSlice bitSlice = new Constant.BitSlice(new Constant.BitSlice.Part[] {
        new Constant.BitSlice.Part(5, 3),
        new Constant.BitSlice.Part(2, 0)}
    );
    assertEquals(1, bitSlice.partSize());
    assertEquals(6, bitSlice.bitSize());
    var parts = bitSlice.parts().toList();
    assertEquals(new Constant.BitSlice.Part(5, 0), parts.get(0));

    var bitPositions = StreamSupport.stream(bitSlice.spliterator(), false).toList();
    assertEquals(List.of(5, 4, 3, 2, 1, 0), bitPositions);
  }

  @Test
  public void bitSliceCreationNormalized2() {
    Constant.BitSlice bitSlice = new Constant.BitSlice(new Constant.BitSlice.Part[] {
        new Constant.BitSlice.Part(0, 0),
        new Constant.BitSlice.Part(7, 5),
        new Constant.BitSlice.Part(4, 4),
        new Constant.BitSlice.Part(3, 2),
        new Constant.BitSlice.Part(9, 8)
    }
    );
    assertEquals(3, bitSlice.partSize());
    assertEquals(9, bitSlice.bitSize());
    assertEquals(List.of(
        new Constant.BitSlice.Part(0, 0),
        new Constant.BitSlice.Part(7, 2),
        new Constant.BitSlice.Part(9, 8)
    ), bitSlice.parts().toList());

    var bitPositions = StreamSupport.stream(bitSlice.spliterator(), false).toList();
    assertEquals(List.of(0, 7, 6, 5, 4, 3, 2, 9, 8), bitPositions);

  }

}
