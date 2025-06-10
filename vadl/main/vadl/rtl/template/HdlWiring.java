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

import java.util.List;
import vadl.viam.Resource;

public class HdlWiring {

  public static void wire(List<HdlModule> modules) {
    var change = true;
    while (change) {
      change = false;
      for (HdlModule module : modules) {
        change |= wire(module);
      }
    }
  }

  public static boolean wire(HdlModule module) {
    var change = false;

    // find unconnected ports
    for (HdlPort port : module.ports()) {
      var endpoint = new HdlConnection.PortEndpoint(null, port);
      change |= connect(module, endpoint);
    }
    for (HdlModule child : module.children()) {
      for (HdlPort port : child.ports()) {
        var endpoint = new HdlConnection.PortEndpoint(child, port);
        change |= connect(module, endpoint);
      }
    }

    return change;
  }

  private static boolean connect(HdlModule module, HdlConnection.PortEndpoint end) {
    if (module.connections().stream().anyMatch(c -> c.connects(end))) {
      return false; // already connected
    }

    if (module.resources().contains(end.port().resource())) {
      // connect locally
      module.addConnection(new HdlConnection(
          end,
          new HdlConnection.ResourceEndpoint(end.port().resource(), null),
          false // ?
      ));
    } else {
      // find resource in children
      var otherChild = module.children().stream()
          .filter(oc -> containsResource(oc, end.port().resource())).findAny();
      if (otherChild.isPresent()) {
        // connect other child
        var otherPort = new HdlPort(end.port().name(), end.port().resource(), end.port().read(),
            !end.port().output(), null);
        otherChild.get().addPort(otherPort);
        module.addConnection(new HdlConnection(
            end,
            new HdlConnection.PortEndpoint(otherChild.get(), otherPort),
            true
        ));
      } else {
        // add port up in the hierarchy
        var upPort = new HdlPort(end.port().name(), end.port().resource(), end.port().read(),
            end.port().output(), null);
        module.addPort(upPort);
        module.addConnection(new HdlConnection(
            end,
            new HdlConnection.PortEndpoint(null, upPort),
            true
        ));
      }
    }

    return true;
  }

  private static boolean containsResource(HdlModule module, Resource resource) {
    return module.resources().contains(resource)
        || module.children().stream().anyMatch(child -> containsResource(child, resource));
  }

}
