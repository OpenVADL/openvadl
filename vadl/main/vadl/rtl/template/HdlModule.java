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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import vadl.viam.Definition;
import vadl.viam.Resource;
import vadl.viam.Signal;
import vadl.viam.graph.Graph;

public class HdlModule {

  private final HdlEmitContext context;

  @Nullable
  private final Definition definition;

  @Nullable
  private HdlModule parent;

  private final String name;

  private final List<Resource> resources;

  private final List<HdlModule> children;

  private final List<HdlPort> ports = new ArrayList<>();

  private final List<HdlConnection> connections = new ArrayList<>();

  @Nullable
  private final Graph behavior;

  public HdlModule(HdlEmitContext context, @Nullable Definition definition,
                   @Nullable HdlModule parent, String name, List<Resource> resources,
                   List<HdlModule> children, @Nullable Graph behavior) {
    this.context = context;
    this.definition = definition;
    this.parent = parent;
    this.name = name;
    this.resources = resources;
    this.children = children;
    this.behavior = behavior;
  }

  public HdlEmitContext context() {
    return context;
  }

  @Nullable
  public Definition definition() {
    return definition;
  }

  @Nullable
  public HdlModule parent() {
    return parent;
  }

  public void setParent(@Nullable HdlModule parent) {
    this.parent = parent;
  }

  public String name() {
    return name;
  }

  public List<Resource> resources() {
    return resources;
  }

  public void addResource(Resource resource) {
    resources.add(resource);
  }

  public List<HdlModule> children() {
    return children;
  }

  @Nullable
  public Graph behavior() {
    return behavior;
  }

  public List<HdlPort> ports() {
    return ports;
  }

  public void addPort(HdlPort port) {
    ports.add(port);
  }

  public List<HdlConnection> connections() {
    return connections;
  }

  public void addConnection(HdlConnection connection) {
    connections.add(connection);
  }

  public Set<String> portNames() {
    return ports.stream().map(HdlPort::name).collect(Collectors.toCollection(HashSet::new));
  }

  public Set<String> localNames() {
    var result = new HashSet<String>();
    resources.stream().map(Definition::simpleName).forEach(result::add);
    children.stream().map(HdlModule::name).forEach(result::add);
    return result;
  }

  public Map<String, Object> createVariables() {
    return Map.of(
        "name", name,
        "children", children.stream().map(this::childVars).toList(),
        "resources", resources.stream().map(this::resourceVars).toList(),
        "ports", ports.stream().map(this::portVars).toList(),
        "connections", connections.stream().map(this::connectionVars).toList()
    );
  }

  private Map<String, Object> childVars(HdlModule module) {
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
        "signal", resource instanceof Signal,
        "name", resource.simpleName(),
        "resourceSize", size,
        "resultType", HdlUtils.type(resource.resultType())
    );
  }

  private Map<String, Object> portVars(HdlPort port) {
    return Map.of(
        "name", port.rtlName(),
        "ioType", port.getIOType(),
        "input", port.input(),
        "output", port.output()
    );
  }

  private Map<String, Object> connectionVars(HdlConnection connection) {
    return Map.of(
        "output", connection.output().rtlName(),
        "input", connection.input().rtlName(),
        "biDir", connection.biDir()
    );
  }
}
