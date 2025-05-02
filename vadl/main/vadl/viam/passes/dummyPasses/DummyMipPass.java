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

package vadl.viam.passes.dummyPasses;

import java.io.IOException;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.Type;
import vadl.viam.Constant;
import vadl.viam.Function;
import vadl.viam.Identifier;
import vadl.viam.Parameter;
import vadl.viam.Processor;
import vadl.viam.Specification;
import vadl.viam.annotations.EnableHtifAnno;
import vadl.viam.graph.Graph;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * Adds a hardcoded RISC-V {@link Processor} definition to the VIAM specification.
 * This is deleted as soon as the frontend can handle the translation.
 */
public class DummyMipPass extends Pass {

  public DummyMipPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("Dummy Micro Processor");
  }

  @Override
  public @Nullable Object execute(PassResults passResults, Specification viam)
      throws IOException {

    if (viam.processor().isPresent()) {
      return null;
    }

    var isa = viam.isa().orElse(null);
    var abi = viam.abi().orElse(null);

    if (isa == null) {
      // if there is no isa nor abi, we just do nothing
      return null;
    }

    var ident = Identifier.noLocation("VADL");
    var mip = new Processor(
        ident,
        isa,
        abi,
        startFunc(ident),
        null,
        null,
        null
    );
    // enabled HTIF
    mip.addAnnotation(new EnableHtifAnno());

    viam.add(mip);
    return null;
  }


  private Function startFunc(Identifier parentIdent) {
    var type = Type.bits(64);
    var behavior = genericFuncBehavior(
        "start",
        new ConstantNode(Constant.Value.of(0x80000000L, type))
    );

    return new Function(
        parentIdent.append("start"),
        new Parameter[] {},
        type,
        behavior
    );
  }


  private Graph genericFuncBehavior(String name, ExpressionNode returnValue) {
    var behavior = new Graph(name);
    var ret = behavior.addWithInputs(new ReturnNode(returnValue));
    behavior.add(new StartNode(ret));
    return behavior;
  }

}
