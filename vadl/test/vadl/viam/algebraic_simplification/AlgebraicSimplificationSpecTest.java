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

package vadl.viam.algebraic_simplification;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import vadl.AbstractTest;
import vadl.pass.PassOrders;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.viam.Instruction;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.passes.algebraic_simplication.AlgebraicSimplificationPass;
import vadl.viam.passes.verification.ViamVerificationPass;

/**
 * This class executes all tests in the `algebraic_optimization.vadl` test source.
 */
public class AlgebraicSimplificationSpecTest extends AbstractTest {


  @TestFactory
  Stream<DynamicTest> checkIfBuiltInOptimized() throws IOException, DuplicatedPassKeyException {
    var config = getConfiguration(false);
    var setup = setupPassManagerAndRunSpec(
        "passes/algebraicSimplification/algebraic_simplification.vadl",
        PassOrders.viam(config)
            .untilFirst(AlgebraicSimplificationPass.class)
            .add(new ViamVerificationPass(config))
    );

    var spec = setup.specification();
    return spec.definitions()
        .filter(Instruction.class::isInstance)
        .map(Instruction.class::cast)
        .map(f -> DynamicTest.dynamicTest("Check " + f.simpleName(), () -> {
          assertTrue(f.behavior().getNodes(BuiltInCall.class).findAny().isEmpty());
        }));
  }
}
