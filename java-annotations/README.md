## The `java-annotations` Module

This modules provides the `@Input, @Successor` and `@DataValue` annotations.
Those annotations are used in the VIAM-Graph to mark certain edges and properties of nodes.

As it is easy to miss something when writing a new node implementation, the `java-annotations` module
also provides `errorprone` check implementations that statically verify the correct implementation
of some properties that are easily overseen and hard to debug.

The implemented checkers are `ApplyOnInptusChecker, ApplyOnSuccessorChecker, CollectDataChecker, CollectInputsChecker`
and `CollectSuccessorChecker`.