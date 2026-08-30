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

package vadl.lcb.passes.llvmLowering.tablegen.model.sched;

import java.util.Map;
import java.util.Objects;
import vadl.template.Renderable;

/**
 * Models a ProcResource (Processor Resource) from LLVM's Machine Scheduling
 * Model.
 *
 * @param name Name of the ProcResource. This will be used to render the .td
 *             file.
 * @param bufferSize bufferSize attribute of the ProcResource. This indicates
 *                   how many simultaneous in-flight instructions the processor
 *                   resource can handle.
 */
public record TableGenProcResource(
    String name,
    int bufferSize
) implements Renderable {

  @Override
  public Map<String, Object> renderObj() {
    return Map.of(
        "name", name,
        "bufferSize", bufferSize
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, bufferSize);
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof TableGenProcResource other
        && name.equals(other.name)
        && bufferSize == other.bufferSize;
  }
}
