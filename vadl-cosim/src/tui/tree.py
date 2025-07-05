from textual.app import ComposeResult
from textual.widget import Widget
from textual.widgets.tree import TreeNode
from textual.reactive import reactive
from textual.widgets import Tree

class JsonTree(Widget):
    json_data: reactive[object | None] = reactive(None)
    filtered_data: reactive[object | None] = reactive(None)
    inner_tree: Tree[str]

    def __init__(self, json_data: object, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.inner_tree: Tree[str] = Tree(".")
        self.json_data = json_data
        self.filtered_data = json_data
        self.inner_tree.root.data = self.json_data  # type: ignore[assignment]

    def compose(self) -> ComposeResult:
        yield self.inner_tree

    def load_obj(self, obj: object, parent: TreeNode, depth: int):
        if depth <= 0:
            return

        if isinstance(obj, dict):
            if len(obj) == 0:
                parent.add_leaf("<empty>", data=obj)
            else:
                for k, v in obj.items():
                    child = parent.add(str(k), data=v)
                    self.load_obj(v, child, depth - 1)
        elif isinstance(obj, list):
            if len(obj) == 0:
                parent.add_leaf("<empty>", data=obj)
            else:
                for i, v in enumerate(obj):
                    child = parent.add(f"{i}", data=v)
                    self.load_obj(v, child, depth - 1)
        else:
            parent.add_leaf(str(obj), data=obj)

    def on_tree_node_expanded(self, message: Tree.NodeExpanded):
        obj = message.node.data
        if obj is None:
            return
        parent = message.node
        self.load_obj(obj, parent, depth=1)
        parent.data = None

    def watch_filtered_data(self, old_value, new_value):
        self.inner_tree.root.data = new_value
        self.inner_tree.root.collapse_all()
        self.inner_tree.root.remove_children()
        self.inner_tree.root.expand()
