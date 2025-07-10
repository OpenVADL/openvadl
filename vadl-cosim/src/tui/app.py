import json

import jq  # type: ignore[import-not-found]
import asyncio
from textual.app import App, ComposeResult, on
from textual.reactive import reactive
from textual.widget import Widget
from textual.widgets import Footer, Header, Input, Tree
from textual.containers import Horizontal, Vertical
from textual.widgets.tree import TreeNode
from .gdb import GDBWindow
from .tree import JsonTree
from ..config import Config

with open("./cosim-run/result/result.json") as f:
    some_json_str = f.read()

some_json = json.loads(some_json_str)


class CosimApp(App):
    CSS_PATH = "cosim.tcss"
    BINDINGS = [("d", "toggle_dark", "Toggle dark mode")]

    config: Config

    def __init__(self, config: Config, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.config = config

    def compose(self) -> ComposeResult:
        yield Header()
        with Horizontal():
            with Vertical(classes="column"):
                yield JsonTree(json_data=some_json, id="json-tree")
                yield Input(placeholder="jq filter, e.g.: .report.passed", id="input-filter")
            with Vertical(classes="column"):
                for i, client in enumerate(self.config.qemu.clients):
                    if client.gdb.enable:
                        yield GDBWindow(remote_target=f"localhost:6000{i}")
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
            t.filtered_data = filter_object(input.value, t.json_data)
        except Exception:
            pass


def filter_object(filter: str, input: object) -> list[object]:
    res = jq.compile(filter).input_value(input).all()
    if len(res) == 1:
        return res[0]
    return res


if __name__ == "__main__":
    app = CosimApp(config)
    app.run()
