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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static vadl.utils.GraphUtils.getSingleNode;

import java.io.IOException;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.testcontainers.shaded.com.google.errorprone.annotations.concurrent.LazyInit;
import vadl.AbstractTest;
import vadl.TestUtils;
import vadl.types.BuiltInTable;
import vadl.utils.ViamUtils;
import vadl.viam.Definition;
import vadl.viam.Function;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.SignExtendNode;
import vadl.viam.graph.dependency.TruncateNode;
import vadl.viam.graph.dependency.ZeroExtendNode;
import vadl.viam.passes.verification.ViamVerifier;

public class TypeCastEliminationTest extends AbstractTest {

  @LazyInit
  private static Specification spec;

  @BeforeEach
  void getSpec() throws IOException {
    spec =
        runAndGetViamSpecification("passes/typeCastElimination/valid_type_cast_elimination.vadl");
    ViamVerifier.verifyAllIn(spec);
  }


  @TestFactory
  Stream<DynamicTest> testTrivial_shouldDeleteTypeCastAndMakeArgumentTypeSameAsResultType() {
    return findFuncNameArgumentsByPrefix("Trivial_", spec).map(func ->
        DynamicTest.dynamicTest(func, () -> {
          var behavior = getTestBehavior(func);
          assertEquals(3, behavior.getNodes().count());
          var returnNode = getSingleNode(behavior, ReturnNode.class);
          var paramNode = getSingleNode(behavior, FuncParamNode.class);
          assertEquals(returnNode.returnType(), paramNode.type());
        }));
  }

  @TestFactory
  Stream<DynamicTest> testTruncate_shouldUseTruncateNodeInsteadOfTypeCast() {
    return findFuncNameArgumentsByPrefix("Truncate_", spec).map(func ->
        DynamicTest.dynamicTest(func, () -> {
          var behavior = getTestBehavior(func);
          assertEquals(4, behavior.getNodes().count());
          assertEquals(1, behavior.getNodes(TruncateNode.class).count());
        }));
  }

  @TestFactory
  Stream<DynamicTest> testZeroExtend_shouldUseZeroExtendNodeInsteadOfTypeCast() {
    return findFuncNameArgumentsByPrefix("ZeroExtend_", spec).map(func ->
        DynamicTest.dynamicTest(func, () -> {
          var behavior = getTestBehavior(func);
          assertEquals(4, behavior.getNodes().count());
          var paramNode = getSingleNode(behavior, FuncParamNode.class);
          var zeroExtendNode = getSingleNode(behavior, ZeroExtendNode.class);
          assertEquals(paramNode, zeroExtendNode.value());
        }));
  }


  @TestFactory
  Stream<DynamicTest> testSignExtend_shouldUseSignExtendNodeInsteadOfTypeCast() {
    return findFuncNameArgumentsByPrefix("SignExtend_", spec).map(func ->
        DynamicTest.dynamicTest(func, () -> {
          var behavior = getTestBehavior(func);
          assertEquals(4, behavior.getNodes().count());
          var paramNode = getSingleNode(behavior, FuncParamNode.class);
          var signExtendNode = getSingleNode(behavior, SignExtendNode.class);
          assertEquals(paramNode, signExtendNode.value());
        }));
  }


  @TestFactory
  Stream<DynamicTest> testBoolCast_shouldReplaceByNegCallWithSecondArg0Const() {
    return findFuncNameArgumentsByPrefix("BoolCast_", spec).map(func ->
        DynamicTest.dynamicTest(func, () -> {
          var behavior = getTestBehavior(func);
          assertEquals(5, behavior.getNodes().count());
          var paramNode = getSingleNode(behavior, FuncParamNode.class);
          var compareNode = getSingleNode(behavior, BuiltInCall.class);
          var zeroConstant = getSingleNode(behavior, ConstantNode.class);
          assertEquals(BuiltInTable.NEQ, compareNode.builtIn());
          assertEquals(0, zeroConstant.constant().asVal().intValue());
          assertEquals(paramNode, compareNode.arguments().get(0));
          assertEquals(zeroConstant, compareNode.arguments().get(1));
          var returnNode = getSingleNode(behavior, ReturnNode.class);
          assertEquals(compareNode, returnNode.value());
        }));
  }

  @Test
  void testNonOptimalExample() {
    var behavior = getTestBehavior("NonOptimalExample");
    assertEquals(5, behavior.getNodes().count());
    var truncateNode = getSingleNode(behavior, TruncateNode.class);
    var signExtendNode = getSingleNode(behavior, SignExtendNode.class);
    var paramNode = getSingleNode(behavior, FuncParamNode.class);
    assertEquals(paramNode, signExtendNode.value());
    assertEquals(signExtendNode, truncateNode.value());
  }

  private Graph getTestBehavior(String testFunction) {
    var simpleTypCast = TestUtils.findDefinitionByNameIn(testFunction, spec, Function.class);
    ViamVerifier.verifyAllIn(simpleTypCast);
    var behavior = simpleTypCast.behavior();
    Assertions.assertEquals(1, behavior.getNodes(StartNode.class).count());
    Assertions.assertEquals(1, behavior.getNodes(ReturnNode.class).count());
    return behavior;
  }

  private static Stream<String> findFuncNameArgumentsByPrefix(String prefix,
                                                              Specification spec) {
    return
        ViamUtils.findDefinitionsByFilter(spec,
                (def) -> def.identifier.name().startsWith(prefix)
                    && def instanceof Function)
            .stream()
            .map(Definition::simpleName);
  }

}
