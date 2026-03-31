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

package vadl.dump;

import java.util.List;
import vadl.dump.infoEnrichers.LcbEnricherCollection;

/**
 * Registers LCB/GCB-specific HTML dump extensions.
 */
public class LcbHtmlDumpExtensionProvider implements HtmlDumpExtensionProvider {

  @Override
  public int priority() {
    return 200;
  }

  @Override
  public void addInfoEnrichers(List<InfoEnricher> enrichers) {
    enrichers.addAll(LcbEnricherCollection.all);
  }
}
