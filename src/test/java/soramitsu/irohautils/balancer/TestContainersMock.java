package soramitsu.irohautils.balancer;

import jp.co.soramitsu.iroha.testcontainers.network.IrohaNetwork;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.GenericContainer;

import java.util.stream.Collectors;


public class TestContainersMock {

    public static GenericContainer rabbitmq = new GenericContainer("rabbitmq:management")
            .withExposedPorts(5672);
    public static IrohaNetwork network = new IrohaNetwork(5)
            .addDefaultTransaction();

    static {
        if (!rabbitmq.isRunning()) {
            rabbitmq.start();
        }

        try {
            network.getApis();
        } catch (Exception e) {
            network.start();
        }
    }

    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {

            TestPropertyValues values = TestPropertyValues.of(
                    "camel.component.rabbitmq.hostname=" + rabbitmq.getContainerIpAddress(),
                    "camel.component.rabbitmq.port-number=" + rabbitmq.getMappedPort(5672),
                    "iroha.peers=" + network.getApis().stream()
                            .map(peer -> peer.getUri().getHost() + ":" + peer.getUri().getPort())
                            .collect(Collectors.joining(","))
            );
            values.applyTo(configurableApplicationContext);
        }
    }

}
