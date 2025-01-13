package vadl.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.images.builder.ImageFromDockerfile;

class DockerImageStoreTest {
  @Test
  void shouldReturnNewWhenEmpty() {
    DockerImageStore.clear();

    var image = new ImageFromDockerfile();

    var cachedImage = DockerImageStore.replaceWithCachedImage("keyValue", image);

    Assertions.assertEquals(cachedImage, image);
    Assertions.assertTrue(DockerImageStore.containsImage("keyValue"));
    Assertions.assertEquals(cachedImage, DockerImageStore.getImage("keyValue"));
  }

  @Test
  void shouldReturnExistingImage() {
    DockerImageStore.clear();
    var image = new ImageFromDockerfile();
    var original = new ImageFromDockerfile();

    DockerImageStore.setImage("keyValue", original);
    var cachedImage = DockerImageStore.replaceWithCachedImage("keyValue", image);

    Assertions.assertEquals(cachedImage, original);
    Assertions.assertTrue(DockerImageStore.containsImage("keyValue"));
    Assertions.assertEquals(cachedImage, DockerImageStore.getImage("keyValue"));
  }


  @Test
  void shouldReturnFalseWhenNotExisting() {
    DockerImageStore.clear();

    Assertions.assertFalse(DockerImageStore.containsImage("keyValue"));
    Assertions.assertNull(DockerImageStore.getImage("keyValue"));
  }
}