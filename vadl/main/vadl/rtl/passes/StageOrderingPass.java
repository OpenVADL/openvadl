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

package vadl.rtl.passes;

import static vadl.error.Diagnostic.error;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.utils.Pair;
import vadl.viam.MiaInstructionFlow;
import vadl.viam.MicroArchitecture;
import vadl.viam.Specification;
import vadl.viam.Stage;
import vadl.viam.ViamError;

/**
 * Sets the next and prev pointers of all stages. Examines stage input and output relations.
 *
 * <p>Currently only supports linear pipelines.
 */
public class StageOrderingPass extends Pass {

  public StageOrderingPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("Stage Ordering");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    var mia = viam.mia().orElseThrow(() -> new ViamError("Missing micro architecture"));
    var order = order(mia);

    mia.setStageOrder(order);

    return order;
  }

  private static List<Stage> order(MicroArchitecture mia) {

    // trivially ordered
    if (mia.stages().size() <= 1) {
      return new ArrayList<>(mia.stages());
    }

    // input/output dependencies
    var dep = new HashSet<Pair<Stage, Stage>>(); // stage read from -> stage reading
    for (var instructionFlow : mia.allInstructionFlows()) {
      if (instructionFlow instanceof MiaInstructionFlow.StageOutputToStage(var src, var dst)) {
        dep.add(new Pair<>(src.stage(), dst));
      } else {
        throw error("Unexpected Dependency", instructionFlow.destination())
            .description("Stage ordering currently only handles reads from stage outputs.")
            .build();
      }
    }
    var readFrom = dep.stream().map(Pair::left).collect(Collectors.toSet());
    var reading = dep.stream().map(Pair::right).collect(Collectors.toSet());

    // check we can order every stage
    var unordered = new HashSet<>(mia.stages());
    unordered.removeAll(reading);
    unordered.removeAll(readFrom);
    var anyUnordered = unordered.stream().findAny();
    ViamError.ensure(anyUnordered.isEmpty(), () -> error(
        "All stages need to be ordered", anyUnordered.get().location()));

    var start = mia.rootStage();

    var order = new ArrayList<Stage>();
    follow(dep, start, order);
    ViamError.ensure(order.size() == mia.stages().size(), () -> error(
        "All stages need to be ordered", mia.location()));

    return order;
  }

  private static void follow(Set<Pair<Stage, Stage>> dep, Stage cur, List<Stage> order) {
    order.add(cur);

    // find successor
    var succ = dep.stream().filter(p -> p.left() == cur).toList();
    ViamError.ensure(succ.size() <= 1, () -> error(
        "Can not order stage, more than one successor", cur.location()));

    // recurse
    if (!succ.isEmpty()) {
      follow(dep, succ.get(0).right(), order);
    }
  }
}
