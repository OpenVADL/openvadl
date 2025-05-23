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

package vadl.iss.aarch64;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import vadl.iss.IssInstrTest;

public abstract class AbstractIssAarch64InstrTest extends IssInstrTest {

  // set at the bottom of this file
  static final Map<String, String> GDB_REF_MAP = new HashMap<>();

  @Override
  public Map<String, String> gdbRegMap() {
    return GDB_REF_MAP;
  }

  @Override
  protected List<String> withUpstreamTargets() {
    return List.of("aarch64-softmmu");
  }

  @Override
  public Tool simulator() {
    return new Tool("/qemu/build/qemu-system-a64", "-bios");
  }

  @Override
  public Tool reference() {
    // we use one-insn-per-tb, otherwise the PC wouldn't be updated at the time of reading
    // it from the plugin
    return new Tool("/qemu/build/qemu-system-aarch64",
        "-M virt -cpu cortex-a57 -semihosting -accel tcg,one-insn-per-tb=on -kernel");
  }

  @Override
  public Tool compiler() {
    return new Tool("/scripts/compilers/aarch64_compiler.py", "");
  }

  static {
    GDB_REF_MAP.put("s0", "x0");
    GDB_REF_MAP.put("s1", "x1");
    GDB_REF_MAP.put("s2", "x2");
    GDB_REF_MAP.put("s3", "x3");
    GDB_REF_MAP.put("s4", "x4");
    GDB_REF_MAP.put("s5", "x5");
    GDB_REF_MAP.put("s6", "x6");
    GDB_REF_MAP.put("s7", "x7");
    GDB_REF_MAP.put("s8", "x8");
    GDB_REF_MAP.put("s9", "x9");
    GDB_REF_MAP.put("s10", "x10");
    GDB_REF_MAP.put("s11", "x11");
    GDB_REF_MAP.put("s12", "x12");
    GDB_REF_MAP.put("s13", "x13");
    GDB_REF_MAP.put("s14", "x14");
    GDB_REF_MAP.put("s15", "x15");
    GDB_REF_MAP.put("s16", "x16");
    GDB_REF_MAP.put("s17", "x17");
    GDB_REF_MAP.put("s18", "x18");
    GDB_REF_MAP.put("s19", "x19");
    GDB_REF_MAP.put("s20", "x20");
    GDB_REF_MAP.put("s21", "x21");
    GDB_REF_MAP.put("s22", "x22");
    GDB_REF_MAP.put("s23", "x23");
    GDB_REF_MAP.put("s24", "x24");
    GDB_REF_MAP.put("s25", "x25");
    GDB_REF_MAP.put("s26", "x26");
    GDB_REF_MAP.put("s27", "x27");
    GDB_REF_MAP.put("s28", "x28");
    GDB_REF_MAP.put("s29", "x29");
    GDB_REF_MAP.put("s30", "x30");
    GDB_REF_MAP.put("s31", "sp");
    GDB_REF_MAP.put("pc", "pc");
  }

}


