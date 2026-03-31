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


import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.ContainerState;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import org.testcontainers.utility.DynamicPollInterval;

public class DockerBuildKitDaemon implements ContainerState {

  public static final int BUILDKIT_DAEMON_PORT = 1334;
  private static final String BUILDKIT_DAEMON_IMAGE = "moby/buildkit:latest";
  private static final String BUILDKIT_DAEMON_CONTAINER_NAME = "buildkitd";

  private static volatile DockerBuildKitDaemon instance;

  private final DockerClient client = DockerClientFactory.lazyClient();
  private final Logger logger = LoggerFactory.getLogger(DockerBuildKitDaemon.class);
  private final String containerId;
  private final InspectContainerResponse containerInfo;

  private DockerBuildKitDaemon() {
    containerId = start();
    containerInfo = setupContainerInfo();
  }

  public static synchronized DockerBuildKitDaemon getRunningInstance() {
    if (instance == null) {
      instance = new DockerBuildKitDaemon();
    }
    return instance;
  }

  private String start() {
    logger.info("Starting docker buildkit daemon");

    var existing = client.listContainersCmd()
        .withShowAll(true)
        .exec().stream()
        .filter(c -> Arrays.stream(c.getNames())
            .anyMatch(name -> name.equals("/" + BUILDKIT_DAEMON_CONTAINER_NAME)))
        .findAny();

    if (existing.isPresent()) {
      var container = existing.get();
      if (container.getState().equals("running")) {
        logger.info("Buildkit daemon already running (container_id: {})", container.getId());
        return container.getId();
      }
      logger.info("Buildkit daemon is not running, starting it (container_id: {})",
          container.getId());
      client.startContainerCmd(container.getId()).exec();
      return container.getId();
    }

    var containerId = client.createContainerCmd(BUILDKIT_DAEMON_IMAGE)
        .withName(BUILDKIT_DAEMON_CONTAINER_NAME)
        .withCmd("--addr", "tcp://0.0.0.0:" + BUILDKIT_DAEMON_PORT)
        .withExposedPorts(ExposedPort.tcp(BUILDKIT_DAEMON_PORT))
        .withHostConfig(HostConfig.newHostConfig()
            .withPrivileged(true)
            .withPortBindings(new PortBinding(
                Ports.Binding.bindPort(BUILDKIT_DAEMON_PORT),
                ExposedPort.tcp(BUILDKIT_DAEMON_PORT)
            ))
        )
        .exec()
        .getId();

    client.startContainerCmd(containerId).exec();
    logger.info("Buildkit daemon started (container_id: {})", containerId);
    return containerId;
  }

  private InspectContainerResponse setupContainerInfo() {
    return Awaitility.await()
        .atMost(5L, TimeUnit.SECONDS)
        .pollInterval(DynamicPollInterval.ofMillis(50L))
        .pollInSameThread()
        .until(
            () -> client.inspectContainerCmd(containerId).exec(),
            response -> response.getNetworkSettings()
                .getPorts()
                .getBindings()
                .entrySet().stream()
                .filter(entry -> Objects.nonNull(entry.getValue()))
                .map(Map.Entry::getKey)
                .anyMatch(port -> port.getPort() == BUILDKIT_DAEMON_PORT)
        );
  }

  @Override
  public List<Integer> getExposedPorts() {
    return List.of(BUILDKIT_DAEMON_PORT);
  }

  @Override
  public InspectContainerResponse getContainerInfo() {
    return containerInfo;
  }
}
