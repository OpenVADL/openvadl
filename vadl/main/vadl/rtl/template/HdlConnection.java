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

import javax.annotation.Nullable;
import vadl.viam.Resource;
import vadl.viam.Signal;
import vadl.viam.graph.Node;

/**
 * Record representing a connection or a statement in HDL.
 *
 * @param output    output endpoint of this connection, if this is null the record represents a
 *                  statement without an assignment.
 * @param input     input endpoint to this connection
 * @param biDir     if the signals in this connection should be connected bi-directionally
 * @param condition conditional assignment or statement
 */
public record HdlConnection(
    @Nullable Endpoint output,
    Endpoint input,
    boolean biDir,
    @Nullable Endpoint condition
) {

  /**
   * HDL Endpoint.
   */
  public interface Endpoint {
    /**
     * Name of this endpoint in HDL.
     *
     * @return name
     */
    String rtlName();
  }

  /**
   * References a port on the containing module or a child module.
   *
   * @param child optional child module
   * @param port HDL port
   */
  public record PortEndpoint(@Nullable HdlModule child, HdlPort port) implements Endpoint {
    @Override
    public String rtlName() {
      if (child != null) {
        return child.name() + ".io." + port.hdlName();
      }
      return "io." + port.hdlName();
    }
  }

  /**
   * Create a connection between two port endpoints.
   *
   * @param end1 port endpoint
   * @param end2 port endpoint
   * @return connection, not bidirectional for signals
   */
  public static HdlConnection of(PortEndpoint end1, PortEndpoint end2) {
    if (end1.port().resource() instanceof Signal && end2.port().resource() instanceof Signal) {
      var child1 = end1.child() != null && end2.child() == null;
      var child2 = end1.child() == null && end2.child() != null;
      var childBoth = end1.child() != null && end2.child() != null;
      var swap = false;
      if (child1 && end1.port().output() && end2.port().output()) {
        swap = true;
      }
      if (child2 && end1.port().input() && end2.port().input()) {
        swap = true;
      }
      if (childBoth && end1.port().output() && end2.port().input()) {
        swap = true;
      }
      if (swap) {
        return new HdlConnection(end2, end1, false, null);
      } else {
        return new HdlConnection(end1, end2, false, null);
      }
    }
    return new HdlConnection(end1, end2, true, null);
  }

  /**
   * References a resource of the containing module. This is either a resource from the VIAM or a
   * resource created during HDL generation to hold the value of a node from the module's behavior.
   *
   * @param resource resource of the containing module
   * @param node optional module behavior node, if this resource holds the value of this node
   */
  public record ResourceEndpoint(Resource resource, @Nullable Node node) implements Endpoint {
    @Override
    public String rtlName() {
      return resource.simpleName();
    }
  }

  /**
   * References an HDL expression given as a string.
   *
   * @param node node for which this expression was generated
   * @param expression expression
   */
  public record ExpressionEndpoint(Node node, String expression) implements Endpoint {
    @Override
    public String rtlName() {
      return expression;
    }
  }

  /**
   * Checks if an endpoint is equal to one of the endpoints of this connection.
   *
   * @param endpoint endpoint
   * @return true if the connection includes this endpoint
   */
  public boolean connects(Endpoint endpoint) {
    return ((output != null && output.equals(endpoint)) || input.equals(endpoint));
  }

}
