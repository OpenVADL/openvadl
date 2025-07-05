import json

import jq  # type: ignore[import-not-found]
import asyncio
from textual.app import App, ComposeResult, on
from textual.reactive import reactive
from textual.widget import Widget
from textual.widgets import Footer, Header, Input, Tree
from textual.containers import Horizontal, Vertical
from textual.widgets.tree import TreeNode
from gdb import GDBWindow
from tree import JsonTree

with open("../../../../cosim-run/result/result.json") as f:
    some_json_str = f.read()

some_json = json.loads(some_json_str)


class CosimApp(App):
    CSS_PATH = "cosim.tcss"
    BINDINGS = [("d", "toggle_dark", "Toggle dark mode")]

    def compose(self) -> ComposeResult:
        yield Header()
        with Horizontal():
            with Vertical(classes="column"):
                yield JsonTree(json_data=some_json, id="json-tree")
                yield Input(placeholder="jq filter, e.g.: .report.passed", id="input-filter")
            with Vertical(classes="column"):
                yield GDBWindow()
                yield GDBWindow()
        yield Footer()

    def action_toggle_dark(self) -> None:
        self.theme = (
            "textual-dark" if self.theme == "textual-light" else "textual-light"
        )

    @on(Input.Submitted)
    def filter_json_tree(self):
        input = self.query_one("#input-filter")
        t = self.query_one("#json-tree", JsonTree)
        try:
            t.filtered_data = to_json(input.value, t.json_data)
        except Exception:
            pass


def to_json(compile: str, input: object) -> list[object]:
    res = jq.compile(compile).input_value(input).all()
    if len(res) == 1:
        return res[0]
    return res


def to_json_str(compile: str, input: object) -> str:
    return json.dumps(to_json(compile, input), indent=4)


if __name__ == "__main__":
    app = CosimApp()
    app.run()
