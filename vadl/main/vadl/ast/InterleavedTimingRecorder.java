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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.SequencedMap;
import java.util.function.Supplier;

/**
 * A timing recorder of AST passes that can handling the tightly interleaved nature of the passes.
 * It's quite common that parsing can be interrupted by symbol resolving or macro expansion.
 * Or that typechecking is interrupted by constatnt folding.
 * This recorder can will suspend the current timing recording of the current pass and continue
 * with the next before returning to the current pass.
 */
public class InterleavedTimingRecorder {

  /**
   * A pass timing of a single pass.
   */
  public static class PassTiming {
    final private String description;
    private long durationNS;

    public String description() {
      return description;
    }

    public long durationNS() {
      return durationNS;
    }

    public PassTiming(String description, long durationNS) {
      this.description = description;
      this.durationNS = durationNS;
    }
  }

  public SequencedMap<String, PassTiming> passTimings = new LinkedHashMap<>();

  private Deque<String> activeStack = new ArrayDeque<>();

  private long activeStartTimeNS;

  private void addTiming(PassTiming passTiming) {
    addTiming(passTiming.description, passTiming.durationNS);
  }

  private void addTiming(String description, long durationNS) {
    passTimings.compute(description,
        (key, existingTiming) -> {
          if (existingTiming == null) return new PassTiming(description, durationNS);
          existingTiming.durationNS += durationNS;
          return existingTiming;
        }
    );
  }

  private void startRecording(String name) {
    // Add dummy timing to keep the order
    addTiming(name, 0);

    activeStack.push(name);
    continueRecording();
  }

  /**
   * Continue recording the current pass timing.
   * This will resume the timing recording of the current pass after a pause.
   */
  public void continueRecording() {
    activeStartTimeNS = System.nanoTime();
  }

  /**
   * Pause recording the current pass timing.
   * This will stop the timing recording of the current pass and continue with the next pass.
   */
  public void pauseRecording() {
    var name = activeStack.getFirst();
    var consumedTimeNS = System.nanoTime() - activeStartTimeNS;
    activeStartTimeNS = -1;
    addTiming(name, consumedTimeNS);
  }

  private void endRecording() {
    pauseRecording();
    activeStack.pop();
  }

  /**
   * Execute a pass with timing recording.
   * This method records the timing of a pass and handles the timing lifecycle.
   */
  public <T> T withPassTiming(String name, Supplier<T> pass) {

    if (!activeStack.isEmpty()) {
      if  (activeStack.peek().equals(name)) {
        // Do nothing if we are already in the correct pass
        return pass.get();
      }

      pauseRecording();
    }
    startRecording(name);
    var result = pass.get();
    endRecording();
    if (!activeStack.isEmpty()) {
      continueRecording();
    }
    return result;
  }

  /**
   * Execute a pass with timing recording.
   * This method records the timing of a pass and handles the timing lifecycle.
   */
  public void withPassTiming(String name, Runnable pass) {
    if (!activeStack.isEmpty()) {
      if  (activeStack.peek().equals(name)) {
        // Do nothing if we are already in the correct pass
        pass.run();
      }

      pauseRecording();
    }
    startRecording(name);
    pass.run();
    endRecording();
    if (!activeStack.isEmpty()) {
      continueRecording();
    }
  }

  public void importAllTimings(InterleavedTimingRecorder other) {
    other.passTimings.values().forEach(this::addTiming);
  }

}
