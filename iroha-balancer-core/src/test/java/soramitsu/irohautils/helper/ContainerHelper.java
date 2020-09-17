package soramitsu.irohautils.helper;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

public class ContainerHelper {

  public static String userDir = System.getProperty("user.dir");

  public static GenericContainer createIrohaBalancerCoreContainer(String contextFolder, String dockerFile) {
    return new GenericContainer(
        new ImageFromDockerfile()
          .withFileFromFile("", new File(contextFolder))
          .withFileFromFile("Dockerfile", new File(dockerFile))
    )
        .withLogConsumer( outputFrame -> System.out.println(outputFrame.toString()))
        .withNetworkMode("host");
  }

  public static Boolean isServiceHealthy(GenericContainer serviceContainer) {
    return serviceContainer.isRunning();
  }

  public static Boolean isServiceDead(GenericContainer serviceContainer) {
    return !serviceContainer.isRunning();
  }
}
