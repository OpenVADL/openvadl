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

package vadl.ast;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import org.junit.jupiter.api.Test;
import vadl.utils.VirtualFileSystem;

public class VirtualFileSystemTest {


  @Test
  void testMockedVFS() throws IOException {
    // Let's parse a file that doesn't exist on disk but is only injected into the compiler via
    // the virtual filesystem (VFS).
    // The file also imports another pseudo file that only exists in the VFS.
    VadlParser.parse(Paths.get("firstPseudoFile.vadl"), new TestVFS());
  }

  private static class TestVFS implements VirtualFileSystem {
    private final Map<String, String> files = Map.of(
        "firstPseudoFile.vadl", "import secondPseudoFile::magicNumber \nconstant flo = $magicNumber()",
        "secondPseudoFile.vadl", "model magicNumber(): Ex = {\n"
            + "  42\n"
            + "}\n"
    );

    @Override
    public boolean exists(Path path) {
      return files.containsKey(path.toString());
    }

    @Override
    public InputStream getInputStream(Path path) throws IOException {
      return new ByteArrayInputStream(files.get(path.toString()).toString().getBytes(
          StandardCharsets.UTF_8));
    }

    @Override
    public Path toAbsolutePath(Path path) {
      return path;
    }

    @Override
    public Path toRelativePath(Path path) {
      return path;
    }
  }

}
