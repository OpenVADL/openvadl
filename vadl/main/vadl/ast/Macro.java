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

package vadl.ast;

import java.util.List;
import java.util.Map;

record Macro(Identifier name, List<MacroParam> params, Node body, SyntaxType returnType,
             Map<String, Node> boundArguments)
    implements MacroOrPlaceholder {
}

record MacroParam(Identifier name, SyntaxType type) {
}

interface IsMacroInstance {
  MacroOrPlaceholder macroOrPlaceholder();
}

sealed interface MacroOrPlaceholder permits Macro, MacroPlaceholder {
  SyntaxType returnType();
}

record MacroPlaceholder(ProjectionType syntaxType, List<String> segments)
    implements MacroOrPlaceholder {
  @Override
  public SyntaxType returnType() {
    return syntaxType.resultType;
  }
}
