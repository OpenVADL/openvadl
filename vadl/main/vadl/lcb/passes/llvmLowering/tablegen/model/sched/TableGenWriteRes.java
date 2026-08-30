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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import vadl.template.Renderable;

/**
 * Models a WriteRes (Write Resolution) from LLVM's Scheduling Machine Model.
 *
 * @param write The SchedWrite that is being written to.
 * @param usedResources The ProcResources that are being used. These are
 *                      expected as {@link ProcResourceUse} to allow enriching
 *                      them with cycle-exact acquire and release semantics.
 * @param latency
 */
public record TableGenWriteRes(
    TableGenSchedWrite write,
    List<ProcResourceUse> usedResources,
    int latency
) implements Renderable {

  @Override
  public Map<String, Object> renderObj() {
    return Map.of(
        "write", write,
        "latency", latency,
        "usedResources", usedResources
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(latency, usedResources);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof TableGenWriteRes other)
        || latency != other.latency
        || usedResources.size() != other.usedResources.size()
    ) {
      return false;
    }

    for (var usedResource : usedResources) {
      if (!other.usedResources.contains(usedResource)) {
        return false;
      }
    }

    return true;
  }

  public record ProcResourceUse(
      TableGenProcResource procResource,
      int acquireAtCycles,
      int releaseAtCycles
  ) implements Renderable {

    @Override
    public Map<String, Object> renderObj() {
      return Map.of(
          "procResource", procResource,
          "acquireAtCycles", acquireAtCycles,
          "releaseAtCycles", releaseAtCycles
      );
    }

    @Override
    public int hashCode() {
      return Objects.hash(procResource, releaseAtCycles);
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof ProcResourceUse other
          && procResource.equals(other.procResource)
          && acquireAtCycles == other.acquireAtCycles
          && releaseAtCycles == other.releaseAtCycles;
    }
  }
}
