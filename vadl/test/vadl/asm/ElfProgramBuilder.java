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

import java.util.ArrayList;
import java.util.List;
import vadl.utils.RawElfBuilder;

/**
 * Builds executable ELF images from an {@link AssemblerSession} plus explicit layout policy.
 *
 * <p>This layer is intentionally thin. It does not choose addresses, symbols, or load segments on
 * its own; callers provide those explicitly while reusing session-managed section bytes.
 *
 * <p>Typical usage:
 *
 * <pre>{@code
 * var session = new AssemblerSession();
 * var text = session.section(".text");
 * text.label("start");
 * text.emit32LittleEndian(0);
 *
 * var builder = new ElfProgramBuilder(session, 0x8000_0000L);
 * int textSection = builder.addResolvedSessionSection(
 *     ".text", ".text", ElfProgramBuilder.SHF_ALLOC | ElfProgramBuilder.SHF_EXECINSTR,
 *     0x8000_0000L, 4);
 * builder.addLoadSegment(ElfProgramBuilder.PT_LOAD, 0x7, 0x8000_0000L, 0x8000_0000L,
 *     4, 4, 0x1000);
 * builder.addSymbol("_start", 0x8000_0000L, 0,
 *     ElfProgramBuilder.info(ElfProgramBuilder.STB_GLOBAL, ElfProgramBuilder.STT_FUNC),
 *     textSection);
 *
 * byte[] elf = builder.build();
 * }</pre>
 */
public final class ElfProgramBuilder {

  public static final int PT_LOAD = RawElfBuilder.PT_LOAD;
  public static final int SHT_PROGBITS = RawElfBuilder.SHT_PROGBITS;
  public static final long SHF_WRITE = RawElfBuilder.SHF_WRITE;
  public static final long SHF_ALLOC = RawElfBuilder.SHF_ALLOC;
  public static final long SHF_EXECINSTR = RawElfBuilder.SHF_EXECINSTR;
  public static final int STB_LOCAL = RawElfBuilder.STB_LOCAL;
  public static final int STB_GLOBAL = RawElfBuilder.STB_GLOBAL;
  public static final int STT_FUNC = RawElfBuilder.STT_FUNC;
  public static final int STT_OBJECT = RawElfBuilder.STT_OBJECT;
  public static final int STT_SECTION = RawElfBuilder.STT_SECTION;

  private record SectionSpec(String elfName, int type, long flags, long addr, byte[] data,
                             int align) {
  }

  private record SegmentSpec(int type, int flags, long vaddr, long paddr, long filesz, long memsz,
                             long align) {
  }

  private record SymbolSpec(String name, long value, long size, int info, int shndx) {
  }

  private final AssemblerSession session;
  private final long entryPoint;
  private final List<SectionSpec> sections = new ArrayList<>();
  private final List<SegmentSpec> segments = new ArrayList<>();
  private final List<SymbolSpec> symbols = new ArrayList<>();

  public ElfProgramBuilder(AssemblerSession session, long entryPoint) {
    this.session = session;
    this.entryPoint = entryPoint;
  }

  public int addResolvedSessionSection(String elfSectionName, String sessionSectionName,
                                       long flags, long addr, int align) {
    return addResolvedSessionSection(elfSectionName, sessionSectionName, SHT_PROGBITS, flags, addr,
        align);
  }

  public int addResolvedSessionSection(String elfSectionName, String sessionSectionName, int type,
                                       long flags, long addr, int align) {
    sections.add(new SectionSpec(elfSectionName, type, flags, addr,
        session.resolvedSectionBytes(sessionSectionName), align));
    return sections.size();
  }

  public int addRawSessionSection(String elfSectionName, String sessionSectionName, long flags,
                                  long addr, int align) {
    return addRawSessionSection(elfSectionName, sessionSectionName, SHT_PROGBITS, flags, addr,
        align);
  }

  public int addRawSessionSection(String elfSectionName, String sessionSectionName, int type,
                                  long flags, long addr, int align) {
    sections.add(new SectionSpec(elfSectionName, type, flags, addr,
        session.rawSectionBytes(sessionSectionName), align));
    return sections.size();
  }

  public int addSection(String elfSectionName, int type, long flags, long addr, byte[] data,
                        int align) {
    sections.add(new SectionSpec(elfSectionName, type, flags, addr, data, align));
    return sections.size();
  }

  public void addLoadSegment(int type, int flags, long vaddr, long paddr, long filesz, long memsz,
                             long align) {
    segments.add(new SegmentSpec(type, flags, vaddr, paddr, filesz, memsz, align));
  }

  public void addSymbol(String name, long value, long size, int info, int shndx) {
    symbols.add(new SymbolSpec(name, value, size, info, shndx));
  }

  public byte[] build() {
    var elfBuilder = new RawElfBuilder(entryPoint);
    for (var section : sections) {
      elfBuilder.addSection(section.elfName(), section.type(), section.flags(), section.addr(),
          section.data(), section.align());
    }
    for (var segment : segments) {
      elfBuilder.addSegment(segment.type(), segment.flags(), segment.vaddr(), segment.paddr(),
          segment.filesz(), segment.memsz(), segment.align());
    }
    for (var symbol : symbols) {
      elfBuilder.addSymbol(symbol.name(), symbol.value(), symbol.size(), symbol.info(),
          symbol.shndx());
    }
    return elfBuilder.build();
  }

  public static int info(int bind, int type) {
    return RawElfBuilder.info(bind, type);
  }
}
