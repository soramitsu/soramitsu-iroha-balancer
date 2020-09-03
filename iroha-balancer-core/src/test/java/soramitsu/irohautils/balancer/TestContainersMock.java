package soramitsu.irohautils.balancer;

import jp.co.soramitsu.iroha.testcontainers.network.IrohaNetwork;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.GenericContainer;
import soramitsu.irohautils.balancer.client.config.RMQConfig;

import java.util.stream.Collectors;


public class TestContainersMock {

    public static GenericContainer rabbitmq = new GenericContainer("rabbitmq:management")
            .withExposedPorts(5672);
    public static IrohaNetwork network = new IrohaNetwork(5)
            .addDefaultTransaction();
    public static RMQConfig rmqConfig = null;

    public static void start(){
        if (!rabbitmq.isRunning()) {
            rabbitmq.start();
            rmqConfig = new RMQConfig(
                    rabbitmq.getHost(),
                    rabbitmq.getMappedPort(5672),
                    "guest",
                    "guest"
            );
        }

        try {
            network.getApis();
        } catch (Exception e) {
            network.start();
        }
    }

    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(@NotNull ConfigurableApplicationContext configurableApplicationContext) {
            TestContainersMock.start();
            TestPropertyValues values = TestPropertyValues.of(
                    "camel.component.rabbitmq.hostname=" + rabbitmq.getContainerIpAddress(),
                    "camel.component.rabbitmq.port-number=" + rabbitmq.getMappedPort(5672),
                    "iroha.peers=" + network.getToriiAddresses().stream()
                            .map(address -> address.getHost() + ":" + address.getPort())
                            .collect(Collectors.joining(","))
            );
            values.applyTo(configurableApplicationContext);
        }
    }
}
