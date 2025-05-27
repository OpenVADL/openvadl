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

import java.util.HashSet;
import java.util.Objects;
import java.util.stream.Collectors;
import vadl.rtl.ipg.InstructionProgressGraph;
import vadl.viam.graph.dependency.ReadResourceNode;
import vadl.viam.graph.dependency.WriteResourceNode;

public class RtlWiring {

  public static void wire(RtlModule module) {
    var done = module.ports().stream().map(RtlPort::node).filter(Objects::nonNull)
        .collect(Collectors.toSet());
    var behavior = module.behavior();
    if (behavior == null) {
      return;
    }
    behavior.getNodes().filter(node -> !done.contains(node))
        .forEach(node -> {
          if (node instanceof ReadResourceNode read) {
            var name = module.context().name(node, read.resourceDefinition().simpleName());
            module.addPort(new RtlPort(name, read.resourceDefinition(), true, false,
                read));
          }
          if (node instanceof WriteResourceNode write) {
            var name = module.context().name(node, write.resourceDefinition().simpleName());
            module.addPort(new RtlPort(name, write.resourceDefinition(), false, false,
                write));
          }
        });

//    var names = module.ports().stream().map(RtlPort::name)
//        .collect(Collectors.toCollection(HashSet::new));
//    var groups = module.ports().stream().collect(Collectors.groupingBy(RtlPort::name));
//
//    groups.forEach((name, group) -> {
//      if (group.size() > 1) {
//        for (RtlPort port : group) {
//          if (port.node() != null
//              && port.node().ensureGraph() instanceof InstructionProgressGraph ipg) {
//            ipg.getContext(port.node()).shortestNameHint(names, 20)
//                .ifPresent(name -> port.n);
//          }
//        }
//      }
//    });
  }

}
