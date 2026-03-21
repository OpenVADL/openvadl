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

package vadl.iss.passes;

import java.io.IOException;
import javax.annotation.Nullable;
import vadl.configuration.IssConfiguration;
import vadl.iss.passes.nodes.IssReadRegNode;
import vadl.iss.passes.nodes.ReadStaticRegTensorNode;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Specification;
import vadl.viam.annotations.TinyBlockStateAnnotation;
import vadl.viam.graph.Graph;

/**
 * Determines if a {@link IssReadRegNode} accesses a register that is saved in the tiny
 * block state and can be done with a static register read (from the DisasContext).
 * If so, then the node is replaced with a {@link ReadStaticRegTensorNode}.
 */
public class IssStaticAccessConversionPass extends AbstractIssPass {

  public IssStaticAccessConversionPass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("ISS Static Access Conversion");
  }

  @Override
  public @Nullable Object execute(PassResults passResults, Specification viam)
      throws IOException {

    tcgInstrs(viam).forEach(i -> new IssStaticAccessConverter(i.behavior()).run());
    return null;
  }
}

class IssStaticAccessConverter {

  Graph graph;

  public IssStaticAccessConverter(Graph graph) {
    this.graph = graph;
  }

  void run() {

    // replace read reg nodes of TB state regs to be just a
    // CpuReg access of the ISS (No tcg op required)
    graph.getNodes(IssReadRegNode.class)
        .filter(this::isStaticRead)
        .forEach(n -> n.replaceAndDelete(new ReadStaticRegTensorNode(n.regTensor())));

    // writes need to remain tcg operations. chaining, jumping and exiting the
    // tb translation loop is handled in TcgOpLoweringPass
  }

  private boolean isStaticRead(IssReadRegNode node) {
    return node.regTensor().hasAnnotation(TinyBlockStateAnnotation.class);
    /*
    // TODO: this is a slow prototype, optimize
    var reg = node.regTensor();
    var annotation = reg.annotation(ExecutionStateAnnotation.class);
    if (annotation == null) {
      // non-static access
      return false;
    }
    var staticSlice = annotation.staticSlice();
    if (staticSlice == null) {
      // the whole underlying register is static
      return true;
    }
    return switch (node.accessKind()) {
      case BASE -> sliceCoversWholeRead(staticSlice, node);
      case ALIAS -> {
        var aliasRes = node.aliasResource();
        if (aliasRes == null) {
          // TODO: should never happen
          yield false;
        }
        var aliasSlice = aliasRes.semantics().aliasSlice();
        if (aliasSlice == null) {
          // no slice -> read whole register
          yield sliceCoversWholeRead(staticSlice, node);
        }
        // check if all read bits are static
        var staticBits = staticSlice.stream().boxed().collect(Collectors.toSet());
        yield aliasSlice.stream().allMatch(staticBits::contains);
      }
    };
    */
  }

  /*
  private boolean sliceCoversWholeRead(Constant.BitSlice slice, IssReadRegNode read) {
    long readBitCount = read.type().bitWidth();
    long sliceBitCount = slice.stream().distinct().count();
    return readBitCount <= sliceBitCount;
  }
  */

}
