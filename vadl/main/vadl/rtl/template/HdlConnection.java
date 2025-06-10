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
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;

public record HdlConnection(
    Endpoint output,
    Endpoint input,
    boolean biDir
) {

  public interface Endpoint {
    String rtlName();
  }

  public record PortEndpoint(@Nullable HdlModule child, HdlPort port) implements Endpoint {
    @Override
    public String rtlName() {
      if (child != null) {
        return child.name() + ".io." + port.rtlName();
      }
      return "io." + port.rtlName();
    }
  }

  public record ResourceEndpoint(Resource resource, @Nullable Node node) implements Endpoint {
    @Override
    public String rtlName() {
      return resource.simpleName();
    }
  }

  public record ExpressionEndpoint(ExpressionNode node, String expression) implements Endpoint {
    @Override
    public String rtlName() {
      return expression;
    }
  }

  public boolean connects(Endpoint endpoint) {
    return (output.equals(endpoint) || input.equals(endpoint));
  }

}
