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
import vadl.viam.Resource;
import vadl.viam.Signal;

/**
 * Wires HDL module ports after they are created by {@link HdlBehavior} from module behavior.
 * Ports are connected either to other child modules containing the resource required by the port
 * or up to the parent module. This is repeated until either the resource is found inside an HDL
 * module and connected or the port is connected to a port on the top module.
 */
public class HdlWiring {

  /**
   * Wire HDL module ports. Connect unconnected ports until all are connected.
   *
   * @param modules list of HDL modules
   */
  public static void wire(List<HdlModule> modules) {
    var change = true;
    while (change) {
      change = false;
      for (HdlModule module : modules) {
        change |= wire(module);
      }
    }
  }

  /**
   * Wire HDL module ports.
   *
   * @param module HDL module
   * @return True, if any new connections were made.
   */
  public static boolean wire(HdlModule module) {
    var change = false;

    // find unconnected ports
    for (HdlPort port : new ArrayList<>(module.ports())) {
      var endpoint = new HdlConnection.PortEndpoint(null, port);
      change |= connect(module, endpoint);
    }
    for (HdlModule child : module.children()) {
      for (HdlPort port : new ArrayList<>(child.ports())) {
        var endpoint = new HdlConnection.PortEndpoint(child, port);
        change |= connect(module, endpoint);
      }
    }

    return change;
  }

  private static boolean connect(HdlModule module, HdlConnection.PortEndpoint end) {
    if (isNodePort(module, end.port())
        || module.connections().stream().anyMatch(c -> c.connects(end))) {
      return false; // already connected
    }

    if (module.resources().contains(end.port().resource())) {
      // connect locally
      module.addConnection(new HdlConnection(
          end,
          new HdlConnection.ResourceEndpoint(end.port().resource(), null),
          false, null // ?
      ));
    } else {
      // find resource in children
      var otherChild = module.children().stream()
          .filter(oc -> containsResource(oc, end.port().resource())).findAny();
      if (otherChild.isPresent()) {
        // connect other child
        var name = module.context().name(end.port().nodes(), otherChild.get().portNames(),
            end.port().name());
        var otherPort = new HdlPort(name, end.port().resource(), end.port().read(),
            !end.port().output(), end.port().nodes());
        otherPort = addOrMergePort(otherChild.get(), otherPort);
        module.addConnection(
            HdlConnection.of(end, new HdlConnection.PortEndpoint(otherChild.get(), otherPort)));
      } else {
        // add port up in the hierarchy
        var name = module.context().name(end.port().nodes(), module.portNames(),
            end.port().name());
        var upPort = new HdlPort(name, end.port().resource(), end.port().read(),
            end.port().output(), end.port().nodes());
        upPort = addOrMergePort(module, upPort);
        module.addConnection(
            HdlConnection.of(end, new HdlConnection.PortEndpoint(null, upPort)));
      }
    }

    return true;
  }

  private static boolean isNodePort(HdlModule module, HdlPort port) {
    if (port.nodes().isEmpty()) {
      return false;
    }
    var behavior = module.behavior();
    return (behavior != null && behavior.getNodes().anyMatch(port.nodes()::contains));
  }

  private static boolean containsResource(HdlModule module, Resource resource) {
    return module.resources().contains(resource)
        || module.children().stream().anyMatch(child -> containsResource(child, resource));
  }

  private static HdlPort addOrMergePort(HdlModule module, HdlPort port) {
    var result = port;
    if (port.read() && !port.resource().hasAddress()) {
      var existing = module.ports().stream()
          .filter(p -> p.resource().equals(port.resource()) && p.read())
          .findAny();
      if (existing.isPresent()) {
        existing.get().nodes().addAll(port.nodes());
        result = existing.get();
      }
    }

    if (!module.ports().contains(result)) {
      module.addPort(result);
    }

    return result;
  }

}
