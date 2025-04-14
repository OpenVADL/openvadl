// SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package vadl.vdt.impl.regular;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import vadl.vdt.model.DecodeTreeGenerator;
import vadl.vdt.model.Node;
import vadl.vdt.model.impl.LeafNodeImpl;
import vadl.vdt.utils.Bit;
import vadl.vdt.utils.BitPattern;
import vadl.vdt.utils.BitVector;
import vadl.vdt.utils.Instruction;
import vadl.vdt.utils.PBit;

/**
 * Uses the Theiling algorithm to generate a decode tree.
 *
 * <p>See: <a
 * href="https://doi.org/10.1145/384198.384213">Generating Decision Trees For Decoding Binaries (Henrik Theiling)</a>
 *
 * <p>This implementation is limited to fixed-width instructions and does not support overlapping
 * instruction patterns (Except of subsumed instructions, through default nodes).
 */
public class RegularDecodeTreeGenerator implements DecodeTreeGenerator<Instruction> {

  @Override
  public Node generate(Collection<Instruction> instructions) {

    var width = instructions.stream()
        .map(Instruction::width)
        .max(Integer::compareTo)
        .orElseThrow(() -> new IllegalArgumentException("Instructions must not be empty"));

    // Pad instructions to the same width. If this results in overlapping instruction patterns,
    // the algorithm will throw an exception anyway.
    instructions = instructions.stream()
        .map(insn -> padInstruction(insn, width))
        .toList();

    validate(instructions);

    // Initially, the mask is set to consider all bits
    int insnWidth = instructions.iterator().next().width();
    return generateInternal(fullMask(insnWidth), instructions);
  }

  private Node generateInternal(BitVector ggMask, Collection<Instruction> instructions) {

    // Step 1: compute a bit mask of bits that are significant for all patterns. Only the bits
    // set in the gMask are considered.
    BitVector mask = ggMask;
    for (Instruction instruction : instructions) {
      mask = mask.and(mask(instruction));
    }

    // Step 2: possibly terminate: insn set must be a singleton
    if (mask.toValue().equals(BigInteger.ZERO) && instructions.size() == 1) {
      return new LeafNodeImpl(instructions.iterator().next());
    }

    // Step 3: Decide about default node (for subsumed instructions)
    Optional<Node> defaultNode = Optional.empty();
    if (mask.toValue().equals(BigInteger.ZERO)) {
      final var result = getDefault(ggMask, instructions);

      defaultNode = Optional.of(new LeafNodeImpl(result.getLeft()));
      instructions = result.getMiddle();
      mask = result.getRight();
    }

    // Step 4: Partition the instructions based on the mask
    final var partition = partition(mask, instructions);

    // Step 5: Recurse on the partition subsets
    final Map<BitPattern, Node> children = new LinkedHashMap<>();

    for (Map.Entry<BitPattern, Collection<Instruction>> entry : partition.entrySet()) {
      final BitPattern decision = entry.getKey();
      final Collection<Instruction> subset = entry.getValue();

      final var subGMask = ggMask.and(mask.not());
      children.put(decision, generateInternal(subGMask, subset));
    }

    // Step 6: Construct the inner decision node
    return new InnerNodeImpl(mask, children, defaultNode.orElse(null));
  }

  private Map<BitPattern, Collection<Instruction>> partition(BitVector mask,
                                                             Collection<Instruction> instructions) {

    final Map<BitPattern, Collection<Instruction>> partition = new LinkedHashMap<>();

    for (Instruction instruction : instructions) {
      final BitPattern part = partition(mask, instruction);
      partition.computeIfAbsent(part, k -> new ArrayList<>()).add(instruction);
    }

    return partition;
  }

  /**
   * Pads the instruction to the target width by adding don't care bits as least significant bits.
   *
   * @param insn        The instruction to pad
   * @param targetWidth The target width
   * @return The padded instruction, if necessary
   */
  private Instruction padInstruction(Instruction insn, int targetWidth) {
    var pattern = insn.pattern();

    if (pattern.width() >= targetWidth) {
      return insn;
    }

    final PBit[] bits = new PBit[targetWidth];
    for (int i = 0; i < targetWidth; i++) {
      if (i < pattern.width()) {
        bits[i] = pattern.get(i);
      } else {
        bits[i] = new PBit(PBit.Value.DONT_CARE);
      }
    }

    return new Instruction(insn.source(), targetWidth, new BitPattern(bits));
  }

  /**
   * Compute the decision bits for the given instruction based on the mask. Only the set bits in the
   * mask are considered for the decision.
   *
   * @param mask        The mask to use for the decision
   * @param instruction The instruction to compute the decision for
   * @return The decision bits
   */
  private BitPattern partition(BitVector mask, Instruction instruction) {
    final PBit[] decisionBits = new PBit[instruction.width()];
    for (int i = 0; i < instruction.width(); i++) {
      if (mask.get(i).value()) {
        decisionBits[i] = instruction.pattern().get(i);
      } else {
        decisionBits[i] = new PBit(PBit.Value.DONT_CARE);
      }
    }
    return new BitPattern(decisionBits);
  }

  private ImmutableTriple<Instruction, Collection<Instruction>, BitVector> getDefault(
      BitVector ggMask, Collection<Instruction> instructions) {

    // Compute the set of bit patterns that have empty remaining bit masks
    final Set<Instruction> m = new LinkedHashSet<>();
    for (Instruction instruction : instructions) {
      var k = mask(instruction).and(ggMask);
      if (k.toValue().equals(BigInteger.ZERO)) {
        m.add(instruction);
      }
    }

    if (m.size() != 1) {
      throw new IllegalArgumentException("Overlapping instructions found: " + m);
    }

    final var defaultInsn = m.iterator().next();
    final var subsumed = instructions.stream()
        .filter(i -> !Objects.equals(i, defaultInsn))
        .toList();

    // Compute the new mask for the subsumed instructions (Similar to Step 1)
    var newMask = ggMask;
    for (var insn : subsumed) {
      newMask = newMask.and(mask(insn));
    }

    if (newMask.toValue().equals(BigInteger.ZERO)) {
      throw new IllegalArgumentException("Overlapping instructions found: " + subsumed);
    }

    return new ImmutableTriple<>(defaultInsn, subsumed, newMask);
  }

  /**
   * Compute the mask for the given instruction. The mask is a bit vector where each bit is set if
   * the corresponding bit in the instruction is significant.
   *
   * @param instruction The instruction to compute the mask for
   * @return The mask
   */
  private BitVector mask(Instruction instruction) {
    final Bit[] maskBits = new Bit[instruction.width()];
    for (int i = 0; i < instruction.width(); i++) {
      maskBits[i] = new Bit(instruction.pattern().get(i).getValue() != PBit.Value.DONT_CARE);
    }
    return new BitVector(maskBits);
  }

  private BitVector fullMask(int width) {
    final Bit[] bits = new Bit[width];
    for (int i = 0; i < width; i++) {
      bits[i] = new Bit(true);
    }
    return new BitVector(bits);
  }

  /**
   * Validate some basic constraints on the instructions, which are required for the algorithm to
   * work.
   *
   * @param instructions the instructions to validate
   */
  private void validate(Collection<Instruction> instructions) {
    if (instructions.isEmpty()) {
      throw new IllegalArgumentException("Instructions cannot be empty");
    }

    final int expectedWidth = instructions.iterator().next().width();

    for (Instruction instruction : instructions) {
      if (instruction.width() <= 0) {
        throw new IllegalArgumentException("Instruction width must be greater than 0");
      }

      if (instruction.width() != instruction.pattern().width()) {
        throw new IllegalArgumentException(
            "Instruction width must match the fixed bit pattern width");
      }

      if (instruction.width() != expectedWidth) {
        throw new IllegalArgumentException("All instructions must have the same width");
      }
    }
  }
}
