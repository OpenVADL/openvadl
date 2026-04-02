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

import java.io.InputStream;
import java.nio.file.Path;

/**
 * A overlay filesystem that allows you to overlay one VFS with another.
 *
 * <p>For example you might want to lie to the compiler with one custom version of a file, in that
 * case you can use this overlay to combine a @see vadl.utils.SingleFileVirtualFileSystem combined
 * with the original filesystem.
 */
public class OverlayVirtualFileSystem implements VirtualFileSystem {
  VirtualFileSystem top;
  VirtualFileSystem bottom;

  /**
   * The top file system will be used first if the file exists in it and otherwise the task will be
   * delegated to the bottom one.
   *
   * @param top filsystem will be checked first.
   * @param bottom filesystem will be checked second.
   */
  public OverlayVirtualFileSystem(VirtualFileSystem top, VirtualFileSystem bottom) {
    this.top = top;
    this.bottom = bottom;
  }

  private VirtualFileSystem selectVFS(Path path) {
    if (top.exists(path)) {
      return top;
    }
    return bottom;
  }

  @Override
  public boolean exists(Path path) {
    return top.exists(path) || bottom.exists(path);
  }

  @Override
  public InputStream getInputStream(Path path) {
    return selectVFS(path).getInputStream(path);
  }

  @Override
  public Path toAbsolutePath(Path path) {
    return selectVFS(path).toAbsolutePath(path);
  }

  @Override
  public Path toRelativePath(Path path) {
    return selectVFS(path).toRelativePath(path);
  }
}
