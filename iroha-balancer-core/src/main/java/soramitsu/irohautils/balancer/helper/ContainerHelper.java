package soramitsu.irohautils.balancer.helper;

import java.io.Closeable;
import java.io.IOException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;

public class ContainerHelper implements Closeable {

  @Override
  public void close() throws IOException {

  }

//  /**
//   *
//   * @param contextFolder
//   * @param dockerFile
//   * @return
//   */
//  public GenericContainer createIrohaBalancerContainer(String contextFolder, String dockerFile) {
//    return GenericContainer(
//
//    )
//  }

//  /**
//   * Creates sora-plugin based docker container
//   * @return container
//   */
//  fun createSoraPluginContainer(contextFolder: String, dockerFile: String): KGenericContainerImage {
//    return KGenericContainerImage(
//        ImageFromDockerfile()
//            .withFileFromFile("", File(contextFolder))
//            .withFileFromFile("Dockerfile", File(dockerFile))
//
//    )
//        .withLogConsumer { outputFrame -> print(outputFrame.utf8String) }
//            .withNetworkMode("host")
//  }
}
