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

package vadl.iss.passes.extensions;

import java.util.Map;
import javax.annotation.Nullable;
import vadl.configuration.IssConfiguration;
import vadl.iss.codegen.IssMemoryRegionInitCodeGen;
import vadl.template.Renderable;
import vadl.viam.Definition;
import vadl.viam.DefinitionExtension;
import vadl.viam.MemoryRegion;

/**
 * A {@link MemoryRegion} extension containing information about the memory region for
 * code generation.
 *
 * <p>This information is collected and added to the {@link MemoryRegion} by the
 * {@link vadl.iss.passes.IssMemoryDetectionPass}.</p>
 *
 * @see vadl.iss.passes.IssMemoryDetectionPass
 * @see IssMemoryRegionInitCodeGen
 * @see MemoryRegion
 */
public class MemoryRegionInfo extends DefinitionExtension<MemoryRegion>
    implements Renderable {

  @Nullable
  Map<String, Object> renderObj;
  final boolean isMainRam;
  final IssConfiguration config;
  // the size of the vector used in the region initialization (gen-machine.c:init_region())
  final int initVecSize;

  /**
   * Constructs the memory region info.
   *
   * @param isMainRam   specifies if this memory region is the main RAM.
   * @param initVecSize the size of the vector used in the region initialization
   *                    ({@code gen-machine.c:init_region()})
   */
  public MemoryRegionInfo(boolean isMainRam, IssConfiguration config, int initVecSize) {
    this.isMainRam = isMainRam;
    this.config = config;
    this.initVecSize = initVecSize;
  }

  public MemoryRegion memReg() {
    return extendingDef();
  }

  public String name() {
    return memReg().simpleName();
  }

  public int initVecSize() {
    return initVecSize;
  }

  public String treeName() {
    return config.targetName().toLowerCase() + "." + config.machineName().toLowerCase() + "."
        + name().toLowerCase();
  }

  public String enumName() {
    return config.machineName().toUpperCase() + "_" + name().toUpperCase();
  }

  public int sizeInTable() {
    var size = memReg().size();
    return size == null ? 0 : size;
  }

  @Override
  public Class<? extends Definition> extendsDefClass() {
    return MemoryRegion.class;
  }

  @Override
  public Map<String, Object> renderObj() {
    if (renderObj == null) {
      renderObj = Map.of(
          "name", name(),
          "name_lower", name().toLowerCase(),
          "tree_name", treeName(),
          "enum_name", enumName(),
          "init_func_name", "memory_region_init_" + memReg().kind().name().toLowerCase(),
          "is_main_ram", isMainRam,
          "region_reference", isMainRam ? "machine->ram" : name().toLowerCase(),
          "region_size", "0x" + Integer.toString(sizeInTable(), 16),
          "region_base", "0x" + memReg().expectBase().toString(16)
      );
    }
    return renderObj;
  }
}
