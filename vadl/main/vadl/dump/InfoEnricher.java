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

package vadl.dump;

import java.util.function.BiConsumer;
import vadl.dump.infoEnrichers.ViamEnricherCollection;
import vadl.pass.PassResults;

/**
 * The InfoEnricher attaches {@link Info} objects to a given {@link DumpEntity} based on
 * the information provided by the entity and the result from already executed passes.
 * New InforEnrichers must be registered in the {@link HtmlDumpPass}.
 *
 * <p>Implementations of the InfoEnricher can be found at
 * {@link ViamEnricherCollection}</p>
 *
 * @see HtmlDumpPass
 */
public interface InfoEnricher {

  void enrich(DumpEntity entity, PassResults passResults);

  /**
   * A helper function that wraps the enricher construction in a check for the {@link DumpEntity}
   * class type.
   * It returns an {@link InfoEnricher} that does early return if the entity type is not of
   * the given type class. Otherwise it calls the given enricher function.
   *
   * @param type     the type to check the entity object against.
   * @param enricher the custom enricher function.
   * @return a new {@link InfoEnricher}
   */
  static <T extends DumpEntity> InfoEnricher forType(Class<T> type,
                                                     BiConsumer<T, PassResults> enricher) {
    return (e, pr) -> {
      if (type.isInstance(e)) {
        //noinspection unchecked
        enricher.accept((T) e, pr);
      }
    };
  }
}
