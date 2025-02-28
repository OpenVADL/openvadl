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

package vadl.iss.template.gdb_xml;

import java.util.Map;
import vadl.configuration.IssConfiguration;
import vadl.iss.passes.IssGdbInfoExtractionPass;
import vadl.iss.template.IssTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Specification;

/**
 * This pass emits the {@code gdb-xml/gen-arch-cpu.xml} file which contains information about
 * the available CPU registers.
 * Each register has a name, size and type, as well as a index that is given by its position in
 * the XML structure.
 * The index is used by the {@link vadl.iss.template.target.EmitIssGdbStubPass} to
 * determine what register is accessed by a GDB request.
 */
public class EmitIssGdbXmlPass extends IssTemplateRenderingPass {
  public EmitIssGdbXmlPass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  protected String issTemplatePath() {
    return "gdb-xml/gen-arch-cpu.xml";
  }

  @Override
  protected boolean enableCopyright() {
    return false;
  }

  @Override
  protected Map<String, Object> createVariables(PassResults passResults,
                                                Specification specification) {
    var vars = super.createVariables(passResults, specification);
    var gdbInfo = passResults.lastResultOf(IssGdbInfoExtractionPass.class,
        IssGdbInfoExtractionPass.Result.class);
    vars.put("regs", gdbInfo.regs());
    return vars;
  }
}
