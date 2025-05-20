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

package vadl.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;

/**
 * A utility class providing methods for computing the Levenshtein distance (edit distance).
 */
public class Leivenshtein {


  /**
   * Ranks the strings in a given dictionary based on their Levenshtein distance
   * (edit distance) to a target string.
   *
   * <p>Implementation is based on the
   * <a href="https://en.wikipedia.org/wiki/Wagner%E2%80%93Fischer_algorithm">Wagner-Fisher
   * Algorithm</a>, without the full matrix to reduce space to O(m).
   *
   * @param target       the target string to which the comparison is made
   * @param dictionary   the list of strings to be ranked based on similarity to the target
   * @param maxSolutions an optional parameter to limit the results
   * @param maxChange    an optional parameter defining the maximum allowable change as a rate
   *                     relative to the target string. 0.5 means half the string can be different.
   * @return a sorted list of ranked pairs consisting of each string from the dictionary and its
   *     corresponding edit distance to the target
   */
  public static List<Pair<String, Integer>> rank(String target, Collection<String> dictionary,
                                                 @Nullable Integer maxSolutions,
                                                 @Nullable Double maxChange) {

    @Nullable Long upperBound =
        maxChange != null ? Math.round(target.length() * maxChange) : null;

    List<Pair<String, Integer>> results = new ArrayList<>(dictionary.size());

    var lastRow = new int[target.length() + 1];
    var currentRow = new int[target.length() + 1];

    for (String word : dictionary) {

      // Init the last row (since the current row will be moved into the last row first thing in
      // the loop below we actually have to write to the current row).
      for (int i = 0; i <= target.length(); i++) {
        currentRow[i] = i;
      }

      for (int j = 1; j <= word.length(); j++) {
        // Move the current row (from the last iteration) to the lastRow.
        // Note: We swap them here to avoid allocating a new array. Since the current content from
        // lastrow is just not needed anymore.
        var tmp = lastRow;
        lastRow = currentRow;
        currentRow = tmp;

        // Init the current row
        currentRow[0] = j;

        for (int i = 1; i <= target.length(); i++) {
          var substituteCost = word.charAt(j - 1) == target.charAt(i - 1) ? 0 : 1;
          currentRow[i] = Math.min(
              Math.min(
                  lastRow[i] + 1,
                  currentRow[i - 1] + 1
              ),
              lastRow[i - 1] + substituteCost
          );
        }

        // FIXME: Insert optimization here respecting upper bound (if all numbers in currentRow are
        // larger than it we can abort this word).
      }

      // FIXME: Add optimization here adjusting upper bound if maxLimit is already reached we can
      // just take as the upperbound the cost of the worst word in our selection we already found.

      var cost = currentRow[target.length()];
      results.add(new Pair<>(word, cost));
    }

    results.sort(Comparator.comparingInt(Pair::right));
    var resultStream = results.stream();
    if (upperBound != null) {
      resultStream = resultStream
          .filter(pair -> pair.right() <= upperBound);
    }
    if (maxSolutions != null) {
      resultStream = resultStream.limit(maxSolutions);
    }
    return resultStream.toList();
  }

  /**
   * Computes the <a href ="https://en.wikipedia.org/wiki/Levenshtein_distance">Levenshtein
   * distance</a> (edit distance) between two strings.
   *
   * @param first  the first string to be compared
   * @param second the second string to be compared
   * @return the Levenshtein distance between the two strings
   */
  public static int distance(String first, String second) {
    return rank(first, List.of(second), null, null).getFirst().right();
  }

  /**
   * Sorts a list of strings based on their similarity to a target string using
   * the Levenshtein distance (edit distance).
   *
   * @param target     the target string to which the dictionary strings are compared
   * @param dictionary the list of strings to be sorted
   * @return a sorted list of strings
   */
  public static List<String> sortAll(String target, Collection<String> dictionary) {
    return rank(target, dictionary, null, null).stream().map(Pair::left).toList();
  }

  /**
   * An opinionated fuzzy string search used for suggestions based on the Levenshtein distance.
   * Used as the default in the open-vadl compiler to generate suggestions.
   *
   * <p>If a variable cannot be found, this method can be used to find similarly named variables
   * and suggest them in the error message.
   *
   * <p>Be aware that potentially none of the provided options are close enough and an empty list
   * might be returned.
   *
   * @param target     against which the available options should be compared against.
   * @param dictionary of all available options.
   * @return a sorted list to be provided as suggestions.
   */
  public static List<String> suggestions(String target, Collection<String> dictionary) {
    return rank(target, dictionary, 6, 0.5).stream().map(Pair::left).toList();
  }
}
