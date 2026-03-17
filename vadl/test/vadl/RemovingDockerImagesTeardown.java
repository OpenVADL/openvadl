// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;
import vadl.lcb.riscv.DockerRiscvImageProvider;

public class RemovingDockerImagesTeardown
    implements BeforeAllCallback, ExtensionContext.Store.CloseableResource {
  private final Logger logger = LoggerFactory.getLogger(RemovingDockerImagesTeardown.class);

  @Override
  public void beforeAll(ExtensionContext context) {
    context.getRoot().getStore(GLOBAL)
        .getOrComputeIfAbsent("teardown", key -> this);
  }

  @Override
  public void close() throws Throwable {
    logger.info("All tests finished!");

    var images = DockerRiscvImageProvider.images;

    if (images.isEmpty()) {
      logger.info("No images to delete");
    }

    for (var imageName : images) {
      logger.info("Removing image: {}", imageName);
      DockerClientFactory.instance().client()
          .removeImageCmd(imageName)
          .withForce(true)
          .exec();
      logger.info("Removing image completed: {}", imageName);
    }
  }
}
