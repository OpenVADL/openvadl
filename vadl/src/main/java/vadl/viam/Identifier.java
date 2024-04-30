package vadl.viam;

import vadl.utils.SourceLocation;

/**
 * Source level identifier class.
 */
public record Identifier(
    String name,
    SourceLocation sourceLocation
) {

}
