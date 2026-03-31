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

import java.util.Map;
import java.util.Objects;
import vadl.cppCodeGen.model.GcbImmediateExtractionCppFunction;
import vadl.gcb.valuetypes.VariantKind;
import vadl.viam.Identifier;
import vadl.viam.Relocation;

/**
 * A logical relocation is helper construct for the {@link vadl.viam.Relocation}.
 * In contrast to {@link AutomaticallyGeneratedRelocation} are {@link UserSpecifiedRelocation}
 * always user generated like {@code %lo} or {@code %hi} in risc-v.
 */
public class UserSpecifiedRelocation extends CompilerRelocation
    implements RelocationsBeforeElfExpansion {
  protected final Modifier modifier;
  protected final VariantKind variantKind;
  protected final GcbImmediateExtractionCppFunction valueRelocation;

  /**
   * Constructor.
   */
  public UserSpecifiedRelocation(
      Identifier identifier,
      Modifier modifier,
      VariantKind variantKind,
      GcbImmediateExtractionCppFunction valueRelocation,
      Relocation originalRelocation) {
    super(identifier, originalRelocation);
    this.modifier = modifier;
    this.variantKind = variantKind;
    this.valueRelocation = valueRelocation;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UserSpecifiedRelocation that = (UserSpecifiedRelocation) o;
    return relocationRef == that.relocationRef;
  }

  @Override
  public int hashCode() {
    return Objects.hash(relocationRef);
  }

  @Override
  public Map<String, Object> renderObj() {
    var obj = super.renderObj();
    obj.put("variantKind", variantKind);
    obj.put("name", identifier.simpleName());
    return obj;
  }

  @Override
  public Modifier modifier() {
    return modifier;
  }

  @Override
  public VariantKind variantKind() {
    return variantKind;
  }

  @Override
  public GcbImmediateExtractionCppFunction valueRelocation() {
    return valueRelocation;
  }
}
