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

import java.util.List;
import java.util.Map;
import vadl.iss.IssInstrTest;

public abstract class AbstractIssRiscv32InstrTest extends IssInstrTest {

  @Override
  protected List<String> withUpstreamTargets() {
    return List.of("riscv32-softmmu");
  }

  @Override
  public Map<String, String> gdbRegMap() {
    return AbstractIssRiscv64InstrTest.GDB_REF_MAP;
  }

  @Override
  public Tool simulator() {
    return new Tool("/qemu/build/qemu-system-rv32imzicsr", "-bios");
  }

  @Override
  public Tool reference() {
    return new Tool("/qemu/build/qemu-system-riscv32", "-M spike -bios");
  }

  @Override
  public Tool compiler() {
    return new Tool("/scripts/compilers/riscv_compiler.py", "-march=rv32imzicsr -mabi=ilp32");
  }

}
