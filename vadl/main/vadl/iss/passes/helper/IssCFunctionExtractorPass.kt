package vadl.iss.passes.helper

import vadl.configuration.IssConfiguration
import vadl.iss.passes.AbstractIssPass
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
 * E.g., this is done for [TensorNode] and [FoldNode], as the generated helper function code
 * becomes cleaner.
 */
class IssCFunctionExtractionPass(config: IssConfiguration) : AbstractIssPass(config) {
    override fun getName(): PassName {
        return PassName.of("ISS C Function Extraction")
    }


    @Throws(IOException::class)
    override fun execute(passResults: PassResults, viam: Specification): Any? {
        wholeHelperInstrs(viam).forEach {
            // extract forall-expression nodes rendered through C helper functions
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
        // first extract inner forall expressions
        toProcess.visitInputs(this)

        val creator = when (toProcess) {
            is TensorNode -> toProcess.extractableParameterExpressions()
                ?.let { toProcess.createFunction(it) }
            is FoldNode -> toProcess.extractableParameterExpressions()
                ?.let { toProcess.createFunction(it) }
            else -> return
        }
        if (creator == null) return
        toProcess.replaceAndDelete(creator.call)
        instrInfo.addExtractedFunction(creator.definition)
    }

    fun TensorNode.createFunction(params: List<ExpressionNode>): FunctionCreator {
        return FunctionCreator(
            instruction = instruction,
            expr = this,
            idx = this.idx(),
            body = this.body(),
            kindName = "tensor",
            paramExprs = params
        )
    }

    fun FoldNode.createFunction(params: List<ExpressionNode>): FunctionCreator {
        return FunctionCreator(
            instruction = instruction,
            expr = this,
            idx = this.idx(),
            body = this.body(),
            kindName = "fold",
            paramExprs = params
        )
    }
}

private const val MAX_C_EXTRACT_WIDTH_BITS = 64

private fun TensorNode.extractableParameterExpressions(): List<ExpressionNode>? {
    if (this.type().asDataType().bitWidth() > MAX_C_EXTRACT_WIDTH_BITS) {
        return null
    }
    return this.body().extractableParameterExpressionsFor(this.idx())
}

private fun FoldNode.extractableParameterExpressions(): List<ExpressionNode>? {
    if (this.type().asDataType().bitWidth() > MAX_C_EXTRACT_WIDTH_BITS) {
        return null
    }
    return this.body().extractableParameterExpressionsFor(this.idx())
}

/**
 * Computes the parameter expressions for extraction.
 *
 * Starts with values independent of the forall-index and recursively expands
 * any >64-bit values into <=64-bit leaves. This allows extraction of fold/tensor
 * nodes that close over wide reads (e.g., vector register reads), while keeping
 * the generated C-function parameter types <=64-bit.
 *
 * Returns `null` if such a decomposition into narrow leaves is not possible.
 */
private fun ExpressionNode.extractableParameterExpressionsFor(idx: ForIdxNode): List<ExpressionNode>? {
    val independentNodes = this.findAllIndependentOf(idx)
        .filter { it !is ConstantNode }

    val memo = mutableMapOf<ExpressionNode, Set<ExpressionNode>?>()
    val params = linkedSetOf<ExpressionNode>()
    for (node in independentNodes) {
        val widened = node.narrowParameterLeaves(memo) ?: return null
        params.addAll(widened)
    }
    return params.sortedBy { it.id.numericId() }
}

/**
 * Recursively decomposes this node into <=64-bit parameter leaves.
 *
 * - constants contribute no parameters
 * - <=64-bit nodes are valid leaves
 * - >64-bit nodes are replaced by the union of their input leaves
 *
 * Returns `null` if a >64-bit node has no expression inputs to decompose.
 */
private fun ExpressionNode.narrowParameterLeaves(
    memo: MutableMap<ExpressionNode, Set<ExpressionNode>?>
): Set<ExpressionNode>? {
    if (memo.containsKey(this)) {
        return memo[this]
    }
    if (this is ConstantNode) {
        return emptySet()
    }
    if (this.type().asDataType().bitWidth() <= MAX_C_EXTRACT_WIDTH_BITS) {
        return setOf(this)
    }

    val narrowed = linkedSetOf<ExpressionNode>()
    val exprInputs = inputs().toList().mapNotNull { it as? ExpressionNode }
    if (exprInputs.isEmpty()) {
        memo[this] = null
        return null
    }

    for (input in exprInputs) {
        val leaves = input.narrowParameterLeaves(memo) ?: run {
            memo[this] = null
            return null
        }
        narrowed.addAll(leaves)
    }
    memo[this] = narrowed
    return narrowed
}

private class FunctionCreator(
    val instruction: Instruction,
    val expr: ExpressionNode,
    val idx: ForIdxNode,
    val body: ExpressionNode,
    val kindName: String,
    private val paramExprs: List<ExpressionNode>
) {

    private val funcIdent: Identifier
    private val paramArgs: List<Pair<ExpressionNode, Parameter>>
    val definition: Function
    val call: Node

    init {
        val name = "${kindName}_${instruction.simpleName()}_${expr.id}"
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
            expr.type(),
            createFuncGraph("${kindName.replaceFirstChar { it.uppercase() }} Function ${funcIdent.simpleName()}")
        )
        return function
    }

    private fun produceCall(): FuncCallNode {
        return FuncCallNode(definition, NodeList(paramArgs.map { it.first }), expr.type())
    }

    /**
     * Finds parameters and their corresponding argument expressions for the extracted tensor function.
     *
     * When extracting a tensor like `forall i in range tensor (a + b[i])` into a function,
     * we need to identify which values come from outside the tensor scope and should become parameters.
     *
     * This method:
     * 1. Uses precomputed parameter expressions (`paramExprs`) selected by the extractor.
     * 2. Creates a parameter for each independent node (named p0, p1, p2, ...)
     * 3. Returns pairs of (Parameter definition, Expression to pass as argument)
     */
    private fun findParamsArgs(): List<Pair<ExpressionNode, Parameter>> {
        // Create a parameter for each extracted expression, naming them p0, p1, p2, etc.
        return paramExprs.mapIndexed { i, node ->
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

        // Copy the expression tree with parameters substituted
        val exprCopy = expr.copySubExpression()

        // Wrap in a return statement and add to the graph
        val end = addWithInputs(ReturnNode(exprCopy))
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
