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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import vadl.viam.Definition;
import vadl.viam.MicroArchitecture;
import vadl.viam.RegisterTensor;
import vadl.viam.Resource;
import vadl.viam.Signal;
import vadl.viam.graph.Graph;

/**
 * HDL module created from the processor description.
 */
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

  private final Map<RegisterTensor, String> regReset = new HashMap<>();

  @Nullable
  private final Graph behavior;

  /**
   * Create new HDL module.
   *
   * @param context HDL emit context
   * @param definition Definition the module was created from (stage, logic element, ...)
   * @param name module name
   * @param resources list of resources the module contains
   * @param children list of child modules
   * @param behavior behavior graph
   */
  public HdlModule(HdlEmitContext context, @Nullable Definition definition,
                   String name, List<Resource> resources,
                   List<HdlModule> children, @Nullable Graph behavior) {
    this.context = context;
    this.definition = definition;
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

  public void addRegisterReset(RegisterTensor reg, String resetExpr) {
    regReset.put(reg, resetExpr);
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

  /**
   * Local names to the module, these are resource and child module names.
   *
   * @return set of local names
   */
  public Set<String> localNames() {
    var result = new HashSet<String>();
    resources.stream().map(Definition::simpleName).forEach(result::add);
    children.stream().map(HdlModule::name).forEach(result::add);
    return result;
  }

  /**
   * Create render variables for this module.
   *
   * @return map of render variables
   */
  public Map<String, Object> createVariables() {
    return Map.of(
        "name", name,
        "syncReset", false, //definition instanceof MicroArchitecture,
        "children", children.stream().map(this::childVars).toList(),
        "resources", resources.stream().map(this::resourceVars).toList(),
        "ports", ports.stream().map(this::portVars).toList(),
        "connections", connections.stream().map(this::connectionVars).toList()
    );
  }

  /**
   * Verify module behavior.
   */
  public void verify() {
    if (behavior != null) {
      behavior.verify();
    }
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
    var map = new HashMap<String, Object>();
    map.put("signal", resource instanceof Signal);
    map.put("name", resource.simpleName());
    map.put("resourceSize", size);
    map.put("resultType", HdlUtils.type(resource.resultType()));
    map.put("reset", regReset.get(resource));
    map.put("keepSignal", context.keepSignals());
    return map;
  }

  private Map<String, Object> portVars(HdlPort port) {
    return Map.of(
        "name", port.hdlName(),
        "ioType", port.getIOType(),
        "input", port.input(),
        "output", port.output()
    );
  }

  private Map<String, Object> connectionVars(HdlConnection connection) {
    var map = new HashMap<String, Object>();
    if (connection.output() != null) {
      map.put("isStatement", false);
      map.put("output", connection.output().rtlName());
      map.put("input", connection.input().rtlName());
      map.put("biDir", connection.biDir());
    } else {
      map.put("isStatement", true);
      map.put("statement", connection.input().rtlName());
    }
    if (connection.condition() != null) {
      map.put("isConditional", true);
      map.put("condition", connection.condition().rtlName());
    }
    return map;
  }
}
