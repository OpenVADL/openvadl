package vadl.pass;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import org.junit.jupiter.api.Test;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.utils.SourceLocation;
import vadl.viam.Identifier;
import vadl.viam.Specification;

class PassManagerTest {
  static class PassTest extends Pass {

    @Override
    public PassName getName() {
      return new PassName("passNameValue");
    }

    @Override
    public Object execute(final PassResults passResults, Specification viam) {
      return 1337;
    }
  }

  @Test
  void shouldThrowException_whenDuplicatedKey() throws DuplicatedPassKeyException {
    // Given
    var manager = new PassManager();
    var pass = new PassTest();
    manager.add(new PassKey("passKeyValue"), pass);

    // When adding already existing pass
    var exception = assertThrows(DuplicatedPassKeyException.class, () -> {
      manager.add(new PassKey("passKeyValue"), pass);
    });

    // Then
    assertEquals("Pass with the key 'passKeyValue' is duplicated.",
        exception.getMessage());
  }

  @Test
  void shouldStoreResult() throws DuplicatedPassKeyException, URISyntaxException, IOException {
    // Given
    var specification =
        new Specification(new Identifier("nameValue",
            new SourceLocation(new URI("/"), 1)));
    var manager = new PassManager();
    var pass = new PassTest();
    manager.add(new PassKey("passKeyValue"), pass);

    // When
    manager.run(specification);

    // Then
    assertEquals(1, manager.getPassResults().size());
    assertEquals(1337, (Integer) manager.getPassResults().get(
        new PassKey("passKeyValue")));
  }
}