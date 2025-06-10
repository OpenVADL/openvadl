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

package vadl.rtl.template;

import javax.annotation.Nullable;
import vadl.viam.Memory;
import vadl.viam.RegisterTensor;
import vadl.viam.Resource;
import vadl.viam.Signal;
import vadl.viam.ViamError;
import vadl.viam.graph.Node;

public record HdlPort(
    String name,
    Resource resource,
    boolean read,
    boolean output,
    @Nullable Node node
) {

  public boolean write() {
    return !read;
  }

  public boolean input() {
    return !output;
  }

  private String resolveIO(String type) {
    if (resource instanceof Signal) {
      if (input()) {
        return "Input(" + type + ")";
      } else {
        return "Output(" + type + ")";
      }
    }
    if (resource instanceof RegisterTensor && input()) {
      return "Flipped(" + type + ")";
    }
    if (resource instanceof Memory && input()) {
      return "Flipped(" + type + ")";
    }
    return type;
  }

  public String getIOType() {
    return resolveIO(getType());
  }

  public String getType() {
    if (resource instanceof Signal sig) {
      return HdlUtils.type(sig.resultType());
    }
    if (resource instanceof RegisterTensor reg) {
      if (reg.hasAddress()) {
        var addrType = reg.addressType();
        if (addrType != null) {
          if (read) {
            return "new VADL.RegFileReadPort(%s, %s)"
                .formatted(HdlUtils.type(reg.resultType()), addrType.bitWidth());
          } else {
            return "new VADL.RegFileWritePort(%s, %s)"
                .formatted(HdlUtils.type(reg.resultType()), addrType.bitWidth());
          }
        }
      } else {
        if (read) {
          return "new VADL.RegReadPort(%s)".formatted(HdlUtils.type(reg.resultType()));
        } else {
          return "new VADL.RegWritePort(%s)".formatted(HdlUtils.type(reg.resultType()));
        }
      }
    }
    if (resource instanceof Memory mem) {
      if (read) {
        return "new VADL.MemReadPort(%s, %s)"
            .formatted(HdlUtils.type(mem.resultType()), mem.addressType().bitWidth());
      } else {
        return "new VADL.MemWritePort(%s, %s)"
            .formatted(HdlUtils.type(mem.resultType()), mem.addressType().bitWidth());
      }
    }
    throw new ViamError("Can not emit resource %s", resource);
  }

  public String rtlName() {
    if (output) {
      return name + "_out";
    }
    return name + "_in";
  }

}
