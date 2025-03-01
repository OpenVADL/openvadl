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

package vadl.cppCodeGen;

import vadl.cppCodeGen.context.CNodeContext;
import vadl.cppCodeGen.mixins.CDefaultMixins;
import vadl.viam.Function;

/**
 * Abstract base class responsible for generating C code from the expression nodes.
 * This class is intended not to rely on the {@code DispatchFor}.
 */
public abstract class AbstractFunctionCodeGenerator
    implements CDefaultMixins.AllExpressions, CDefaultMixins.Utils {
  protected final Function function;
  protected final StringBuilder builder;

  public AbstractFunctionCodeGenerator(Function function) {
    this.function = function;
    this.builder = new StringBuilder();
  }

  @Override
  public Function function() {
    return function;
  }

  @Override
  public StringBuilder builder() {
    return builder;
  }

  @Override
  public abstract CNodeContext context();
}
