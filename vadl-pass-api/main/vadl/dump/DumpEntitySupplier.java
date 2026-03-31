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

package vadl.dump;

import java.util.List;
import vadl.dump.entities.DefinitionEntity;
import vadl.pass.PassResults;
import vadl.viam.Specification;

/**
 * DumpEntitySuppliers produce a list of {@link DumpEntity}s from a given VIAM specification
 * and PassResults.
 * Those entities are rendered as boxes in the HTML dump.
 * The most important supplier is the {@link vadl.dump.entitySuppliers.ViamEntitySupplier} that
 * returns a list of {@link DefinitionEntity}s representing
 * all definitions in the VIAM specification.
 */
public interface DumpEntitySupplier<T extends DumpEntity> {

  /**
   * Returns a list of DumpEntities produced from the given spec and pass results.
   */
  List<T> getEntities(Specification spec, PassResults passResults);

}
