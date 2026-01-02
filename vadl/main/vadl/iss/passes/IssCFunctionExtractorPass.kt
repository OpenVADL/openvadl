package vadl.iss.passes

import vadl.configuration.IssConfiguration
import vadl.iss.passes.extensions.InstrInfo
import vadl.pass.PassName
import vadl.pass.PassResults
import vadl.utils.GraphUtils
import vadl.viam.*
import vadl.viam.Function
import vadl.viam.graph.Graph
import vadl.viam.graph.Node
import vadl.viam.graph.NodeList
import vadl.viam.graph.control.ReturnNode
import vadl.viam.graph.control.StartNode
import vadl.viam.graph.dependency.*
import vadl.viam.passes.GraphProcessor
import java.io.IOException


/**
 * An ISS pass that extracts expressions into functions and replaces them by calls.
 * E.g., this is done for [TensorNode], as the generated function becomes cleaner.
 */
class IssCFunctionExtractionPass(config: IssConfiguration) : AbstractIssPass(config) {
    override fun getName(): PassName {
        return PassName.of("ISS C Function Extraction")
    }


    @Throws(IOException::class)
    override fun execute(passResults: PassResults, viam: Specification): Any? {
        helperInstrs(viam).forEach {
            // extract tensor nodes
            FunctionExtractor(it).run()
        }
        return null
    }
}

private fun Instruction.issInfo(): InstrInfo {
    return expectExtension<InstrInfo>(InstrInfo::class.java)
}

private class FunctionExtractor(
    val instruction: Instruction
) : GraphProcessor<Unit>() {
    val graph = instruction.behavior()
    val instrInfo = instruction.issInfo()

    fun run() {
        GraphUtils.getAllDependencyRoots(graph).forEach(::processNode)
    }

    override fun processUnprocessedNode(toProcess: Node) {
        // first extract the inner tensors
        toProcess.visitInputs(this)

        val tensor = toProcess as? TensorNode ?: return
        val creator = tensor.createFunction()
        tensor.replaceAndDelete(creator.call)
        instrInfo.addExtractedFunction(creator.definition)
    }

    fun TensorNode.createFunction(): FunctionCreator {
        return FunctionCreator(instruction, this)
    }
}

private class FunctionCreator(val instruction: Instruction, val tensor: TensorNode) {

    private val funcIdent: Identifier
    private val paramArgs: List<Pair<ExpressionNode, Parameter>>
    val definition: Function
    val call: Node

    init {
        val name = "tensor_${instruction.simpleName()}_${tensor.id}"
        funcIdent = instruction.identifier().append(name)
        paramArgs = findParamsArgs()
        definition = produceDefinition()
        call = produceCall()
    }

    private fun produceDefinition(): Function {
        val params = Array(paramArgs.size) { paramArgs[it].second }
        val function = Function(
            funcIdent,
            params,
            tensor.type(),
            createFuncGraph("Tensor Function ${funcIdent.simpleName()}")
        )
        return function
    }

    private fun produceCall(): FuncCallNode {
        return FuncCallNode(definition, NodeList(paramArgs.map { it.first }), tensor.type())
    }

    /**
     * Finds parameters and their corresponding argument expressions for the extracted tensor function.
     *
     * When extracting a tensor like `forall i in range tensor (a + b[i])` into a function,
     * we need to identify which values come from outside the tensor scope and should become parameters.
     *
     * This method:
     * 1. Finds all nodes in the tensor body that don't depend on the loop index (e.g., `a` but not `b[i]`)
     * 2. Creates a parameter for each independent node (named p0, p1, p2, ...)
     * 3. Returns pairs of (Parameter definition, Expression to pass as argument)
     */
    private fun findParamsArgs(): List<Pair<ExpressionNode, Parameter>> {
        // Find all expression nodes that don't depend on the tensor index variable
        // These are values from outside the tensor scope that need to be passed as parameters
        val externalExpressions = tensor.body().findAllIndependentOf(tensor.idx())
            // constant nodes shouldn't be passed as parameters
            .filter { it !is ConstantNode }

        // Create a parameter for each independent node, naming them p0, p1, p2, etc.
        return externalExpressions.mapIndexed { i, node ->
            val ident = funcIdent.append("p$i")
            Pair(node, Parameter(ident, node.type(), i))
        }
    }

    /**
     * Creates the behavior graph for the extracted tensor function.
     *
     * This builds a new graph containing a copy of the tensor expression tree where external
     * dependencies are replaced with parameter references. For example, if the original tensor
     * is `forall i in 0..3 tensor (a + i * 2)`, this creates a graph that returns the tensor
     * with `a` replaced by a FuncParamNode(p0).
     *
     * @param name Display name for the graph
     * @return A graph containing the tensor expression with parameterized external references
     */
    private fun createFuncGraph(name: String) = buildGraph(name) {
        // Create a map from original expression nodes to their corresponding parameters
        // This allows quick lookup when copying the expression tree
        val paramMap = paramArgs.toMap()

        /**
         * Recursively copies an expression subtree, replacing external references with parameter nodes.
         *
         * This performs a deep copy of the expression tree where:
         * - Nodes that are function parameters get replaced with FuncParamNode references
         * - All other nodes are shallow-copied with their inputs recursively copied
         */
        fun ExpressionNode.copySubExpression(): ExpressionNode = modifyRecursive { node ->
            paramMap[node]?.let { param ->
                // This node is an external value that was extracted as a parameter
                // Replace it with a reference to the function parameter
                FuncParamNode(param)
            } ?: run {
                // This node is internal to the tensor - make a shallow copy
                // and recursively copy its inputs
                node.shallowCopy().apply {
                    applyOnInputs { _, input ->
                        (input as? ExpressionNode)?.copySubExpression()
                    }
                } as ExpressionNode
            }
        }

        // Copy the tensor expression tree with parameters substituted
        val tensorCopy = tensor.copySubExpression()

        // Wrap in a return statement and add to the graph
        val end = addWithInputs(ReturnNode(tensorCopy))
        addWithInputs(StartNode(end))
    }
}

private fun buildGraph(name: String, builder: Graph.() -> Unit): Graph = Graph(name).apply(builder)

/**
 * Finds all expression nodes in this expression tree that do not depend on the specified [node].
 *
 * This method performs a recursive traversal of the expression tree through its inputs to identify
 * which nodes are independent of the target node.
 *
 * @param node The target expression node to check dependencies against
 * @return A set of all expression nodes that do not have a dependency path to [node]
 *
 * @see ExpressionNode
 *
 * ## Example
 * Given an expression tree like `(a + b) * c` where we check dependencies on `b`:
 * - `a` and `c` don't depend on `b`, so they would be in the returned set
 * - The `+` and `*` operations depend on `b` (directly or indirectly), so they would not be included
 */
private fun ExpressionNode.findAllIndependentOf(node: ExpressionNode): Set<ExpressionNode> {
    fun ExpressionNode.traverse(): Pair<Boolean, Set<ExpressionNode>> {
        if (this === node) return Pair(
            true,
            node.inputs().toList().mapNotNull { it as? ExpressionNode }.toSet()
        )

        var isDependingOnNode = false
        val nonDependingSubNodes = mutableSetOf<ExpressionNode>()
        for (input in inputs().map { it as ExpressionNode }) {
            val result = input.traverse()
            isDependingOnNode = isDependingOnNode || result.first
            nonDependingSubNodes.addAll(result.second)
        }

        return if (isDependingOnNode) Pair(true, nonDependingSubNodes) else Pair(false, setOf(this))
    }

    return traverse().second
}

private fun ExpressionNode.modifyRecursive(modifier: (ExpressionNode) -> ExpressionNode): ExpressionNode =
    modifier(this)