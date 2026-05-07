// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.asm;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal symbolic assembler session for programmatic test emitters.
 *
 * <p>The session owns named sections, section-local labels, and deferred fixups that rewrite
 * section bytes after all labels are known. It intentionally does not know anything about ISA
 * encoding or ELF layout.
 *
 * <p>Typical usage:
 *
 * <pre>{@code
 * var session = new AssemblerSession();
 * var text = session.section(".text");
 * var data = session.section(".data");
 *
 * text.label("start");
 * text.emit32LittleEndian(0);
 * session.addLabelFixup(".text", 0, ".text", "done", (context, bytes) ->
 *     bytes.putInt(context.sourceOffset(), context.relativeOffset()));
 *
 * data.label("result");
 * data.align(16);
 * data.writeBytes(new byte[128]);
 *
 * text.label("done");
 * var textBytes = session.resolvedSectionBytes(".text");
 * }</pre>
 */
public final class AssemblerSession {

  @FunctionalInterface
  public interface LabelFixupPatcher {

    void patch(LabelFixupContext context, ByteBuffer sectionBytes);
  }

  public record LabelFixupContext(
      String sourceSectionName,
      int sourceOffset,
      String targetSectionName,
      int targetOffset
  ) {

    public int relativeOffset() {
      return targetOffset - sourceOffset;
    }
  }

  public static final class Section {

    private final String name;
    private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    private final Map<String, Integer> labels = new LinkedHashMap<>();

    private Section(String name) {
      this.name = name;
    }

    public String name() {
      return name;
    }

    public int size() {
      return bytes.size();
    }

    public void label(String labelName) {
      if (labels.putIfAbsent(labelName, size()) != null) {
        throw new IllegalArgumentException("Duplicate label `%s` in section `%s`."
            .formatted(labelName, name));
      }
    }

    public void emit32LittleEndian(int word) {
      bytes.write(word & 0xff);
      bytes.write((word >>> 8) & 0xff);
      bytes.write((word >>> 16) & 0xff);
      bytes.write((word >>> 24) & 0xff);
    }

    public void writeBytes(byte[] data) {
      bytes.writeBytes(data);
    }

    public void align(int alignment) {
      if (alignment <= 0) {
        throw new IllegalArgumentException("Alignment must be positive: " + alignment);
      }
      int mod = size() % alignment;
      if (mod == 0) {
        return;
      }
      bytes.writeBytes(new byte[alignment - mod]);
    }

    public byte[] rawBytes() {
      return bytes.toByteArray();
    }

    public Map<String, Integer> labels() {
      return Map.copyOf(labels);
    }

    private int labelOffset(String labelName) {
      var offset = labels.get(labelName);
      if (offset == null) {
        throw new IllegalStateException("Unknown label `%s` in section `%s`."
            .formatted(labelName, name));
      }
      return offset;
    }
  }

  private record LabelFixup(
      String sourceSectionName,
      int sourceOffset,
      String targetSectionName,
      String targetLabel,
      LabelFixupPatcher patcher
  ) {
  }

  private final Map<String, Section> sections = new LinkedHashMap<>();
  private final List<LabelFixup> labelFixups = new ArrayList<>();

  public Section section(String name) {
    return sections.computeIfAbsent(name, Section::new);
  }

  public int labelOffset(String sectionName, String labelName) {
    return section(sectionName).labelOffset(labelName);
  }

  public Map<String, Integer> labels(String sectionName) {
    return section(sectionName).labels();
  }

  public byte[] rawSectionBytes(String sectionName) {
    return section(sectionName).rawBytes();
  }

  public void addLabelFixup(String sourceSectionName, int sourceOffset, String targetSectionName,
                            String targetLabel, LabelFixupPatcher patcher) {
    labelFixups.add(new LabelFixup(sourceSectionName, sourceOffset, targetSectionName, targetLabel,
        patcher));
  }

  public byte[] resolvedSectionBytes(String sectionName) {
    var raw = section(sectionName).rawBytes();
    var buffer = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN);
    for (var fixup : labelFixups) {
      if (!fixup.sourceSectionName().equals(sectionName)) {
        continue;
      }
      var context = new LabelFixupContext(
          fixup.sourceSectionName(),
          fixup.sourceOffset(),
          fixup.targetSectionName(),
          labelOffset(fixup.targetSectionName(), fixup.targetLabel()));
      fixup.patcher().patch(context, buffer);
    }
    return raw;
  }
}
