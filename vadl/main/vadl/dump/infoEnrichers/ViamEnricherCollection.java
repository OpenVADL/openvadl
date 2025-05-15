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

package vadl.dump.infoEnrichers;

import static java.util.Collections.reverse;
import static vadl.dump.InfoEnricher.forType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import vadl.dump.BehaviorTimelineDisplay;
import vadl.dump.CollectBehaviorDotGraphPass;
import vadl.dump.Info;
import vadl.dump.InfoEnricher;
import vadl.dump.InfoUtils;
import vadl.dump.entities.DefinitionEntity;
import vadl.utils.Pair;
import vadl.utils.SourceLocation;
import vadl.viam.DefProp;
import vadl.viam.Encoding;
import vadl.viam.Format;
import vadl.viam.Function;
import vadl.viam.Instruction;
import vadl.viam.Parameter;
import vadl.viam.Stage;
import vadl.viam.ViamError;
import vadl.viam.graph.Graph;
import vadl.viam.passes.InstructionResourceAccessAnalysisPass;

/**
 * A static collection of {@link InfoEnricher} that provides information about
 * VIAM definitions.
 * E.g. the definition type ({@link #DEF_CLASS_SUPPLIER_TAG} and the behavior graph
 * ({@link #BEHAVIOR_SUPPLIER_MODAL}).
 * They are collected in the {@link #all} list and added in the {@link vadl.dump.HtmlDumpPass}.
 */
public class ViamEnricherCollection {

  /**
   * A {@link InfoEnricher} that adds a {@link vadl.dump.Info.Tag} containing the type of
   * the definition.
   * This is only done if the given entity is a
   * {@link DefinitionEntity} and if the origin definition
   * has the prop {@link DefProp.WithType}.
   */
  public static InfoEnricher TYPE_SUPPLIER_TAG = (entity, passResult) -> {
    if (entity instanceof DefinitionEntity defEntity
        && defEntity.origin() instanceof DefProp.WithType typed) {
      entity.addInfo(Info.Tag.of("Type", typed.type().toString()));
    }
  };

  /**
   * The DEF_CLASS_SUPPLIER_TAG variable is an instance of the InfoEnricher interface
   * which attaches a {@link Info.Tag} to a given {@link vadl.dump.DumpEntity} based on
   * the information provided by the entity and the result from already executed passes.
   *
   * <p>It is used to enrich {@link DefinitionEntity} objects of type
   * with a tag named "DefType", that represents the simple name of the class of
   * the entity's origin (definition).
   *
   * @see InfoEnricher
   * @see DefinitionEntity
   */
  public static InfoEnricher DEF_CLASS_SUPPLIER_TAG =
      forType(DefinitionEntity.class, (defEntity, passResult) -> {
        var info = Info.Tag.of("DefType", defEntity.origin().getClass().getSimpleName());
        defEntity.addInfo(info);
      });

  /**
   * The PARENT_SUPPLIER_TAG variable is an implementation of the {@link InfoEnricher} interface.
   * It attaches an {@link Info.Tag} object with the "Parent" tag to a given
   * {@link DefinitionEntity} based on the information
   * provided by the entity.
   *
   * @see InfoEnricher
   * @see DefinitionEntity
   */
  public static InfoEnricher PARENT_SUPPLIER_TAG =
      forType(DefinitionEntity.class, (defEntity, passResult) -> {
        var info =
            Info.Tag.of("Parent", defEntity.parent().name(), "#" + defEntity.parent().cssId());
        defEntity.addInfo(info);
      });

  /**
   * Formats the supplier tag for a given {@link DefinitionEntity}
   * if the origin is an instance of {@link Instruction}.
   *
   * <p>The tag created includes the identifier of the instruction and its CSS ID.
   * This information is added to the {@link DefinitionEntity} as an {@link Info.Tag}.
   */
  public static InfoEnricher FORMAT_SUPPLIER_TAG =
      forType(DefinitionEntity.class, (defEntity, passResult) -> {
        if (defEntity.origin() instanceof Instruction instruction) {
          var info =
              Info.Tag.of("Format", instruction.format().identifier.toString(),
                  "#" + DefinitionEntity.cssIdFor(instruction.format()));
          defEntity.addInfo(info);
        }
      });

  /**
   * A {@link InfoEnricher} that adds a {@link vadl.dump.Info.Tag} containing the fixed encoding of
   * an instruction.
   * This is only done if the given entity is a {@link DefinitionEntity} and if the origin
   * definition is an instruction encoding field.
   */
  public static InfoEnricher ENCODING_SUPPLIER_TAG = InfoEnricher.forType(DefinitionEntity.class,
      (definition, passResult) -> {
        if (!(definition.origin() instanceof Encoding.Field field)) {
          return;
        }
        definition.addInfo(Info.Tag.of("Encoding", field.constant().binary("")));
      });


  /**
   * BEHAVIOR_SUPPLIER_MODAL is an implementation of the {@link InfoEnricher} interface.
   * It enriches the given {@link vadl.dump.DumpEntity} with information about the
   * behavior of a {@link vadl.viam.Definition} that has a behavior defined.
   *
   * <p>If the entity is an instance of {@link DefinitionEntity} and it
   * has a behavior defined, BEHAVIOR_SUPPLIER_MODAL retrieves the behavior information,
   * creates an {@link Info.Modal} object, and adds it to the entity.
   *
   * <p>While the info has the graph in DOT syntax added in a script tag,
   * the actual rendering happens when the modal is opened the first time.
   * The d3-graphviz js library is used for this part.</p>
   *
   * @see InfoEnricher
   * @see Info
   */
  public static InfoEnricher BEHAVIOR_SUPPLIER_MODAL = (entity, passResult) -> {
    if (entity instanceof DefinitionEntity defEntity
        && defEntity.origin() instanceof DefProp.WithBehavior withBehavior) {
      var def = defEntity.origin();

      var behaviorGraphs = passResult.allResultsOf(
              CollectBehaviorDotGraphPass.class,
              CollectBehaviorDotGraphPass.Result.class
          ).map(r -> Pair.of(r.prevPass(), r.behaviors().getOrDefault(def, List.of())))
          .filter(r -> !r.right().isEmpty())
          .map(r -> Pair.of(r.left(), r.right().get(0)))
          .toList();

      // filter only passes that altered graph
      var filteredBehaviorGraphs =
          new ArrayList<Pair<BehaviorTimelineDisplay, String>>();
      behaviorGraphs.forEach(entry -> {
        if (filteredBehaviorGraphs.isEmpty()
            || !filteredBehaviorGraphs.get(filteredBehaviorGraphs.size() - 1).right()
            .equals(entry.right())) {
          filteredBehaviorGraphs.add(Pair.of(entry.left(), entry.right()));
        }
      });

      // reverse the result so the first one is the latest one
      reverse(filteredBehaviorGraphs);

      if (filteredBehaviorGraphs.isEmpty()) {
        return;
      }

      // fetch behavior
      var info = InfoUtils.createGraphModalWithTimeline(
          "Behavior",
          def.simpleName() + " Behavior",
          filteredBehaviorGraphs
      );
      defEntity.addInfo(info);
    }
  };


  /**
   * A {@link InfoEnricher} that adds an expandable to definition entities if
   * the verification failed.
   * This helps debugging and finding bugs in the VIAM.
   */
  public static InfoEnricher VERIFY_SUPPLIER_EXPANDABLE =
      forType(DefinitionEntity.class, (entity, passResult) -> {
        try {
          // run verification
          entity.origin().verify();
        } catch (ViamError e) {
          // catch and add error information
          var info = InfoUtils.createCodeBlockExpandable(
              "<span class=\"text-red-500\">Validation Exception</span>",
              e.getMessage()
          );
          entity.addInfo(info);
        }
      });

  /**
   * The header that is included in every pretty print source. It explains why the code looks
   * different, because all macros are expanded and from where this code originated.
   *
   * @param location of the definition.
   * @return the header text.
   */
  private static String sourceHeader(SourceLocation location) {
    var header = new StringBuilder(
        "// The code below has all macros expanded.\n// SourceLocation: %s \n".formatted(
            location.toConciseString()
        ));

    for (var current = location; current.expandedFrom() != null; current = current.expandedFrom()) {
      header.append("//     expanded from macro call at: %s \n".formatted(
          current.expandedFrom().toConciseString()));
    }

    header.append("\n");
    return header.toString();
  }

  /**
   * A {@link InfoEnricher} that adds an expandable to definition entities if
   * the verification failed.
   * This helps debugging and finding bugs in the VIAM.
   */
  public static InfoEnricher SOURCE_CODE_SUPPLIER_EXPANDABLE =
      forType(DefinitionEntity.class, (entity, passResult) -> {
        var noSourceCode = List.of(
            Format.Field.class,
            Encoding.Field.class,
            Parameter.class
        );
        if (noSourceCode.contains(entity.origin().getClass())) {
          // dont add source code
          return;
        }

        if (entity.origin() instanceof Function && entity.parentLevel() > 2) {
          return;
        }
        var sourceLocation = entity.origin().location();
        if (entity.origin() instanceof Instruction) {
          sourceLocation = ((Instruction) entity.origin()).behavior().sourceLocation();
        }
        var source = sourceHeader(sourceLocation) + entity.origin().prettyPrintSource();
        var info = InfoUtils.createCodeBlockExpandable(
            "Source Code",
            source
        );
        entity.addInfo(info);
      });

  public static InfoEnricher RESOURCE_ACCESS_SUPPLIER_EXPANDABLE =
      forType(DefinitionEntity.class, (entity, passResult) -> {
        if (!passResult.hasRunPassOnce(InstructionResourceAccessAnalysisPass.class)
            || !(entity.origin() instanceof Instruction)) {
          return;
        }
        var instr = (Instruction) entity.origin();
        var reads = Objects.requireNonNull(instr.readResources())
            .stream().sorted(Comparator.comparing(e -> e.getClass().getSimpleName()))
            .map(rsrc -> rsrc.getClass().getSimpleName() + " " + rsrc.simpleName())
            .collect(Collectors.toCollection(ArrayList::new));
        var writes = Objects.requireNonNull(instr.writtenResources())
            .stream().sorted(Comparator.comparing(e -> e.getClass().getSimpleName()))
            .map(rsrc -> rsrc.getClass().getSimpleName() + " " + rsrc.simpleName())
            .collect(Collectors.toCollection(ArrayList::new));

        if (reads.isEmpty() && writes.isEmpty()) {
          return;
        }

        reads.add(0, "Read");
        writes.add(0, "Written");

        var info = InfoUtils.createTableExpandable(
            "Accessed Resources",
            List.of(reads, writes)
        );
        entity.addInfo(info);
      });


  public static InfoEnricher BEHAVIOR_NO_LOCATION_EXPANDABLE =
      forType(DefinitionEntity.class, (entity, passResult) -> {
        if (!(entity.origin() instanceof DefProp.WithBehavior withBehavior)) {
          return;
        }

        var nodesWithoutSourceLocation = withBehavior.behaviors()
            .stream().flatMap(Graph::getNodes)
            .filter(n -> n.location().equals(SourceLocation.INVALID_SOURCE_LOCATION))
            .toList();

        if (nodesWithoutSourceLocation.isEmpty()) {
          return;
        }

        var info = InfoUtils.createTableExpandable(
            "<span class=\"text-red-500 font-bold\">Nodes without Source Location</span>",
            List.of(nodesWithoutSourceLocation)
        );
        entity.addInfo(info);
      });

  /**
   * A {@link InfoEnricher} that adds a {@link vadl.dump.Info.Tag} containing the next/prev stage
   * of the micro architecture.
   * This is only done if the given entity is a {@link Stage}.
   */
  public static InfoEnricher STAGE_ORDER_SUPPLIER =
      forType(DefinitionEntity.class, (definitionEntity, passResult) -> {
        if (definitionEntity.origin() instanceof Stage stage) {
          var prev = stage.prev();
          if (prev != null) {
            definitionEntity.addInfo(Info.Tag.of("Prev", prev.simpleName()));
          }
          var list = stage.next();
          if (list != null) {
            for (Stage next : list) {
              definitionEntity.addInfo(Info.Tag.of("Next", next.simpleName()));
            }
          }
        }
      });

  /**
   * A list of all info enrichers for the default VIAM specification.
   */
  public static List<InfoEnricher> all = List.of(
      DEF_CLASS_SUPPLIER_TAG,
      TYPE_SUPPLIER_TAG,
      PARENT_SUPPLIER_TAG,
      FORMAT_SUPPLIER_TAG,
      ENCODING_SUPPLIER_TAG,
      BEHAVIOR_SUPPLIER_MODAL,
      VERIFY_SUPPLIER_EXPANDABLE,
      SOURCE_CODE_SUPPLIER_EXPANDABLE,
      RESOURCE_ACCESS_SUPPLIER_EXPANDABLE,
      BEHAVIOR_NO_LOCATION_EXPANDABLE,
      STAGE_ORDER_SUPPLIER
  );

}
