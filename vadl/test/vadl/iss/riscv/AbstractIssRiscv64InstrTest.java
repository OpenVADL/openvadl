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

package vadl.iss.riscv;

import java.util.HashMap;
import java.util.Map;
import vadl.iss.IssInstrTest;

public abstract class AbstractIssRiscv64InstrTest extends IssInstrTest {

  // set at the bottom of this file
  static final Map<String, String> GDB_REF_MAP = new HashMap<>();

  @Override
  public Map<String, String> gdbRegMap() {
    return GDB_REF_MAP;
  }

  @Override
  public Tool simulator() {
    return new Tool("/qemu/build/qemu-system-rv64im", "-bios");
  }

  @Override
  public Tool reference() {
    return new Tool("/qemu/build/qemu-system-riscv64", "-M spike -bios");
  }

  @Override
  public Tool compiler() {
    return new Tool("/scripts/compilers/riscv_compiler.py", "-march=rv64im -mabi=lp64");
  }

  static {
    GDB_REF_MAP.put("x0", "zero");
    GDB_REF_MAP.put("x1", "ra");
    GDB_REF_MAP.put("x2", "sp");
    GDB_REF_MAP.put("x3", "gp");
    GDB_REF_MAP.put("x4", "tp");
    GDB_REF_MAP.put("x5", "t0");
    GDB_REF_MAP.put("x6", "t1");
    GDB_REF_MAP.put("x7", "t2");
    GDB_REF_MAP.put("x8", "fp");
    GDB_REF_MAP.put("x9", "s1");
    GDB_REF_MAP.put("x10", "a0");
    GDB_REF_MAP.put("x11", "a1");
    GDB_REF_MAP.put("x12", "a2");
    GDB_REF_MAP.put("x13", "a3");
    GDB_REF_MAP.put("x14", "a4");
    GDB_REF_MAP.put("x15", "a5");
    GDB_REF_MAP.put("x16", "a6");
    GDB_REF_MAP.put("x17", "a7");
    GDB_REF_MAP.put("x18", "s2");
    GDB_REF_MAP.put("x19", "s3");
    GDB_REF_MAP.put("x20", "s4");
    GDB_REF_MAP.put("x21", "s5");
    GDB_REF_MAP.put("x22", "s6");
    GDB_REF_MAP.put("x23", "s7");
    GDB_REF_MAP.put("x24", "s8");
    GDB_REF_MAP.put("x25", "s9");
    GDB_REF_MAP.put("x26", "s10");
    GDB_REF_MAP.put("x27", "s11");
    GDB_REF_MAP.put("x28", "t3");
    GDB_REF_MAP.put("x29", "t4");
    GDB_REF_MAP.put("x30", "t5");
    GDB_REF_MAP.put("x31", "t6");
    GDB_REF_MAP.put("pc", "pc");
  }

}
