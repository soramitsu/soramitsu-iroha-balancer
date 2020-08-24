package soramitsu.irohautils.balancer.helper;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;

public class ContainerHelper implements Closeable {

  public String userDir = System.getProperty("user.dir");

  public GenericContainer createIrohaBalancerCoreContainer(String contextFolder, String dockerFile) {
    return new GenericContainer(
        new ImageFromDockerfile()
          .withFileFromFile("", new File(contextFolder))
          .withFileFromFile("Dockerfile", new File(dockerFile))
    )
        .withLogConsumer( outputFrame -> System.out.println(outputFrame.toString()))
        .withNetworkMode("host");
  }

  public Boolean isServiceHealthy(GenericContainer serviceContainer) {
    return serviceContainer.isRunning();
  }

  public Boolean isServiceDead(GenericContainer serviceContainer) {
    return !serviceContainer.isRunning();
  }

  @Override
  public void close() throws IOException {

  }
}