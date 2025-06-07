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

package vadl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static vadl.utils.ViamUtils.findDefinitionsByFilter;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.arbitraries.BigIntegerArbitrary;
import org.junit.jupiter.api.Assertions;
import vadl.ast.TypeChecker;
import vadl.ast.VadlParser;
import vadl.ast.ViamLowering;
import vadl.error.Diagnostic;
import vadl.error.DiagnosticList;
import vadl.types.BitsType;
import vadl.types.DataType;
import vadl.utils.SourceLocation;
import vadl.viam.Assembly;
import vadl.viam.Constant;
import vadl.viam.Definition;
import vadl.viam.Encoding;
import vadl.viam.Format;
import vadl.viam.Function;
import vadl.viam.Identifier;
import vadl.viam.Instruction;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.Parameter;
import vadl.viam.Processor;
import vadl.viam.Resource;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.passes.verification.ViamVerifier;

public class TestUtils {

  /**
   * Asserts that each expected error message is a substring of exactly
   * one unique actual error message in the provided {@code DiagnosticList},
   * and that each actual error message corresponds to exactly one expected error message.
   *
   * @param diagnosticList        the {@code DiagnosticList} containing the actual error messages;
   * @param expectedErrorMessages the expected error messages to verify as substrings
   * @throws AssertionError if the number of actual and expected messages differ, or if any expected
   *                        error message does not have a unique corresponding actual error message,
   *                        or if any actual error message is matched more than once
   */
  public static void assertErrors(DiagnosticList diagnosticList, String... expectedErrorMessages) {
    List<String> actualMessages = diagnosticList.items.stream()
        .map(Diagnostic::getMessage)
        .collect(Collectors.toList());

    // Check if the sizes match
    assertThat(actualMessages)
        .as("Mismatch in number of actual and expected error messages")
        .hasSameSizeAs(expectedErrorMessages);

    List<String> unmatchedActualMessages = new ArrayList<>(actualMessages);

    for (String expectedMessage : expectedErrorMessages) {
      Iterator<String> iterator = unmatchedActualMessages.iterator();
      boolean matchFound = false;

      while (iterator.hasNext()) {
        String actualMessage = iterator.next();
        if (actualMessage.contains(expectedMessage)) {
          matchFound = true;
          iterator.remove(); // Remove to prevent duplicate matching
          break;
        }
      }

      assertThat(matchFound)
          .as("Expected error message not found: %s", expectedMessage)
          .isTrue();
    }
  }

  /**
   * Creates the VIAM for the given source code string.
   */
  public static Specification compileToViam(String sourceCode) {
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(sourceCode), "Cannot parse input");
    var typechecker = new TypeChecker();
    typechecker.verify(ast);
    var lowering = new ViamLowering();
    var spec = Assertions.assertDoesNotThrow(() -> lowering.generate(ast), "Cannot generate VIAM");
    ViamVerifier.verifyAllIn(spec);
    return spec;
  }

  /**
   * Finds a {@link Resource} with the given name in the specified {@link Specification}.
   *
   * @param name the name of the resource to find
   * @param spec the specification containing the resource
   * @return the resource with the given name
   * @throws AssertionError if the number of resources found with the given name is not equal to 1
   */
  public static Resource findResourceByName(String name, Specification spec) {
    var definitions = new ArrayList<>(spec.definitions().toList());

    // The ISA is no longer in the spec definitions but the tests expect it so let's add it here
    spec.definitions().filter(Processor.class::isInstance)
        .findFirst()
        .ifPresent(mp -> definitions.add(((Processor) mp).isa()));

    var r = definitions.stream()
        .filter(InstructionSetArchitecture.class::isInstance)
        .map(InstructionSetArchitecture.class::cast)
        .flatMap(i -> i.registerTensors().stream())
        .filter(i -> i.identifier.name().equals(name))
        .toList();

    assertEquals(1, r.size(), "Wrong number of resources with name " + name + " found");
    return r.get(0);
  }


  /**
   * Finds a {@link Format} with the given name in the specified {@link Specification}.
   *
   * @param name the name of the format to find
   * @param spec the specification containing the formats
   * @return the format with the given name
   * @throws AssertionError if the number of formats found with the given name is not equal to 1
   */
  public static Format findFormatByName(String name, Specification spec) {
    var formats = spec.findAllFormats()
        .filter(f -> f.identifier.name().equals(name))
        .toList();
    assertEquals(1, formats.size(), "Wrong number of formats with name " + name + " found");
    return formats.get(0);
  }

  /**
   * Finds a Definition with the given name in the specified Specification.
   *
   * @param name the name of the Definition to find
   * @param spec the Specification containing the Definitions
   * @return the Definition with the given name
   * @throws AssertionError if the number of Definitions found with the given name is not equal to 1
   */
  public static <T extends Definition> T findDefinitionByNameIn(String name, Definition spec,
                                                                Class<T> definitionClass) {
    var result = findDefinitionsByFilter(spec,
        (def) -> definitionClass.isInstance(def) && def.identifier.name().equals(name));

    assertEquals(1, result.size(), "Wrong number of definitions with name " + name + " found");
    //noinspection unchecked
    return (T) result.toArray()[0];
  }


  /**
   * Results a (positive) big integer that represents a random sequence of bits with the
   * given bit-width.
   * The caller must ensure that only the bits within the specified bit-width are used.
   * So it is not ensured that the msb is 1.
   *
   * @param bitWidth the number of bits to generate.
   * @return positive number with potentially zero bits within the bit-width
   */
  public static BigIntegerArbitrary arbitraryBits(int bitWidth) {
    return Arbitraries.bigIntegers()
        .greaterOrEqual(BigInteger.ZERO)
        .lessOrEqual(BigInteger.valueOf(2)
            .pow(bitWidth)
            .subtract(BigInteger.ONE)
        );
  }

  /**
   * Results a (positive) big integer that represents a random sequence of bits with the
   * given bit-width.
   * The caller must ensure that only the bits within the specified bit-width are used.
   * So it is not ensured that the msb is 1.
   *
   * @param bitWidth the number of bits to generate.
   * @return positive number with potentially zero bits within the bit-width
   */
  public static BigIntegerArbitrary arbitraryUnsignedInt(int bitWidth) {
    return arbitraryBits(bitWidth);
  }


  /**
   * Results a big integer that represents a random sequence of bits with the
   * given bit-width.
   *
   * @param bitWidth the number of bits to generate.
   * @return number with potentially zero bits within the bit-width
   */
  public static BigIntegerArbitrary arbitrarySignedInt(int bitWidth) {
    return Arbitraries.bigIntegers()
        .greaterOrEqual(BigInteger.valueOf(-2)
            .pow(bitWidth - 1))
        .lessOrEqual(BigInteger.valueOf(2)
            .pow(bitWidth - 1)
            .subtract(BigInteger.ONE)
        );
  }

  /**
   * Results a big integer that represents a random sequence of bits between the given
   * range.
   * Both boundaries are inclusive.
   *
   * @return number with potentially zero bits within the bit-width
   */
  public static Arbitrary<BigInteger> arbitraryBetween(BigInteger min, BigInteger max) {
    return Arbitraries.bigIntegers()
        .greaterOrEqual(min)
        .lessOrEqual(max);
  }

  public static Identifier createIdentifier(String name) {
    return new Identifier(name, SourceLocation.INVALID_SOURCE_LOCATION);
  }

  public static Specification createSpecification(String name) {
    return new Specification(createIdentifier(name));
  }

  public static Format createFormat(String name, BitsType ty) {
    return new Format(createIdentifier(name), ty);
  }

  public static Format.FieldAccess createFieldAccess(String name, Function accessFunction) {
    return new Format.FieldAccess(createIdentifier(name), accessFunction, null, null);
  }

  public static Function createFunction(String name, DataType retTy) {
    return new Function(createIdentifier(name), new Parameter[] {}, retTy);
  }

  public static Function createFunction(String name, Parameter param, DataType retTy) {
    return new Function(createIdentifier(name), new Parameter[] {param}, retTy);
  }

  public static Function createFunctionWithoutParam(String name, DataType retTy) {
    return new Function(createIdentifier(name), new Parameter[] {}, retTy);
  }

  public static Parameter createParameter(String name, DataType ty) {
    return new Parameter(createIdentifier(name), ty);
  }

  public static Format.Field createFieldWithParent(String name, DataType ty,
                                                   Constant.BitSlice slice, int bitWidthFormat) {
    var parent = new Format(createIdentifier(name + ".format"), BitsType.bits(bitWidthFormat));
    return createField(name, ty, slice, parent);
  }

  public static Format.Field createField(String name, DataType ty, Constant.BitSlice slice,
                                         Format parent) {
    return new Format.Field(createIdentifier(name), ty, slice, parent);
  }

  public static Assembly createAssembly(String name) {
    return new Assembly(createIdentifier(name), new Function(createIdentifier(name + ".assembly"),
        new Parameter[] {}, DataType.string()));
  }

  public static Instruction createInstruction(String name, BitsType ty) {
    var format = createFormat(name + ".format", ty);
    return createInstruction(name, format);
  }

  public static Instruction createInstruction(String name, Format format) {
    return new Instruction(createIdentifier(name), new Graph(name + ".graph"),
        createAssembly(name + ".assembly"),
        new Encoding(createIdentifier(name + ".encoding"),
            format,
            new Encoding.Field[] {}));
  }

}
