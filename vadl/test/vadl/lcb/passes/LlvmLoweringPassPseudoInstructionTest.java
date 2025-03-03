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

package vadl.lcb.passes;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import vadl.lcb.AbstractLcbTest;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.passes.llvmLowering.tablegen.lowering.TableGenMachineInstructionPrinterVisitor;
import vadl.lcb.passes.llvmLowering.tablegen.lowering.TableGenPatternPrinterVisitor;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenSelectionWithOutputPattern;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionOperand;
import vadl.pass.PassKey;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.viam.Constant;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;

public class LlvmLoweringPassPseudoInstructionTest extends AbstractLcbTest {

  record TestOutput(List<TableGenInstructionOperand> inputs,
                    List<TableGenInstructionOperand> outputs, List<String> selectorPatterns,
                    List<String> machinePatterns, LlvmLoweringPass.Flags flags) {
  }

  private static final HashMap<String, TestOutput> expectedResults = new HashMap<>();
  private static final Node DUMMY_NODE = new ConstantNode(new Constant.Str(""));

  static {
    expectedResults.put("BEQZ", new TestOutput(Collections.emptyList(), Collections.emptyList(),
        Collections.emptyList(), Collections.emptyList(), getPseudoFlags()));
    expectedResults.put("BGEZ", new TestOutput(Collections.emptyList(), Collections.emptyList(),
        Collections.emptyList(), Collections.emptyList(), getPseudoFlags()));
    expectedResults.put("BGTZ", new TestOutput(Collections.emptyList(), Collections.emptyList(),
        Collections.emptyList(), Collections.emptyList(), getPseudoFlags()));
    expectedResults.put("BLEZ", new TestOutput(Collections.emptyList(), Collections.emptyList(),
        Collections.emptyList(), Collections.emptyList(), getPseudoFlags()));
    expectedResults.put("BLTZ", new TestOutput(Collections.emptyList(), Collections.emptyList(),
        Collections.emptyList(), Collections.emptyList(), getPseudoFlags()));
    expectedResults.put("BNEZ", new TestOutput(Collections.emptyList(), Collections.emptyList(),
        Collections.emptyList(), Collections.emptyList(), getPseudoFlags()));
    expectedResults.put("CALL", new TestOutput(Collections.emptyList(), Collections.emptyList(),
        Collections.emptyList(), Collections.emptyList(), getPseudoFlagsCall()));
    expectedResults.put("J", new TestOutput(Collections.emptyList(), Collections.emptyList(),
        List.of("(br bb:$imm)"), List.of("(J RV3264I_Jtype_immAsLabel:$imm)"),
        new LlvmLoweringPass.Flags(
            true, true, false, false, true, false, false, false, true, false, false)));
    expectedResults.put("MOV",
        new TestOutput(List.of(createOperand("X", "rd")), List.of(createOperand("X", "rs1")),
            List.of("(add X:$rs1, (i64 0))", "(add AddrFI:$rs1, (i64 0))"),
            List.of("(MOV X:$rs1)", "(MOV AddrFI:$rs1)"), getPseudoFlags()));
    expectedResults.put("NEG",
        new TestOutput(List.of(createOperand("X", "rd")), List.of(createOperand("X", "rs1")),
            List.of("(sub (i64 0), X:$rs1)"), List.of("(NEG X:$rs1)"), getPseudoFlags()));
    expectedResults.put("NOP", new TestOutput(Collections.emptyList(), Collections.emptyList(),
        List.of("(add (i64 0), (i64 0))"), List.of("(NOP )"), getPseudoFlags()));
    expectedResults.put("NOT", new TestOutput(Collections.emptyList(), Collections.emptyList(),
        List.of("(xor X:$rs1, (i64 4095))"), List.of("(NOT X:$rs1)"), getPseudoFlags()));
    expectedResults.put("RETURN", new TestOutput(Collections.emptyList(), Collections.emptyList(),
        Collections.emptyList(), Collections.emptyList(), getPseudoFlags()));
    expectedResults.put("SGTZ",
        new TestOutput(List.of(createOperand("X", "rd")), List.of(createOperand("X", "rs1")),
            List.of("(setcc (i64 0), X:$rs1, SETLT)"), List.of("(SGTZ X:$rs1)"), getPseudoFlags()));
    expectedResults.put("SLTZ",
        new TestOutput(List.of(createOperand("X", "rd")), List.of(createOperand("X", "rs1")),
            List.of("(setcc X:$rs1, (i64 0), SETLT)"), List.of("(SLTZ X:$rs1)"), getPseudoFlags()));
    expectedResults.put("SNEZ", new TestOutput(List.of(createOperand("X", "rd")),
        List.of(createOperand("X", "rs1")),
        List.of("(setcc (i64 0), X:$rs1, SETULT)"), List.of("(SNEZ X:$rs1)"), getPseudoFlags()));
    expectedResults.put("TAIL", new TestOutput(Collections.emptyList(), Collections.emptyList(),
        Collections.emptyList(), Collections.emptyList(), getPseudoFlagsCall()));
  }

  private static TableGenInstructionOperand createOperand(String type, String name) {
    return new TableGenInstructionOperand(DUMMY_NODE, type, name);
  }

  private static LlvmLoweringPass.Flags getPseudoFlags() {
    return new LlvmLoweringPass.Flags(false,
        false,
        false,
        false,
        true,
        false,
        false,
        false,
        false,
        false,
        false);
  }


  private static LlvmLoweringPass.Flags getPseudoFlagsCall() {
    return new LlvmLoweringPass.Flags(false,
        false,
        false,
        false,
        true,
        false,
        false,
        false,
        false,
        false,
        false);
  }

  @TestFactory
  Stream<DynamicTest> testLowering() throws IOException, DuplicatedPassKeyException {
    // Given
    var setup = runLcb(getConfiguration(false), "sys/risc-v/rv64im.vadl",
        new PassKey(LlvmLoweringPass.class.getName() + "-1"));
    var passManager = setup.passManager();
    var spec = setup.specification();

    // When
    var llvmResults = (LlvmLoweringPass.LlvmLoweringPassResult) passManager.getPassResults()
        .lastResultOf(LlvmLoweringPass.class);

    return spec.isa().map(x -> x.ownPseudoInstructions().stream()).orElse(Stream.empty())
        .filter(x -> expectedResults.containsKey(x.identifier.simpleName()))
        .map(t -> DynamicTest.dynamicTest(t.identifier.simpleName(), () -> {
          var expectedTestOutput = expectedResults.get(t.identifier.simpleName());
          var res = llvmResults.pseudoInstructionRecords().get(t);
          Assertions.assertNotNull(res);

          // Selector Patterns
          var selectorPatterns = res.patterns().stream().map(TableGenPattern::selector)
              .flatMap(x -> x.getDataflowRoots().stream()).map(rootNode -> {
                var visitor = new TableGenPatternPrinterVisitor();
                visitor.visit(rootNode);
                return visitor.getResult();
              }).toList();
          Assertions.assertEquals(expectedTestOutput.selectorPatterns, selectorPatterns);

          // Machine Patterns
          var machinePatterns =
              res.patterns().stream().filter(x -> x instanceof TableGenSelectionWithOutputPattern)
                  .map(x -> (TableGenSelectionWithOutputPattern) x)
                  .map(TableGenSelectionWithOutputPattern::machine)
                  .flatMap(x -> x.getDataflowRoots().stream()).map(rootNode -> {
                    var visitor = new TableGenMachineInstructionPrinterVisitor();
                    visitor.visit((ExpressionNode) rootNode);
                    return visitor.getResult();
                  }).toList();
          Assertions.assertEquals(expectedTestOutput.machinePatterns, machinePatterns);

          // Flags
          Assertions.assertEquals(expectedTestOutput.flags(), res.flags());
        }));
  }
}
