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

package vadl.dump.entitySuppliers;

import java.util.List;
import vadl.dump.DumpEntitySupplier;
import vadl.dump.entities.VdtEntity;
import vadl.pass.PassResults;
import vadl.vdt.model.Node;
import vadl.vdt.passes.VdtLoweringPass;
import vadl.viam.Specification;

/**
 * A {@link DumpEntitySupplier} that produces a {@link VdtEntity} for the VDT tree.
 */
public class VdtEntitySupplier implements DumpEntitySupplier<VdtEntity> {

  @Override
  public List<VdtEntity> getEntities(Specification spec, PassResults passResults) {

    if (!passResults.hasRunPassOnce(VdtLoweringPass.class)) {
      // Nothing to do here
      return List.of();
    }

    var vdt = passResults.lastResultOf(VdtLoweringPass.class, Node.class);
    return List.of(new VdtEntity(vdt));
  }
}
