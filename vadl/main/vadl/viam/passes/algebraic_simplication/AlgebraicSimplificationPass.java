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

package vadl.viam.passes.algebraic_simplication;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Specification;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.passes.algebraic_simplication.rules.AlgebraicSimplificationRule;
import vadl.viam.passes.algebraic_simplication.rules.impl.AdditionWithZeroSimplificationRule;
import vadl.viam.passes.algebraic_simplication.rules.impl.AndWithFalseSimplificationRule;
import vadl.viam.passes.algebraic_simplication.rules.impl.AndWithTrueSimplificationRule;
import vadl.viam.passes.algebraic_simplication.rules.impl.DivisionWithOneSimplificationRule;
import vadl.viam.passes.algebraic_simplication.rules.impl.MultiplicationWithOneSimplificationRule;
import vadl.viam.passes.algebraic_simplication.rules.impl.MultiplicationWithZeroSimplificationRule;
import vadl.viam.passes.algebraic_simplication.rules.impl.OrWithFalseSimplificationRule;
import vadl.viam.passes.algebraic_simplication.rules.impl.OrWithTrueSimplificationRule;
import vadl.viam.passes.algebraic_simplication.rules.impl.RemainderWithOneSimplificationRule;
import vadl.viam.passes.algebraic_simplication.rules.impl.RemainderWithZeroSimplificationRule;
import vadl.viam.passes.algebraic_simplication.rules.impl.ShiftSimplificationRule;

/**
 * It looks at all the {@link BuiltInCall} nodes and tries to match a static set of rules.
 * If a rule matches then the {@link BuiltInCall} will be simplified.
 * It will repeat the process until nothing changes.
 * It will only consider machine instructions.
 */
public class AlgebraicSimplificationPass extends Pass {
  public static final List<AlgebraicSimplificationRule> rules = new ArrayList<>();

  static {
    rules.add(new AdditionWithZeroSimplificationRule());
    rules.add(new MultiplicationWithZeroSimplificationRule());
    rules.add(new MultiplicationWithOneSimplificationRule());
    rules.add(new DivisionWithOneSimplificationRule());
    rules.add(new RemainderWithZeroSimplificationRule());
    rules.add(new RemainderWithOneSimplificationRule());
    rules.add(new AndWithFalseSimplificationRule());
    rules.add(new AndWithTrueSimplificationRule());
    rules.add(new OrWithTrueSimplificationRule());
    rules.add(new OrWithFalseSimplificationRule());
    rules.add(new ShiftSimplificationRule());
  }

  public AlgebraicSimplificationPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("algebraicSimplificationPass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) {
    viam.isa().map(isa -> isa.ownInstructions().stream())
        .orElse(Stream.empty())
        .forEach(instruction -> new AlgebraicSimplifier(rules).run(instruction.behavior()));

    viam.isa()
        .map(isa -> isa.ownPseudoInstructions().stream())
        .orElse(Stream.empty())
        .forEach(instruction -> new AlgebraicSimplifier(rules).run(instruction.behavior()));

    viam.isa()
        .map(isa -> isa.ownFormats().stream())
        .orElse(Stream.empty())
        .flatMap(x -> x.fieldAccesses().stream())
        .map(x -> x.accessFunction().behavior())
        .forEach(x -> new AlgebraicSimplifier(rules).run(x));

    return null;
  }
}
