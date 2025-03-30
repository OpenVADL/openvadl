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

class InteractiveNav {
    static topOffset = 100
    static headers = []

    static init() {
        window.addEventListener("load", () => {
            let nav = document.querySelector("#nav-tree-contents")
            if (nav) {
                var path = window.location.pathname;
                var filename = path.substring(path.lastIndexOf('/') + 1);
                var navItem = document.querySelector('#nav-tree-contents a[href="' + filename + '"]');
                var navItemLi = navItem.closest('li');

                // if we are in top level, we don't do anything
                var topLevelList = document.querySelector("#nav-tree-contents > ul")
                for (var child of topLevelList.children) {
                    if (child === navItemLi) {
                        return;
                    }
                }

                if (navItemLi.querySelector("a > span.arrow") === null) {
                    return;
                }

                InteractiveNav.openAndAddChildren(navItemLi)

                document.getElementById("doc-content")?.addEventListener(
                    "scroll",
                    this.throttle(InteractiveNav.update, 300)
                )
                InteractiveNav.update()
            }
        })
    }

    static update() {
        let active = InteractiveNav.headers[0]
        InteractiveNav.headers.forEach((header) => {
            let position = header.headerNode.getBoundingClientRect().top
            header.node.classList.remove("active")
            header.node.classList.remove("aboveActive")
            if (position < InteractiveNav.topOffset) {
                active = header
                active?.node.classList.add("aboveActive")
            }
        })
        active?.node.classList.add("active")
        active?.node.classList.remove("aboveActive")
        InteractiveNav.closeNavItems(active)
        InteractiveNav.openAndAddChildren(active?.nodeLi);
    }

    static openAndAddChildren(itemLi) {
        InteractiveNav.headers.forEach((header) => {
            let hLi = header.nodeLi
            if (hLi !== itemLi && hLi.contains(itemLi)) {
                InteractiveNav.openAndAddChildren(header.nodeLi)
            }
        })

        let hasChildren = itemLi?.querySelector("a > .arrow");
        if (!hasChildren) {
            return
        }

        // open if not already open
        if (InteractiveNav.isClosed(itemLi)) {
            console.log("open", itemLi)
            itemLi.querySelector(".arrow").click()
        }

        itemLi.querySelectorAll(":scope > ul.children_ul a")
            .forEach(item => {
                let alreadyInSet = InteractiveNav.headers.some(e => e.node === item)
                if (alreadyInSet) {
                    return
                }
                let id = item.getAttribute("href")
                    .split("#")[1];
                if (id != null) {
                    let headerNode = document.getElementById(id)
                    InteractiveNav.addToHeaders({
                        node: item,
                        nodeLi: item.closest("li"),
                        headerNode: headerNode
                    })
                }
            })
    }

    static closeNavItems(newActive) {
        InteractiveNav.headers.forEach((header) => {
            let hLi = header.node.closest("li");
            // if header is not a parent, we close it
            if (!newActive || !hLi.contains(newActive.nodeLi)) {
                // only if it is not already closed
                if (!InteractiveNav.isClosed(hLi)) {
                    console.log("close", hLi)
                    hLi.querySelector(".arrow")?.click()
                }
            }
        })
    }

    static throttle(func, delay) {
        let lastCall = 0;
        return function (...args) {
            const now = new Date().getTime();
            if (now - lastCall < delay) {
                return;
            }
            lastCall = now;
            return setTimeout(() => {
                func(...args)
            }, delay);
        };
    }

    static addToHeaders(element) {
        // Determine the vertical position of the new element
        const elementTop = element.headerNode.offsetTop;

        // Find the correct insertion index
        let insertIndex = InteractiveNav.headers.findIndex(existingElement =>
            existingElement.headerNode.offsetTop > elementTop
        );

        // If no appropriate index is found, append to the end
        if (insertIndex === -1) {
            insertIndex = InteractiveNav.headers.length;
        }

        // Insert the new element at the determined index
        InteractiveNav.headers.splice(insertIndex, 0, element);
    }

    static isClosed(itemLi) {
        let ul = itemLi.querySelector("ul.children_ul");
        return (ul == null || ul.attributes["style"].textContent.length !== 0) && ul?.style.display !== "block"
    }
}
