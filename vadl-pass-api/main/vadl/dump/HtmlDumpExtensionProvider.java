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

/**
 * Registers HTML dump extensions from optional modules.
 *
 * <p>The shared HTML dump lives in {@code :vadl-pass-api}, but some entity suppliers and
 * info enrichers are owned by backend modules such as {@code :vadl-vdt}, {@code :vadl-rtl},
 * or {@code :vadl-iss}. To avoid a compile-time dependency from {@code :vadl-pass-api} back
 * to those modules, the registration is done via {@link java.util.ServiceLoader}.</p>
 *
 * <p>To contribute extensions from another module:</p>
 * <ol>
 *   <li>Implement this interface in that module.</li>
 *   <li>Register the implementation in
 *       {@code META-INF/services/vadl.dump.HtmlDumpExtensionProvider}.</li>
 *   <li>List the provider's fully qualified class name in that service file.</li>
 * </ol>
 *
 * <p>At runtime, {@link HtmlDumpPass} loads all registered providers from the classpath and
 * lets them add extra {@link DumpEntitySupplier}s and {@link InfoEnricher}s. Without the
 * {@code META-INF/services} entry, a provider class exists only as dead code and is not
 * discovered.</p>
 */
public interface HtmlDumpExtensionProvider {

  default int priority() {
    return 0;
  }

  default void addEntitySuppliers(List<DumpEntitySupplier<?>> suppliers) {
  }

  default void addInfoEnrichers(List<InfoEnricher> enrichers) {
  }
}
