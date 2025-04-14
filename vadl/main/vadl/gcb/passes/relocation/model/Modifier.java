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

package vadl.gcb.passes.relocation.model;

import static vadl.viam.ViamError.ensureNonNull;

import java.util.Map;
import java.util.Optional;
import vadl.error.Diagnostic;
import vadl.gcb.valuetypes.RelocationCtx;
import vadl.gcb.valuetypes.RelocationFunctionLabel;
import vadl.template.Renderable;
import vadl.viam.Format;
import vadl.viam.Relocation;

/**
 * Represents the transformation functions(?) in the assembler during fixups.
 */
public record Modifier(String value,
                       CompilerRelocation.Kind kind,
                       Optional<RelocationFunctionLabel> relocationFunctionLabel)
    implements Renderable {

  /**
   * Create a modifier for a VIAM relocation.
   */
  public static Modifier from(Relocation relocation) {
    var name = relocation.identifier.lower();
    var kind = relocation.isAbsolute() ? CompilerRelocation.Kind.ABSOLUTE
        : CompilerRelocation.Kind.RELATIVE;

    // Check the label for the relocation
    var ctx = ensureNonNull(relocation.extension(RelocationCtx.class),
        () -> Diagnostic.error("Expected a relocation label", relocation.sourceLocation()));

    return new Modifier("MO_" + name, kind, Optional.of(ctx.label()));
  }

  /**
   * Create an absolute modifier for an {@link AutomaticallyGeneratedRelocation}.
   */
  public static Modifier absolute(Format.Field imm) {
    return new Modifier("MO_ABS_" + imm.identifier.lower(),
        CompilerRelocation.Kind.ABSOLUTE,
        Optional.empty());
  }

  /**
   * Create a relative modifier for an {@link AutomaticallyGeneratedRelocation}.
   */
  public static Modifier relative(Format.Field imm) {
    return new Modifier("MO_REL_" + imm.identifier.lower(), CompilerRelocation.Kind.RELATIVE,
        Optional.empty());
  }

  @Override
  public Map<String, Object> renderObj() {
    return Map.of("value", value);
  }
}
