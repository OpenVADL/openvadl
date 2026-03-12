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

package vadl.lsp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for the DependencyMap used in the language server.
 */
public class DependencyMapTest {
  private static final String ITEM0 = "item0";
  private static final String ITEM1 = "item1";
  private static final String ITEM2 = "abc";
  private static final String ITEM3 = "xyz";
  private static final String[] ALL_ITEMS = {
      ITEM0, ITEM1, ITEM2, ITEM3
  };

  private DependencyMap<String> map;

  @BeforeEach
  void setup() {
    map = new DependencyMap<>();
  }

  @Test
  void emptyMap() {
    assertAllItemsHaveNoDependents();
  }

  @Test
  void singleDependency() {
    assertAllItemsHaveNoDependents();

    map.setDependencies(ITEM0, Set.of(ITEM1));

    assertHasNoDependents(ITEM0);
    assertDependents(ITEM1, Set.of(ITEM0));
    assertHasNoDependents(ITEM2);
    assertHasNoDependents(ITEM3);

    // Clear map
    map.setDependencies(ITEM0, Set.of());

    assertAllItemsHaveNoDependents();
  }

  @Test
  void noSelfDependency() {
    assertAllItemsHaveNoDependents();

    map.setDependencies(ITEM3, Set.of(ITEM3));

    assertAllItemsHaveNoDependents();
  }

  @Test
  void severalDependencies() {
    assertAllItemsHaveNoDependents();

    map.setDependencies(ITEM1, Set.of(ITEM0, ITEM2));

    assertDependents(ITEM0, Set.of(ITEM1));
    assertHasNoDependents(ITEM1);
    assertDependents(ITEM2, Set.of(ITEM1));
    assertHasNoDependents(ITEM3);

    map.setDependencies(ITEM1, Set.of(ITEM0));

    assertDependents(ITEM0, Set.of(ITEM1));
    assertHasNoDependents(ITEM1);
    assertHasNoDependents(ITEM2);
    assertHasNoDependents(ITEM3);

    map.setDependencies(ITEM1, Set.of(ITEM2, ITEM3));

    assertHasNoDependents(ITEM0);
    assertHasNoDependents(ITEM1);
    assertDependents(ITEM2, Set.of(ITEM1));
    assertDependents(ITEM3, Set.of(ITEM1));

    // Clear map
    map.setDependencies(ITEM1, Set.of());

    assertAllItemsHaveNoDependents();
  }

  @Test
  void severalDependents() {
    assertAllItemsHaveNoDependents();

    map.setDependencies(ITEM0, Set.of(ITEM1, ITEM2));

    assertHasNoDependents(ITEM0);
    assertDependents(ITEM1, Set.of(ITEM0));
    assertDependents(ITEM2, Set.of(ITEM0));
    assertHasNoDependents(ITEM3);

    map.setDependencies(ITEM1, Set.of(ITEM2));

    assertHasNoDependents(ITEM0);
    assertDependents(ITEM1, Set.of(ITEM0));
    assertDependents(ITEM2, Set.of(ITEM0, ITEM1));
    assertHasNoDependents(ITEM3);

    map.setDependencies(ITEM3, Set.of(ITEM1));

    assertHasNoDependents(ITEM0);
    assertDependents(ITEM1, Set.of(ITEM0, ITEM3));
    assertDependents(ITEM2, Set.of(ITEM0, ITEM1));
    assertHasNoDependents(ITEM3);

    map.setDependencies(ITEM3, Set.of(ITEM2, ITEM0));

    assertDependents(ITEM0, Set.of(ITEM3));
    assertDependents(ITEM1, Set.of(ITEM0));
    assertDependents(ITEM2, Set.of(ITEM0, ITEM1, ITEM3));
    assertHasNoDependents(ITEM3);

    // Clear map
    map.setDependencies(ITEM0, Set.of());
    map.setDependencies(ITEM1, Set.of());
    map.setDependencies(ITEM3, Set.of());

    assertAllItemsHaveNoDependents();
  }

  @Test
  void cyclicDependencies() {
    assertAllItemsHaveNoDependents();

    map.setDependencies(ITEM0, Set.of(ITEM1));

    assertHasNoDependents(ITEM0);
    assertDependents(ITEM1, Set.of(ITEM0));
    assertHasNoDependents(ITEM2);
    assertHasNoDependents(ITEM3);

    map.setDependencies(ITEM1, Set.of(ITEM0));
    // Cycle item0 <-> item1

    assertDependents(ITEM0, Set.of(ITEM1));
    assertDependents(ITEM1, Set.of(ITEM0));
    assertHasNoDependents(ITEM2);
    assertHasNoDependents(ITEM3);

    map.setDependencies(ITEM1, Set.of(ITEM2));

    assertHasNoDependents(ITEM0);
    assertDependents(ITEM1, Set.of(ITEM0));
    assertDependents(ITEM2, Set.of(ITEM1));
    assertHasNoDependents(ITEM3);

    map.setDependencies(ITEM2, Set.of(ITEM0));
    // Cycle item0 -> item1 -> item2 -> item0

    assertDependents(ITEM0, Set.of(ITEM2));
    assertDependents(ITEM1, Set.of(ITEM0));
    assertDependents(ITEM2, Set.of(ITEM1));
    assertHasNoDependents(ITEM3);

    // Clear map
    map.setDependencies(ITEM0, Set.of());
    map.setDependencies(ITEM1, Set.of());
    map.setDependencies(ITEM2, Set.of());

    assertAllItemsHaveNoDependents();
  }

  private void assertAllItemsHaveNoDependents() {
    for (String item : ALL_ITEMS) {
      assertHasNoDependents(item);
    }
  }

  private void assertHasNoDependents(String item) {
    assertThat(map.getDependents(item)).isEmpty();
  }

  private void assertDependents(String item, Set<String> expectedDependents) {
    assertThat(map.getDependents(item)).isEqualTo(expectedDependents);
  }
}
