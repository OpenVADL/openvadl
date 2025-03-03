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

package vadl.viam.passes;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static vadl.utils.GraphUtils.getSingleNode;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import vadl.AbstractTest;
import vadl.TestUtils;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.PassOrders;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.types.BuiltInTable;
import vadl.viam.Constant;
import vadl.viam.Instruction;
import vadl.viam.graph.control.BranchEndNode;
import vadl.viam.graph.control.IfNode;
import vadl.viam.graph.control.InstrEndNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.SideEffectNode;
import vadl.viam.graph.dependency.WriteRegNode;
import vadl.viam.matching.Matcher;
import vadl.viam.matching.impl.BuiltInMatcher;
import vadl.viam.matching.impl.ConstantValueMatcher;
import vadl.viam.passes.sideeffect_condition.SideEffectConditionResolvingPass;
import vadl.viam.passes.verification.ViamVerificationPass;

public class SideEffectConditionResolvingPassTest extends AbstractTest {

  private void testTrivial(Instruction instr) {
    var behavior = instr.behavior();
    var write = getSingleNode(behavior, WriteRegNode.class);
    assertTrue(new ConstantValueMatcher(Constant.Value.of(true))
        .matches(write.condition()));
  }

  private void testSingle(Instruction instr) {
    var behavior = instr.behavior();
    var ifNode = getSingleNode(behavior, IfNode.class);
    var instrEnd = getSingleNode(behavior, InstrEndNode.class);
    var branchEnd = behavior.getNodes(BranchEndNode.class)
        .filter(e -> !e.sideEffects().isEmpty()).findFirst().get();

    var trivialMatcher = new ConstantValueMatcher(Constant.Value.of(true));
    var ifCaseMatcher = new BuiltInMatcher(
        BuiltInTable.AND,
        List.of(
            trivialMatcher,
            (node) -> node == ifNode.condition()
        )
    );

    assertTrue(trivialMatcher.matches(instrEnd.sideEffects().get(0).condition()));
    assertTrue(ifCaseMatcher.matches(branchEnd.sideEffects().get(0).condition()));
  }


  private void testSingleUnique(Instruction instr) {
    var behavior = instr.behavior();
    var ifNode = getSingleNode(behavior, IfNode.class);
    var instrEnd = getSingleNode(behavior, InstrEndNode.class);
    var branchEnd = behavior.getNodes(BranchEndNode.class)
        .filter(e -> !e.sideEffects().isEmpty()).findFirst().get();

    var trivialMatcher = new ConstantValueMatcher(Constant.Value.of(true));
    var nestedCaseMatcher = new BuiltInMatcher(
        BuiltInTable.OR,
        List.of(
            new BuiltInMatcher(
                BuiltInTable.AND,
                List.of(
                    trivialMatcher,
                    (node) -> node == ifNode.condition()
                )
            ),
            trivialMatcher
        )
    );

    assertSame(instrEnd.sideEffects().get(0), branchEnd.sideEffects().get(0));
    assertTrue(nestedCaseMatcher.matches(instrEnd.sideEffects().get(0).condition()));
  }

  private void testSingleIfElse(Instruction instr) {
    var behavior = instr.behavior();
    var ifNode = getSingleNode(behavior, IfNode.class);
    var branchEnd = behavior.getNodes(BranchEndNode.class)
        .filter(e -> !e.sideEffects().isEmpty()).findFirst().get();

    var trivialMatcher = new ConstantValueMatcher(Constant.Value.of(true));
    var nestedCaseMatcher = new BuiltInMatcher(
        BuiltInTable.OR,
        List.of(
            new BuiltInMatcher(
                BuiltInTable.AND,
                List.of(
                    trivialMatcher,
                    (node) -> node == ifNode.condition()
                )
            ),
            new BuiltInMatcher(
                BuiltInTable.AND,
                List.of(
                    trivialMatcher,
                    new BuiltInMatcher(
                        BuiltInTable.NOT,
                        (node) -> node == ifNode.condition()
                    )
                )
            )
        )
    );

    assertTrue(nestedCaseMatcher.matches(branchEnd.sideEffects().get(0).condition()));
  }

  private void testDualIfElse(Instruction instr) {
    var behavior = instr.behavior();
    var ifNodeOne = (IfNode) getSingleNode(behavior, StartNode.class)
        .next();
    var ifNodeTwo = behavior.getNodes(IfNode.class).filter(e -> ifNodeOne != e)
        .findFirst().get();


    var trivialMatcher = new ConstantValueMatcher(Constant.Value.of(true));
    Matcher ifOneMatcher = (node) -> node == ifNodeOne.condition();
    Matcher ifTwoMatcher = (node) -> node == ifNodeTwo.condition();

    var notFirstIf = new BuiltInMatcher(BuiltInTable.NOT, ifOneMatcher);
    var notSecondIf = new BuiltInMatcher(BuiltInTable.NOT, ifTwoMatcher);

    var ifMatcher = new BuiltInMatcher(BuiltInTable.AND, List.of(trivialMatcher, ifOneMatcher));
    var elseMatcher = new BuiltInMatcher(BuiltInTable.AND, List.of(trivialMatcher, notFirstIf));
    var ififMatcher = new BuiltInMatcher(BuiltInTable.AND, List.of(ifMatcher, ifTwoMatcher));
    var ifelseMatcher = new BuiltInMatcher(BuiltInTable.AND, List.of(ifMatcher, notSecondIf));

    var ifEffect = (SideEffectNode) behavior.getNodes(ConstantNode.class)
        .filter(c -> c.constant().asVal().intValue() == 10)
        .findFirst().get().usages().findFirst().get();

    var elseEffect = (SideEffectNode) behavior.getNodes(ConstantNode.class)
        .filter(c -> c.constant().asVal().intValue() == 20)
        .findFirst().get().usages().findFirst().get();

    var ififEffect = (SideEffectNode) behavior.getNodes(ConstantNode.class)
        .filter(c -> c.constant().asVal().intValue() == 11)
        .findFirst().get().usages().findFirst().get();

    var ifElseEffect = (SideEffectNode) behavior.getNodes(ConstantNode.class)
        .filter(c -> c.constant().asVal().intValue() == 12)
        .findFirst().get().usages().findFirst().get();


    assertTrue(ifMatcher.matches(ifEffect.condition()));
    assertTrue(elseMatcher.matches(elseEffect.condition()));
    assertTrue(ififMatcher.matches(ififEffect.condition()));
    assertTrue(ifelseMatcher.matches(ifElseEffect.condition()));
  }


  @TestFactory
  Stream<DynamicTest> sideEffectConditionResolvingPass()
      throws IOException, DuplicatedPassKeyException {
    var config = new GeneralConfiguration(Path.of("build/test-out"), true);
    var setup = setupPassManagerAndRunSpec(
        "passes/sideEffectConditionResolving/valid_test_cases.vadl",
        PassOrders.viam(config)
            .untilFirst(SideEffectConditionResolvingPass.class)
            .add(new ViamVerificationPass(config))
    );

    var spec = setup.specification();

    return Stream.of(
        dynamicTest("TRIVIAL", () -> testTrivial(
            TestUtils.findDefinitionByNameIn("Tests::TRIVIAL", spec, Instruction.class)
        )),
        dynamicTest("SINGLE", () -> testSingle(
            TestUtils.findDefinitionByNameIn("Tests::SINGLE", spec, Instruction.class)
        )),
        dynamicTest("SINGLE_UNIQUE", () -> testSingleUnique(
            TestUtils.findDefinitionByNameIn("Tests::SINGLE_UNIQUE", spec, Instruction.class)
        )),
        dynamicTest("SINGLE_IF_ELSE", () -> testSingleIfElse(
            TestUtils.findDefinitionByNameIn("Tests::SINGLE_IF_ELSE", spec, Instruction.class)
        )),
        dynamicTest("DUAL_IF_ELSE", () -> testDualIfElse(
            TestUtils.findDefinitionByNameIn("Tests::DUAL_IF_ELSE", spec, Instruction.class)
        ))
    );
  }

}
