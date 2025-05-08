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

package vadl.iss.template.hw;

import java.util.List;
import java.util.Map;
import vadl.configuration.IssConfiguration;
import vadl.iss.codegen.IssMemoryRegionInitCodeGen;
import vadl.iss.passes.extensions.MemoryRegionInfo;
import vadl.iss.template.IssTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.MemoryRegion;
import vadl.viam.Specification;
import vadl.viam.annotations.EnableHtifAnno;

/**
 * Emits the {@code hw/gen-arch/gen-machine.c} which is the core implementation for the
 * virtual hardware board.
 * It defines things like memory, start address, HTIF, firmware loading, etc...
 *
 * @see IssMemoryRegionInitCodeGen
 * @see MemoryRegionInfo
 */
public class EmitIssHwMachineCPass extends IssTemplateRenderingPass {

  public EmitIssHwMachineCPass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  protected String issTemplatePath() {
    return "hw/gen-arch/gen-machine.c";
  }

  @Override
  protected Map<String, Object> createVariables(PassResults passResults,
                                                Specification specification) {
    var vars = super.createVariables(passResults, specification);
    vars.put("dram_base", getDramBaseExpr());
    // the start address of the firmware, when we don't load an elf
    vars.put("firmware_base_addr", getFirmwareBaseAddress(specification));
    vars.put("htif_enabled", htifEnabled(specification));
    vars.put("mem_region_inits", getMemoryRegionInits(specification));
    return vars;
  }


  private String getDramBaseExpr() {
    // TODO: Don't hardcode this
    return "0x80000000";
  }

  private String getFirmwareBaseAddress(Specification specification) {
    return "0x" + specification.processor().get().memoryRegions().stream()
        .filter(MemoryRegion::holdsFirmware)
        .findFirst().get().expectBase().toString(16);
  }

  private boolean htifEnabled(Specification specification) {
    var mip = specification.processor().orElse(null);
    specification.ensure(mip != null, "No MicroProcessor definition found");
    return mip.hasAnnotation(EnableHtifAnno.class);
  }

  private List<Map<String, Object>> getMemoryRegionInits(Specification specification) {
    return specification.processor().get().memoryRegions().stream()
        .filter(MemoryRegion::hasInitialization)
        .map(r -> Map.of(
            "mem", r.expectExtension(MemoryRegionInfo.class),
            "function",
            new IssMemoryRegionInitCodeGen(r.expectExtension(MemoryRegionInfo.class),
                configuration())
                .fetch()))
        .toList();
  }

}
