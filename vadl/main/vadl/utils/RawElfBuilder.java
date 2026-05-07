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

package vadl.utils;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A low-level API to emit a valid ELF64 file.
 */
public final class RawElfBuilder {

  public static final int PT_LOAD = 1;
  public static final int SHT_PROGBITS = 1;
  public static final int SHT_SYMTAB = 2;
  public static final int SHT_STRTAB = 3;
  public static final long SHF_WRITE = 0x1;
  public static final long SHF_ALLOC = 0x2;
  public static final long SHF_EXECINSTR = 0x4;
  public static final int STB_LOCAL = 0;
  public static final int STB_GLOBAL = 1;
  public static final int STT_FUNC = 2;
  public static final int STT_OBJECT = 1;
  public static final int STT_SECTION = 3;

  private static final int ELF64_HEADER_SIZE = 64;
  private static final int ELF64_PROGRAM_HEADER_SIZE = 56;
  private static final int ELF64_SECTION_HEADER_SIZE = 64;
  private static final int ELF64_SYMBOL_SIZE = 24;
  private static final int ET_EXEC = 2;
  private static final int EM_NONE = 0;
  private static final int EV_CURRENT = 1;
  private static final int ELFCLASS64 = 2;
  private static final int ELFDATA2LSB = 1;
  private static final int SHN_UNDEF = 0;

  private final long entryPoint;
  private final List<Section> sections = new ArrayList<>();
  private final List<Segment> segments = new ArrayList<>();
  private final List<Symbol> symbols = new ArrayList<>();
  private final Map<String, Integer> strtab = new LinkedHashMap<>();
  private final ByteArrayOutputStream strtabBytes = new ByteArrayOutputStream();

  public RawElfBuilder(long entryPoint) {
    this.entryPoint = entryPoint;
    addString(""); // Index 0 is always empty string
  }

  public void addSection(String name, int type, long flags, long addr, byte[] data, int align) {
    sections.add(new Section(name, type, flags, addr, data, align));
  }

  public void addSegment(int type, int flags, long vaddr, long paddr, long filesz, long memsz,
                         long align) {
    segments.add(
        new Segment(type, flags, vaddr, paddr, filesz, memsz, align));
  }

  public void addSymbol(String name, long value, long size, int info, int shndx) {
    symbols.add(new Symbol(name, value, size, info, shndx));
  }

  public static int info(int bind, int type) {
    return (bind << 4) | (type & 0xf);
  }

  private int addString(String s) {
    if (strtab.containsKey(s)) {
      return strtab.get(s);
    }
    int offset = strtabBytes.size();
    byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
    strtabBytes.writeBytes(bytes);
    strtabBytes.write(0);
    strtab.put(s, offset);
    return offset;
  }

  /** Serializes the ELF file and returns the resulting bytes. */
  public byte[] build() {
    ByteArrayOutputStream shstrtabOut = new ByteArrayOutputStream();
    Map<String, Integer> shstrtabOffsets = new LinkedHashMap<>();
    shstrtabOffsets.put("", putString(shstrtabOut, ""));
    for (Section s : sections) {
      shstrtabOffsets.put(s.name, putString(shstrtabOut, s.name));
    }
    shstrtabOffsets.put(".symtab", putString(shstrtabOut, ".symtab"));
    shstrtabOffsets.put(".strtab", putString(shstrtabOut, ".strtab"));
    shstrtabOffsets.put(".shstrtab", putString(shstrtabOut, ".shstrtab"));

    ByteArrayOutputStream symtabOut = new ByteArrayOutputStream();
    writeSym(symtabOut, 0, 0, 0, SHN_UNDEF, 0, 0);
    for (Symbol sym : symbols) {
      writeSym(symtabOut, addString(sym.name), sym.info, 0, sym.shndx, sym.value, sym.size);
    }
    byte[] symtab = symtabOut.toByteArray();
    byte[] strtabData = strtabBytes.toByteArray();

    int segmentOffset = 0x1000;
    int currentOffset = segmentOffset;
    for (Section s : sections) {
      if (s.addr != 0) {
        // This is a bit of a hack to match VectorBench64Assembler's layout
        // where it assumes a fixed mapping from VADDR to offset in the load segment.
        s.offset = segmentOffset + (int) (s.addr - entryPoint);
      } else {
        s.offset = currentOffset;
      }
      currentOffset = Math.max(currentOffset, s.offset + s.data.length);
    }
    int imageSize = currentOffset - segmentOffset;

    int symtabOffset = alignUp(segmentOffset + imageSize, 8);
    int strtabOffset = alignUp(symtabOffset + symtab.length, 8);
    int shstrtabOffset = alignUp(strtabOffset + strtabData.length, 8);
    byte[] shstrtab = shstrtabOut.toByteArray();
    int shoff = alignUp(shstrtabOffset + shstrtab.length, 8);
    int sectionCount = 1 + sections.size() + 3; // NULL + sections + .symtab + .strtab + .shstrtab

    ByteBuffer elf = ByteBuffer.allocate(shoff + sectionCount * ELF64_SECTION_HEADER_SIZE)
        .order(ByteOrder.LITTLE_ENDIAN);

    // ELF Header
    elf.put((byte) 0x7f).put((byte) 'E').put((byte) 'L').put((byte) 'F');
    elf.put((byte) ELFCLASS64);
    elf.put((byte) ELFDATA2LSB);
    elf.put((byte) EV_CURRENT);
    elf.put((byte) 0);
    elf.put(new byte[8]);
    elf.putShort((short) ET_EXEC);
    elf.putShort((short) EM_NONE);
    elf.putInt(EV_CURRENT);
    elf.putLong(entryPoint);
    elf.putLong(ELF64_HEADER_SIZE);
    elf.putLong(shoff);
    elf.putInt(0);
    elf.putShort((short) ELF64_HEADER_SIZE);
    elf.putShort((short) ELF64_PROGRAM_HEADER_SIZE);
    elf.putShort((short) segments.size());
    elf.putShort((short) ELF64_SECTION_HEADER_SIZE);
    elf.putShort((short) sectionCount);
    elf.putShort((short) (sectionCount - 1)); // shstrtab is last

    // Program Headers
    elf.position(ELF64_HEADER_SIZE);
    for (Segment seg : segments) {
      elf.putInt(seg.type);
      elf.putInt(seg.flags);
      elf.putLong(seg.offsetInFile);
      elf.putLong(seg.vaddr);
      elf.putLong(seg.paddr);
      elf.putLong(seg.filesz);
      elf.putLong(seg.memsz);
      elf.putLong(seg.align);
    }

    // Write sections data
    for (Section s : sections) {
      elf.position(s.offset);
      elf.put(s.data);
    }
    elf.position(symtabOffset);
    elf.put(symtab);
    elf.position(strtabOffset);
    elf.put(strtabData);
    elf.position(shstrtabOffset);
    elf.put(shstrtab);

    // Section Headers
    elf.position(shoff);
    writeSectionHeader(elf, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0); // NULL section
    for (Section s : sections) {
      Integer nameOffset = shstrtabOffsets.get(s.name);
      if (nameOffset == null) {
        throw new IllegalStateException("Section name not found in shstrtab: " + s.name);
      }
      writeSectionHeader(elf, nameOffset, s.type, s.flags, s.addr, s.offset,
          s.data.length, 0, 0, s.align, 0);
    }
    Integer symtabNameOffset = shstrtabOffsets.get(".symtab");
    if (symtabNameOffset == null) {
      throw new IllegalStateException(".symtab not found in shstrtab");
    }
    writeSectionHeader(elf, symtabNameOffset, SHT_SYMTAB, 0, 0, symtabOffset,
        symtab.length, sectionCount - 2, symbols.size() + 1, 8, ELF64_SYMBOL_SIZE);
    Integer strtabNameOffset = shstrtabOffsets.get(".strtab");
    if (strtabNameOffset == null) {
      throw new IllegalStateException(".strtab not found in shstrtab");
    }
    writeSectionHeader(elf, strtabNameOffset, SHT_STRTAB, 0, 0, strtabOffset,
        strtabData.length, 0, 0, 1, 0);
    Integer shstrtabNameOffset = shstrtabOffsets.get(".shstrtab");
    if (shstrtabNameOffset == null) {
      throw new IllegalStateException(".shstrtab not found in shstrtab");
    }
    writeSectionHeader(elf, shstrtabNameOffset, SHT_STRTAB, 0, 0, shstrtabOffset,
        shstrtab.length, 0, 0, 1, 0);

    return elf.array();
  }

  private void writeSym(ByteArrayOutputStream out, int name, int info, int other, int shndx,
                        long value, long size) {
    ByteBuffer buf = ByteBuffer.allocate(ELF64_SYMBOL_SIZE).order(ByteOrder.LITTLE_ENDIAN);
    buf.putInt(name);
    buf.put((byte) info);
    buf.put((byte) other);
    buf.putShort((short) shndx);
    buf.putLong(value);
    buf.putLong(size);
    out.writeBytes(buf.array());
  }

  private void writeSectionHeader(ByteBuffer elf, int name, int type, long flags, long addr,
                                  long offset, long size, int link, int info, long addralign,
                                  long entsize) {
    elf.putInt(name);
    elf.putInt(type);
    elf.putLong(flags);
    elf.putLong(addr);
    elf.putLong(offset);
    elf.putLong(size);
    elf.putInt(link);
    elf.putInt(info);
    elf.putLong(addralign);
    elf.putLong(entsize);
  }

  private int putString(ByteArrayOutputStream out, String value) {
    int offset = out.size();
    out.writeBytes(value.getBytes(StandardCharsets.UTF_8));
    out.write(0);
    return offset;
  }

  private int alignUp(int value, int alignment) {
    return (value + alignment - 1) & ~(alignment - 1);
  }

  private static class Section {
    final String name;
    final int type;
    final long flags;
    final long addr;
    final byte[] data;
    final int align;
    int offset;

    Section(String name, int type, long flags, long addr, byte[] data, int align) {
      this.name = name;
      this.type = type;
      this.flags = flags;
      this.addr = addr;
      this.data = data;
      this.align = align;
    }
  }

  private static class Segment {
    final int type;
    final int flags;
    final long vaddr;
    final long paddr;
    final long filesz;
    final long memsz;
    final long align;
    final long offsetInFile = 0x1000; // Simplified for this use case

    Segment(int type, int flags, long vaddr, long paddr, long filesz, long memsz, long align) {
      this.type = type;
      this.flags = flags;
      this.vaddr = vaddr;
      this.paddr = paddr;
      this.filesz = filesz;
      this.memsz = memsz;
      this.align = align;
    }
  }

  private static class Symbol {
    final String name;
    final long value;
    final long size;
    final int info;
    final int shndx;

    Symbol(String name, long value, long size, int info, int shndx) {
      this.name = name;
      this.value = value;
      this.size = size;
      this.info = info;
      this.shndx = shndx;
    }
  }
}
