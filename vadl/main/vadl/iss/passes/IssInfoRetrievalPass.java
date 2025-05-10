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
import vadl.iss.passes.extensions.ExceptionInfo;
import vadl.iss.passes.extensions.RegInfo;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Specification;

/**
 * Collects information about registers, exceptions that are raised by instructions in the ISA.
 * It constructs a {@link ExceptionInfo} extension that is added to the ISA.
 * Additionally, it constructs a {@link RegInfo} extension and
 * adds it to every register.
 */
public class IssInfoRetrievalPass extends AbstractIssPass {
  public IssInfoRetrievalPass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("ISS Exception Detection Pass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {

    var isa = viam.processor().get().isa();

    var info = new ExceptionInfo(configuration());
    isa.attachExtension(info);

    for (var exception : isa.exceptions()) {
      info.addException(exception);
    }

    isa.registerTensors().forEach(r -> {
      r.attachExtension(new RegInfo());
    });

    return null;
  }
}
