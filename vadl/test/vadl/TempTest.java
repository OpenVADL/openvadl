package vadl;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

public class TempTest {

  @Test
  void test() throws IOException, InterruptedException {
    try (
        Network network = Network.newNetwork();
        GenericContainer<?> foo = new GenericContainer<>("ubuntu")
            .withNetwork(network)
            .withNetworkAliases("foo")
            .withCommand(
                "/bin/sh",
                "-c",
                "while true ; do printf 'HTTP/1.1 200 OK\\n\\nyay' | nc -l -p 8080; done"
            );
        GenericContainer<?> bar = new GenericContainer<>("ubuntu")
            .withNetwork(network)
            .withCommand("top")
    ) {
      foo.start();
      bar.start();

      String response = bar.execInContainer("wget", "-O", "-", "http://foo:8080").getStdout();
      assertThat(response).as("received response").isEqualTo("yay");
    }
  }


}
