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

package vadl.rtl.template;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import vadl.viam.Resource;
import vadl.viam.graph.Graph;

public class RtlModule {

  private RtlEmitContext context;

  @Nullable
  private RtlModule parent;

  private final String name;

  private final List<Resource> resources;

  private final List<RtlModule> children;

  private List<RtlPort> ports = new ArrayList<>();

  private List<RtlConnection> connections = new ArrayList<>();

  @Nullable
  private final Graph behavior;

  public RtlModule(RtlEmitContext context, @Nullable RtlModule parent, String name,
                   List<Resource> resources, List<RtlModule> children, @Nullable Graph behavior) {
    this.context = context;
    this.parent = parent;
    this.name = name;
    this.resources = resources;
    this.children = children;
    this.behavior = behavior;
  }

  public RtlEmitContext context() {
    return context;
  }

  @Nullable
  public RtlModule parent() {
    return parent;
  }

  public void setParent(@Nullable RtlModule parent) {
    this.parent = parent;
  }

  public String name() {
    return name;
  }

  public List<Resource> resources() {
    return resources;
  }

  public List<RtlModule> children() {
    return children;
  }

  @Nullable
  public Graph behavior() {
    return behavior;
  }

  public List<RtlPort> ports() {
    return ports;
  }

  public void addPort(RtlPort port) {
    ports.add(port);
  }

  public List<RtlConnection> connections() {
    return connections;
  }

  public void addConnection(RtlConnection connection) {
    connections.add(connection);
  }

  public Map<String, Object> createVariables() {
    return Map.of(
        "name", name,
        "children", children.stream().map(this::childVars).toList(),
        "resources", resources.stream().map(this::resourceVars).toList(),
        "ports", ports.stream().map(this::portVars).toList()
    );
  }

  private Map<String, Object> childVars(RtlModule module) {
    return Map.of(
        "name", module.name
    );
  }

  private Map<String, Object> resourceVars(Resource resource) {
    var size = 1;
    if (resource.hasAddress()) {
      var addrType = Objects.requireNonNull(resource.addressType());
      size = 1 << addrType.bitWidth();
    }
    return Map.of(
        "name", resource.simpleName(),
        "resourceSize", size,
        "resultType", RtlType.of(resource.resultType())
    );
  }

  private Map<String, Object> portVars(RtlPort port) {
    return Map.of(
        "name", port.name(),
        "ioType", port.getIOType(),
        "input", port.input(),
        "output", port.output()
    );
  }
}
