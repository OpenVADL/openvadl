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

package vadl.viam;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The Specification is the root of the VIAM, as it contains all other definitions of a
 * specification.
 * Note that the hierarchy of definitions is the same as in the textual VADL specification.
 * E.g., a {@link Function} could be in the global scope, which means it is directly held by the
 * Specification, or it could be defined in a
 * {@link InstructionSetArchitecture} definition, which means that
 * it is hold by the respective ISA definition instance and not by the Specification itself.
 */
public class Specification extends Definition {

  private final List<Definition> definitions = new ArrayList<>();


  public Specification(Identifier identifier) {
    super(identifier);
  }

  public Stream<Definition> definitions() {
    return definitions.stream();
  }

  /**
   * Returns the instruction set architecture of the specification.
   */
  public Optional<InstructionSetArchitecture> isa() {
    // TODO: remove this method completely
    var isaDef = definitions()
        .filter(InstructionSetArchitecture.class::isInstance)
        .map(InstructionSetArchitecture.class::cast)
        .findFirst();
    if (isaDef.isEmpty()) {
      return mip().map(MicroProcessor::isa);
    }
    return isaDef;
  }

  /**
   * Returns the ABI of the specification.
   */
  public Optional<Abi> abi() {
    return definitions()
        .filter(Abi.class::isInstance)
        .map(Abi.class::cast)
        .findFirst();
  }

  /**
   * Returns the assembly description of the specification.
   */
  public Optional<AssemblyDescription> assemblyDescription() {
    return definitions()
        .filter(AssemblyDescription.class::isInstance)
        .map(AssemblyDescription.class::cast)
        .findFirst();
  }

  /**
   * Returns the instruction set architecture of the specification.
   */
  public Optional<MicroProcessor> mip() {
    return definitions()
        .filter(MicroProcessor.class::isInstance)
        .map(MicroProcessor.class::cast)
        .findFirst();
  }

  /**
   * Returns the micro architecture of the specification.
   */
  public Optional<MicroArchitecture> mia() {
    return definitions()
        .filter(MicroArchitecture.class::isInstance)
        .map(MicroArchitecture.class::cast)
        .findFirst();
  }

  /**
   * Returns all global format definitions as stream.
   */
  public Stream<Format> formats() {
    return definitions.stream()
        .filter(Format.class::isInstance)
        .map(Format.class::cast);
  }

  /**
   * Returns all register tensors in the ISA as a stream.
   */
  public Stream<RegisterTensor> registerTensors() {
    return isa()
        .map(x -> x.registerTensors().stream())
        .orElseGet(Stream::empty);
  }

  /**
   * Returns all register files as stream.
   * So all register tensors that have one index dimension (two dimensions in total).
   *
   * @deprecated Use the {@link #registerTensors()} method instead.
   */
  @Deprecated
  public Stream<RegisterTensor> registerFiles() {
    return registerTensors().filter(RegisterTensor::isRegisterFile);
  }

  /**
   * Returns a stream of all format definitions within the specification, including the ones
   * nested within instruction set architectures.
   *
   * @return A stream of {@link Format} objects representing the format definitions.
   */
  public Stream<Format> findAllFormats() {
    var innerFormats = isa().map(
        i -> i.ownFormats().stream()
    ).orElse(Stream.empty());

    return Stream.concat(formats(), innerFormats);
  }

  public void add(Definition definition) {
    definitions.add(definition);
  }

  public void addAll(Collection<Definition> definition) {
    definitions.addAll(definition);
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }
}
