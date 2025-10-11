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

import java.util.HashSet;
import java.util.Set;
import vadl.rtl.ipg.nodes.RtlReadMemNode;
import vadl.rtl.ipg.nodes.RtlWriteMemNode;
import vadl.viam.Memory;
import vadl.viam.RegisterTensor;
import vadl.viam.Resource;
import vadl.viam.Signal;
import vadl.viam.ViamError;
import vadl.viam.graph.Node;

/**
 * Input/output port on an HDL module.
 *
 * @param name     Port name
 * @param resource Resource this port reads/writes
 * @param read     True, if this is a read or write port.
 * @param output   True, if this is an output (write nodes create outputs, reads nodes inputs).
 * @param nodes    Read/write nodes this port is connected to. This set is extended when signal or
 *                 register read ports are merged.
 */
public record HdlPort(
    String name,
    Resource resource,
    boolean read,
    boolean output,
    Set<Node> nodes
) {

  public HdlPort(String name, Resource resource, boolean read, boolean output, Node node) {
    this(name, resource, read, output, new HashSet<>(Set.of(node)));
  }

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
    } else {
      if ((read() && input()) || (write() && output())) {
        return "Flipped(" + type + ")";
      }
    }
    return type;
  }

  public String getIOType() {
    return resolveIO(getType());
  }

  /**
   * Type of this port in the HDL description.
   *
   * @return HDL type
   */
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
        var maxWords = nodes.stream()
            .mapToInt(node -> (node instanceof RtlReadMemNode rd) ? rd.maxWords() : 0)
            .max().getAsInt();
        return "new VADL.MemReadPort(%s, %s, %s)"
            .formatted(HdlUtils.type(mem.resultType()), maxWords, mem.addressType().bitWidth());
      } else {
        var maxWords = nodes.stream()
            .mapToInt(node -> (node instanceof RtlWriteMemNode wr) ? wr.maxWords() : 0)
            .max().getAsInt();
        return "new VADL.MemWritePort(%s, %s, %s)"
            .formatted(HdlUtils.type(mem.resultType()), maxWords, mem.addressType().bitWidth());
      }
    }
    throw new ViamError("Can not emit resource %s", resource);
  }

  /**
   * Name of this port in the HDL description.
   *
   * @return HDL name
   */
  public String hdlName() {
    if (output) {
      return name + "_out";
    }
    return name + "_in";
  }

}
