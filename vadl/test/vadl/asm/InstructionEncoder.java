// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.asm;

import static vadl.utils.GraphUtils.getSingleNode;

import java.math.BigInteger;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import vadl.types.BuiltInTable;
import vadl.types.DataType;
import vadl.viam.Constant;
import vadl.viam.Encoding;
import vadl.viam.Format;
import vadl.viam.Instruction;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.SignExtendNode;
import vadl.viam.graph.dependency.SliceNode;
import vadl.viam.graph.dependency.TruncateNode;
import vadl.viam.graph.dependency.ZeroExtendNode;
import vadl.viam.passes.canonicalization.Canonicalizer;

/**
 * Encodes instructions from a VIAM ISA using explicit operand names.
 *
 * <p>This class is intentionally small and explicit: it resolves an instruction by name, accepts a
 * list of named operands, derives raw format-field assignments, and encodes the instruction word.
 * It does not parse assembly text or manage sections/symbols.
 *
 * <p>Typical usage:
 *
 * <pre>{@code
 * var isa = TestUtils.compileToViam(specSource).isa().orElseThrow();
 * var encoder = new InstructionEncoder(isa, ByteOrder.LITTLE_ENDIAN);
 *
 * int addi = encoder.encode32("ADDI",
 *     InstructionEncoder.Operand.of("rd", 1),
 *     InstructionEncoder.Operand.of("rs1", 1),
 *     InstructionEncoder.Operand.of("imm", 1));
 * }</pre>
 */
public final class InstructionEncoder {

  public record Operand(String name, long value) {

    public Operand {
      if (name == null || name.isBlank()) {
        throw new IllegalArgumentException("Operand name must not be blank");
      }
    }

    public static Operand of(String name, long value) {
      return new Operand(name, value);
    }
  }

  private final InstructionSetArchitecture isa;
  private final ByteOrder byteOrder;
  private final Map<String, InstructionSchema> schemasByInstructionName = new LinkedHashMap<>();

  public InstructionEncoder(InstructionSetArchitecture isa, ByteOrder byteOrder) {
    this.isa = isa;
    this.byteOrder = byteOrder;
    isa.ownInstructions().forEach(instruction ->
        schemasByInstructionName.put(instruction.simpleName(), InstructionSchema.of(instruction)));
  }

  public Set<String> operandNames(String instructionName) {
    return schema(instructionName).operandNames();
  }

  public int encode32(String instructionName, Operand... operands) {
    var instruction = instruction(instructionName);
    if (instruction.format().type().bitWidth() != 32) {
      throw new IllegalArgumentException(
          "Instruction `%s` is not 32-bit wide.".formatted(instructionName));
    }
    return encode(instructionName, operands).toBits().unsignedInteger().intValue();
  }

  public Constant.Value encode(String instructionName, Operand... operands) {
    var schema = schema(instructionName);
    var assignment = new LinkedHashMap<Format.Field, BigInteger>();
    seedConstantFields(schema.instruction().encoding(), assignment);

    var seenNames = new LinkedHashSet<String>();
    for (var operand : operands) {
      if (!seenNames.add(operand.name())) {
        throw new IllegalArgumentException(
            "Duplicate operand `%s` for instruction `%s`."
                .formatted(operand.name(), instructionName));
      }
      var binder = schema.bindersByName().get(operand.name());
      if (binder == null) {
        throw new IllegalArgumentException(
            "Unknown operand `%s` for instruction `%s`. Known operands: %s"
                .formatted(operand.name(), instructionName, schema.operandNames()));
      }
      binder.bind(operand.value(), assignment);
    }

    var missing = Arrays.stream(schema.instruction().format().fields())
        .filter(field -> !assignment.containsKey(field))
        .map(Format.Field::simpleName)
        .toList();
    if (!missing.isEmpty()) {
      throw new IllegalArgumentException(
          "Missing operands for instruction `%s`: %s"
              .formatted(instructionName, missing));
    }

    var encoding = BigInteger.ZERO;
    for (var field : schema.instruction().format().fields()) {
      var value = assignment.get(field);
      var destPos = field.bitSlice().stream().boxed().toArray(Integer[]::new);
      for (int i = 0; i < field.size(); i++) {
        var pos = field.size() - i - 1;
        if (value.testBit(pos)) {
          encoding = encoding.setBit(destPos[i]);
        }
      }
    }

    return Constant.Value.fromInteger(encoding, schema.instruction().format().type());
  }

  private InstructionSchema schema(String instructionName) {
    var schema = schemasByInstructionName.get(instructionName);
    if (schema == null) {
      throw new IllegalArgumentException(
          "Unknown instruction `%s` in ISA `%s`."
              .formatted(instructionName, isa.simpleName()));
    }
    return schema;
  }

  private Instruction instruction(String instructionName) {
    return schema(instructionName).instruction();
  }

  private static void seedConstantFields(Encoding encoding,
                                         Map<Format.Field, BigInteger> assignment) {
    for (var field : encoding.fieldEncodings()) {
      var value = field.constant().asVal().castTo(field.formatField().type()).toBits()
          .unsignedInteger();
      assignment.put(field.formatField(), value);
    }
  }

  private static void putAssignment(Map<Format.Field, BigInteger> assignment, Format.Field field,
                                    BigInteger value, String operandName) {
    var existing = assignment.putIfAbsent(field, value);
    if (existing != null && !existing.equals(value)) {
      throw new IllegalArgumentException(
          "Operand `%s` conflicts with an existing assignment for field `%s`: %s vs %s"
              .formatted(operandName, field.simpleName(), existing, value));
    }
  }

  private interface OperandBinder {

    void bind(long operandValue, Map<Format.Field, BigInteger> assignment);
  }

  private record FieldBinder(Format.Field field) implements OperandBinder {

    @Override
    public void bind(long operandValue, Map<Format.Field, BigInteger> assignment) {
      var normalized = Constant.Value.of(operandValue, field.type()).toBits().unsignedInteger();
      putAssignment(assignment, field, normalized, field.simpleName());
    }
  }

  private record FieldAccessBinder(Format.FieldAccess access,
                                   java.util.List<Format.FieldEncoding> encodings)
      implements OperandBinder {

    @Override
    public void bind(long operandValue, Map<Format.Field, BigInteger> assignment) {
      var accessType = access.type();
      if (!(accessType instanceof DataType dataType)) {
        throw new IllegalArgumentException(
            "Field access `%s` does not have a data type and cannot be encoded."
                .formatted(access.simpleName()));
      }
      var accessValue = Constant.Value.of(operandValue, dataType);
      for (var encoding : encodings) {
        var graph = encoding.behavior().copy();
        graph.getNodes(FieldAccessRefNode.class).forEach(node -> {
          if (node.fieldAccess().equals(access)) {
            node.replaceAndDelete(accessValue.toNode());
          }
        });

        var expr = getSingleNode(graph, ReturnNode.class).value();
        var result = Canonicalizer.canonicalizeSubGraph(expr);
        if (!(result instanceof ConstantNode constantNode)) {
          throw new IllegalStateException(
              "Could not canonicalize encoding for field access `%s`."
                  .formatted(access.simpleName()));
        }

        var rawValue = constantNode.constant().asVal().castTo(encoding.targetField().type())
            .toBits().unsignedInteger();
        putAssignment(assignment, encoding.targetField(), rawValue, access.simpleName());
      }
    }
  }

  private record DerivedFieldAccessBinder(Format.FieldAccess access,
                                          Format.Field targetField,
                                          ValueProjection projection)
      implements OperandBinder {

    @Override
    public void bind(long operandValue, Map<Format.Field, BigInteger> assignment) {
      var accessType = access.type();
      if (!(accessType instanceof DataType dataType)) {
        throw new IllegalArgumentException(
            "Field access `%s` does not have a data type and cannot be encoded."
                .formatted(access.simpleName()));
      }

      var accessBits = Constant.Value.of(operandValue, dataType).toBits().unsignedInteger();
      var rawValue = projection.project(accessBits);
      putAssignment(assignment, targetField, normalizeBits(rawValue, targetField.type().bitWidth()),
          access.simpleName());
    }
  }

  @FunctionalInterface
  private interface ValueProjection {

    BigInteger project(BigInteger valueBits);
  }

  private record InstructionSchema(
      Instruction instruction,
      Map<String, OperandBinder> bindersByName,
      Set<String> operandNames
  ) {

    static InstructionSchema of(Instruction instruction) {
      var binders = new LinkedHashMap<String, OperandBinder>();
      for (var field : instruction.encoding().nonEncodedFormatFields()) {
        binders.put(field.simpleName(), new FieldBinder(field));
      }

      var fieldAccesses = Stream.of(
              instruction.format().fieldAccesses().stream(),
              instruction.behavior().getNodes(FieldAccessRefNode.class)
                  .map(FieldAccessRefNode::fieldAccess),
              instruction.assembly().fieldAccesses().stream())
          .flatMap(stream -> stream)
          .distinct()
          .toList();

      for (var access : fieldAccesses) {
        var encodings = instruction.format().fieldEncodingsOf(Set.of(access));
        if (encodings.isEmpty()) {
          var inferred = inferDerivedBinder(access);
          if (inferred != null) {
            binders.putIfAbsent(access.simpleName(), inferred);
          }
          continue;
        }
        binders.putIfAbsent(access.simpleName(), new FieldAccessBinder(access, encodings));
      }

      return new InstructionSchema(instruction, Map.copyOf(binders), Set.copyOf(binders.keySet()));
    }

    private static OperandBinder inferDerivedBinder(Format.FieldAccess access) {
      if (access.fieldRefs().size() != 1) {
        return null;
      }

      var expr = getSingleNode(access.accessFunction().behavior(), ReturnNode.class).value();
      var inverted = invertToField(expr);
      if (inverted == null) {
        return null;
      }
      return new DerivedFieldAccessBinder(access, inverted.field(), inverted.projection());
    }
  }

  private record InvertedFieldProjection(Format.Field field, ValueProjection projection) {
  }

  private static InvertedFieldProjection invertToField(ExpressionNode expr) {
    if (expr instanceof FieldRefNode fieldRefNode) {
      return new InvertedFieldProjection(fieldRefNode.formatField(),
          bits -> normalizeBits(bits, fieldRefNode.formatField().type().bitWidth()));
    }

    if (expr instanceof ZeroExtendNode zeroExtendNode) {
      return invertToField(zeroExtendNode.value());
    }

    if (expr instanceof SignExtendNode signExtendNode) {
      return invertToField(signExtendNode.value());
    }

    if (expr instanceof TruncateNode truncateNode) {
      return invertToField(truncateNode.value());
    }

    if (expr instanceof SliceNode sliceNode) {
      var parts = sliceNode.bitSlice().parts().toList();
      if (parts.size() != 1) {
        return null;
      }
      var part = parts.getFirst();
      var inner = invertToField(sliceNode.value());
      if (inner == null) {
        return null;
      }
      var sliceWidth = sliceNode.bitSlice().bitSize();
      return new InvertedFieldProjection(inner.field(),
          bits -> inner.projection().project(
              normalizeBits(bits, sliceWidth).shiftLeft(part.lsb())));
    }

    if (expr instanceof BuiltInCall builtInCall) {
      if (builtInCall.builtIn() == BuiltInTable.LSL
          && builtInCall.arguments().size() == 2
          && builtInCall.arg(1) instanceof ConstantNode constantNode) {
        var shift = constantNode.constant().asVal().intValue();
        var inner = invertToField(builtInCall.arg(0));
        if (inner == null) {
          return null;
        }
        return new InvertedFieldProjection(inner.field(),
            bits -> inner.projection().project(bits.shiftRight(shift)));
      }

      if (builtInCall.builtIn() == BuiltInTable.LSR
          && builtInCall.arguments().size() == 2
          && builtInCall.arg(1) instanceof ConstantNode constantNode) {
        var shift = constantNode.constant().asVal().intValue();
        var inner = invertToField(builtInCall.arg(0));
        if (inner == null) {
          return null;
        }
        return new InvertedFieldProjection(inner.field(),
            bits -> inner.projection().project(bits.shiftLeft(shift)));
      }
    }

    return null;
  }

  private static BigInteger normalizeBits(BigInteger value, int bitWidth) {
    if (bitWidth <= 0) {
      return BigInteger.ZERO;
    }
    var mask = BigInteger.ONE.shiftLeft(bitWidth).subtract(BigInteger.ONE);
    return value.and(mask);
  }
}
