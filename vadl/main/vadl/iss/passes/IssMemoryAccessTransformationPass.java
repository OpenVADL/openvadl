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

package vadl.iss.passes;

import java.io.IOException;
import javax.annotation.Nullable;
import vadl.configuration.IssConfiguration;
import vadl.iss.passes.nodes.IssConstExtractNode;
import vadl.iss.passes.nodes.IssLoadNode;
import vadl.iss.passes.nodes.IssStoreNode;
import vadl.iss.passes.tcgLowering.TcgExtend;
import vadl.iss.passes.tcgLowering.Tcg_8_16_32_64;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.WriteMemNode;

/**
 * TCG loads and stores provide a memory operation that allows to directly extend loaded values.
 * However, in VADL the user cast read values, causing extend nodes in the VIAM.
 * To minimize the number of TCG operations, this pass detects sign extends that can be included
 * in the load/store operation and removes the unnecessary extends.
 *
 * <p>It also lowers all {@link ReadMemNode} and {@link WriteMemNode} to
 * {@link IssLoadNode} and {@link IssStoreNode}s.</p>
 */
public class IssMemoryAccessTransformationPass extends AbstractIssPass {


  public IssMemoryAccessTransformationPass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("ISS Memory Access Optimization");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {

    viam.isa().get()
        .ownInstructions().forEach((instruction) -> {
          new MemoryTransformer(instruction.behavior()).run();
        });

    return null;
  }
}


class MemoryTransformer {

  Graph behavior;

  MemoryTransformer(Graph behavior) {
    this.behavior = behavior;
  }


  void run() {
    behavior.getNodes(ReadMemNode.class).forEach(this::handleRead);
    behavior.getNodes(WriteMemNode.class).forEach(this::handleWrite);

    // required as write replacements do not care about the skipped iss extract node
    behavior.deleteUnusedDependencies();
  }

  private void handleRead(ReadMemNode read) {
    var loadSize = Tcg_8_16_32_64.fromWidth(
        read.words() * read.memory().wordSize()
    );
    // assign the default
    var issLoadNode = new IssLoadNode(read, TcgExtend.ZERO, loadSize, read.type());

    // optimize subsequent extracts if applicable
    if (read.usageCount() == 1) {
      var usage = read.usages().findFirst().get();
      if (usage instanceof IssConstExtractNode extract
          && loadSize.width == extract.fromWidth()) {
        // if the only user of this node is a constant extract node with a
        // from extract that matches the load size, we can
        // include the extraction directly in the TCG load
        issLoadNode = new IssLoadNode(read, extract.extendMode(), loadSize, extract.type());
        // delete the extract node
        extract.replaceByNothingAndDelete();
      }
    }

    read.replaceAndDelete(issLoadNode);
  }

  private void handleWrite(WriteMemNode write) {
    var storeSize = Tcg_8_16_32_64.fromWidth(
        write.words() * write.memory().wordSize()
    );

    if (write.value() instanceof IssConstExtractNode extract
        && extract.toWidth() >= storeSize.width
        && extract.isTruncate()
    ) {
      // if the value is a const extract node that truncates the original value
      // to the same or greater with as the storeSize, we can directly use the original value
      write.replaceInput(extract, extract.value());
    }

    var issStoreNode = new IssStoreNode(write, storeSize);
    write.replaceAndDelete(issStoreNode);
  }

}

