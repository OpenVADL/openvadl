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

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LevenshteinTest {

  /// Examples taken from this
  /// [Wikipedia Article](https://en.wikipedia.org/wiki/Levenshtein_distance).
  @Test
  void testWikipediaExamples() {
    // https://en.wikipedia.org/wiki/Levenshtein_distance#Example
    Assertions.assertEquals(3, Levenshtein.distance("kitten", "sitting"));
    Assertions.assertEquals(3, Levenshtein.distance("sitting", "kitten"));

    // https://en.wikipedia.org/wiki/Levenshtein_distance#Iterative_with_full_matrix
    Assertions.assertEquals(3, Levenshtein.distance("Saturday", "Sunday"));
  }

  /// Examples from this
  /// [blog post](https://stevehanov.ca/blog/fast-and-easy-levenshtein-distance-using-a-trie)
  @Test
  void testSteveHanovsBlog() {
    Assertions.assertEquals(0, Levenshtein.distance("goober", "goober"));
    Assertions.assertEquals(1, Levenshtein.distance("goober", "goobers"));
    Assertions.assertEquals(1, Levenshtein.distance("goober", "gooier"));

    Assertions.assertEquals(2, Levenshtein.distance("kate", "cat"));
    Assertions.assertEquals(2, Levenshtein.distance("kate", "cats"));
  }

  @Test
  void testSuggestEmptyListIfAllCandidatesBad() {
    // In the past there once was a bug
    Assertions.assertEquals(Collections.EMPTY_LIST, Levenshtein.suggestions("XSize", List.of("hex")));
  }
}
