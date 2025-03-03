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

package vadl.cppCodeGen.context;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import vadl.viam.graph.Node;

/**
 * A code generation context for {@link Node}s.
 * It will handle pass all generation calls to the dispatch
 * function passed to the constructor.
 */
public class CNodeContext extends CGenContext<Node> {

  protected BiConsumer<CNodeContext, Node> dispatch;

  /**
   * Construct a new code generation context for {@link Node}s.
   *
   * @param writer   The writer that handles string passed by handlers.
   * @param dispatch The dispatch method used when requesting generation of some node.
   */
  public CNodeContext(Consumer<String> writer,
                      BiConsumer<CNodeContext, Node> dispatch) {
    super(writer, "");
    this.dispatch = dispatch;
  }


  /**
   * Construct a new code generation context for {@link Node}s.
   *
   * @param writer   The writer that handles string passed by handlers.
   * @param prefix   The string which should be emitted before writing to {@code writer}.
   * @param dispatch The dispatch method used when requesting generation of some node.
   */
  public CNodeContext(Consumer<String> writer,
                      String prefix,
                      BiConsumer<CNodeContext, Node> dispatch) {
    super(writer, prefix);
    this.dispatch = dispatch;
  }

  @Override
  public CGenContext<Node> gen(Node entity) {
    dispatch.accept(this, entity);
    return this;
  }

  @Override
  public String genToString(Node node) {
    var builder = new StringBuilder();
    var subContext = new CNodeContext(builder::append, dispatch);
    subContext.gen(node);
    return builder.toString();
  }
}
