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

package vadl.viam.passes.behaviorRewrite;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Specification;
import vadl.viam.passes.algebraic_simplication.AlgebraicSimplificationPass;
import vadl.viam.passes.behaviorRewrite.rules.BehaviorRewriteSimplificationRule;
import vadl.viam.passes.behaviorRewrite.rules.impl.LetNodeSimplificationRule;
import vadl.viam.passes.behaviorRewrite.rules.impl.MergeSMullAndTruncateToMulSimplificationRule;

/**
 * This pass should provide more generic rewrites than {@link AlgebraicSimplificationPass}.
 */
public class BehaviorRewritePass extends Pass {
  public static final List<BehaviorRewriteSimplificationRule> rules = new ArrayList<>();

  static {
    rules.add(new LetNodeSimplificationRule());
    rules.add(new MergeSMullAndTruncateToMulSimplificationRule());
  }

  public BehaviorRewritePass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("behaviorRewritePass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    viam.isa().map(isa -> isa.ownInstructions().stream())
        .orElse(Stream.empty())
        .forEach(instruction -> new BehaviorRewriteSimplifier(rules).run(instruction.behavior()));

    return null;
  }
}
