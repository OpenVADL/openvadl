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

import java.util.Arrays;

/**
 * Enum of all the assembly directives that can be used in the VADL language.
 */
public enum AsmDirective {
  SET,
  EQU,
  EQUIV,
  ASCII,
  ASCIZ,
  STRING,
  BYTE,
  SHORT,
  VALUE,
  BYTE2("2byte"),
  LONG,
  INT,
  BYTE4("4byte"),
  QUAD,
  BYTE8("8byte"),
  OCTA,
  SINGLE,
  FLOAT,
  DOUBLE,
  ALIGN_POW2("align"),
  ALIGN_BYTE("align"),
  ALIGN32_POW2("align32"),
  ALIGN32_BYTE("align32"),
  BALIGN,
  BALIGNW,
  BALIGNL,
  P2ALIGN,
  P2ALIGNW,
  P2ALIGNL,
  ORG,
  FILL,
  ZERO,
  EXTERN,
  GLOBL,
  GLOBAL,
  LAZY_REFERENCE,
  NO_DEAD_STRIP,
  SYMBOL_RESOLVER,
  PRIVATE_EXTERN,
  REFERENCE,
  WEAK_DEFINITION,
  WEAK_REFERENCE,
  WEAK_DEF_CAN_BE_HIDDEN,
  COLD,
  COMM,
  COMMON,
  LCOMM,
  ABORT,
  INCLUDE,
  INCBIN,
  CODE16,
  CODE16GCC,
  REPT,
  REP,
  IRP,
  IRPC,
  ENDR,
  BUNDLE_ALIGN_MODE,
  BUNDLE_LOCK,
  BUNDLE_UNLOCK,
  IF,
  IFEQ,
  IFGE,
  IFGT,
  IFLE,
  IFLT,
  IFNE,
  IFB,
  IFNB,
  IFC,
  IFEQS,
  IFNC,
  IFNES,
  IFDEF,
  IFNDEF,
  IFNOTDEF,
  ELSEIF,
  ELSE,
  END,
  ENDIF,
  SKIP,
  SPACE,
  FILE,
  LINE,
  LOC,
  STABS,
  CV_FILE,
  CV_FUNC_ID,
  CV_LOC,
  CV_LINETABLE,
  CV_INLINE_LINETABLE,
  CV_INLINE_SITE_ID,
  CV_DEF_RANGE,
  CV_STRING,
  CV_STRINGTABLE,
  CV_FILECHECKSUMS,
  CV_FILECHECKSUM_OFFSET("cv_filechecksumoffset"),
  CV_FPO_DATA,
  SLEB128,
  ULEB128,
  CFI_SECTIONS,
  CFI_STARTPROC,
  CFI_ENDPROC,
  CFI_DEF_CFA,
  CFI_DEF_CFA_OFFSET,
  CFI_ADJUST_CFA_OFFSET,
  CFI_DEF_CFA_REGISTER,
  CFI_LLVM_DEF_ASPACE_CFA,
  CFI_OFFSET,
  CFI_REL_OFFSET,
  CFI_PERSONALITY,
  CFI_LSDA,
  CFI_REMEMBER_STATE,
  CFI_RESTORE_STATE,
  CFI_SAME_VALUE,
  CFI_RESTORE,
  CFI_ESCAPE,
  CFI_RETURN_COLUMN,
  CFI_SIGNAL_FRAME,
  CFI_UNDEFINED,
  CFI_REGISTER,
  CFI_WINDOW_SAVE,
  CFI_B_KEY_FRAME,
  CFI_MTE_TAGGED_FRAME,
  MACROS_ON,
  MACROS_OFF,
  MACRO,
  EXITM,
  ENDM,
  ENDMACRO,
  PURGEM,
  ERR,
  ERROR,
  WARNING,
  ALTMACRO,
  NOALTMACRO,
  RELOC,
  DC,
  DC_A_CODEPOINTER4("dc.a"),
  DC_A_CODEPOINTER8("dc.a"),
  DC_B("dc.b"),
  DC_D("dc.d"),
  DC_L("dc.l"),
  DC_S("dc.s"),
  DC_W("dc.w"),
  DC_X("dc.x"),
  DCB,
  DCB_B("dcb.b"),
  DCB_D("dcb.d"),
  DCB_L("dcb.l"),
  DCB_S("dcb.s"),
  DCB_W("dcb.w"),
  DCB_X("dcb.x"),
  DS,
  DS_B("ds.b"),
  DS_D("ds.d"),
  DS_L("ds.l"),
  DS_P("ds.p"),
  DS_S("ds.s"),
  DS_W("ds.w"),
  DS_X("ds.x"),
  PRINT,
  ADDRSIG,
  ADDRSIG_SYM,
  PSEUDO_PROBE("pseudoprobe"),
  LTO_DISCARD,
  LTO_SET_CONDITIONAL,
  MEMTAG;

  private final String asmName;

  AsmDirective(String asmName) {
    this.asmName = asmName;
  }

  AsmDirective() {
    this.asmName = this.name().toLowerCase();
  }

  /**
   * Get the assembly name of the directive.
   *
   * @return The assembly name of the directive.
   */
  public String getAsmName() {
    return "." + asmName;
  }

  /**
   * Check if a given string is an assembly directive.
   *
   * @param name The name to check.
   * @return True if the name is an assembly directive, false otherwise.
   */
  public static boolean isAsmDirective(String name) {
    return Arrays.stream(AsmDirective.values()).anyMatch(dir -> dir.toString().equals(name));
  }
}
