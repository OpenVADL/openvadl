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

package vadl.rtl.analysis;

import java.util.Set;
import javax.annotation.Nullable;
import vadl.viam.Definition;
import vadl.viam.DefinitionExtension;
import vadl.viam.Resource;
import vadl.viam.Stage;
import vadl.viam.graph.dependency.ReadResourceNode;
import vadl.viam.graph.dependency.WriteResourceNode;

/**
 * Hazard analysis for a resource.
 */
public class HazardAnalysis extends DefinitionExtension<Resource> {

  /**
   * Hazard analysis for a read node.
   *
   * @param node read node
   * @param effect stage the read takes effect
   * @param condition stage, in which the condition is known
   * @param address stage, in which the address is known
   */
  public record ReadAnalysis(ReadResourceNode node, Stage effect, Stage condition,
                             @Nullable Stage address) {
  }

  /**
   * Hazard analysis for a write node.
   *
   * @param node write node
   * @param effect stage the write takes effect
   * @param condition stage, in which the condition is known
   * @param address stage, in which the address is known
   * @param value stage, in which the value is known
   */
  public record WriteAnalysis(WriteResourceNode node, Stage effect, Stage condition,
                              @Nullable Stage address, Stage value) {
  }

  private final Resource resource;
  private final Set<ReadAnalysis> reads;
  private final Set<WriteAnalysis> writes;

  /**
   * Create new hazard analysis for a resource.
   *
   * @param resource resource
   * @param reads set of read analyses
   * @param writes set of write analyses
   */
  public HazardAnalysis(Resource resource, Set<ReadAnalysis> reads,
                        Set<WriteAnalysis> writes) {
    this.resource = resource;
    this.reads = reads;
    this.writes = writes;
  }

  public Resource resource() {
    return resource;
  }

  public Set<ReadAnalysis> reads() {
    return reads;
  }

  public Set<WriteAnalysis> writes() {
    return writes;
  }

  @Override
  public Class<? extends Definition> extendsDefClass() {
    return Resource.class;
  }
}
