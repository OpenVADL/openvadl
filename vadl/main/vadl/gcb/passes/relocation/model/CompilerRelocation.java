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

import java.util.HashMap;
import java.util.Map;
import vadl.template.Renderable;
import vadl.viam.Identifier;
import vadl.viam.Relocation;

/**
 * {@link CompilerRelocation} is a super class to hold
 * both {@link UserSpecifiedRelocation} and {@link AutomaticallyGeneratedRelocation}.
 */
public abstract class CompilerRelocation implements Renderable {
  protected final Identifier identifier;
  protected final CompilerRelocation.Kind kind;
  protected final Relocation relocationRef;

  /**
   * Determines what kind of relocation this is.
   * A relocation is relative when it patches a value based on the previous value.
   * More concretely, it is relative when reads from the PC.
   * A relocation is absolute when the patched value overwrites the previous value.
   */
  public enum Kind implements Renderable {
    RELATIVE,
    ABSOLUTE,
    GLOBAL_OFFSET_TABLE;

    /**
     * Returns {@code true} when kind is {@code RELATIVE} or {@code GLOBAL_OFFSET_TABLE}.
     */
    public boolean isRelative() {
      return this == RELATIVE || this == GLOBAL_OFFSET_TABLE;
    }

    @Override
    public Map<String, Object> renderObj() {
      return Map.of(
          "name", this.name(),
          "isRelative", this.isRelative()
      );
    }

    /**
     * Returns {@code true} when kind is {@code ABSOLUTE}.
     */
    public boolean isAbsolute() {
      return this == ABSOLUTE;
    }

    /**
     * Maps a {@link CompilerRelocation.Kind} to a {@link Relocation.Kind}.
     */
    public Relocation.Kind map() {
      return switch (this) {
        case ABSOLUTE -> Relocation.Kind.ABSOLUTE;
        case RELATIVE -> Relocation.Kind.RELATIVE;
        case GLOBAL_OFFSET_TABLE -> Relocation.Kind.GLOBAL_OFFSET_TABLE;
      };
    }

    /**
     * Get the {@link CompilerRelocation.Kind} from a {@link Relocation.Kind}.
     */
    public static Kind fromRelocationKind(Relocation.Kind kind) {
      return switch (kind) {
        case ABSOLUTE -> ABSOLUTE;
        case RELATIVE -> RELATIVE;
        case GLOBAL_OFFSET_TABLE -> GLOBAL_OFFSET_TABLE;
      };
    }

    /**
     * Maps a {@link CompilerRelocation.Kind} to a LLVM relocation kind.
     */
    public String llvmKind() {
      return switch (this) {
        case RELATIVE -> "R_PC";
        case ABSOLUTE -> "R_ABS";
        case GLOBAL_OFFSET_TABLE -> "R_GOT_PC";
      };
    }
  }

  /**
   * Constructor.
   */
  public CompilerRelocation(
      Identifier identifier,
      Relocation relocationRef
  ) {
    this(identifier,
        Kind.fromRelocationKind(relocationRef.kind()),
        relocationRef);
  }

  /**
   * Constructor.
   */
  public CompilerRelocation(
      Identifier identifier,
      Kind kind,
      Relocation relocationRef
  ) {
    this.identifier = identifier;
    this.kind = kind;
    this.relocationRef = relocationRef;
  }

  public Kind kind() {
    return kind;
  }

  public Relocation relocation() {
    return relocationRef;
  }

  public Identifier identifier() {
    return identifier;
  }

  /**
   * Get the ELF name.
   */
  public ElfRelocationName elfRelocationName() {
    return new ElfRelocationName(
        "R_" + relocation().identifier.lower());
  }

  @Override
  public Map<String, Object> renderObj() {
    var obj = new HashMap<String, Object>();
    obj.put("llvmKind", kind.llvmKind());
    obj.put("elfRelocationName", elfRelocationName());
    obj.put("relocation", Map.of(
        "name", relocation().simpleName()
    ));
    return obj;
  }
}
