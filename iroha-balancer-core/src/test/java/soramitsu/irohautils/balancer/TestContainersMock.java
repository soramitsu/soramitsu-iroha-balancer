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
    public static IrohaNetwork network = new IrohaNetwork(3)
            .addDefaultTransaction();
    public static RMQConfig rmqConfig = null;

    public static void start(){
        if (!rabbitmq.isRunning()) {
            var username = "testUser";
            var password = "testPassword";
            rabbitmq.withEnv("RABBITMQ_DEFAULT_USER", username)
                    .withEnv("RABBITMQ_DEFAULT_PASS", password)
                    .start();
            rmqConfig = new RMQConfig(
                    rabbitmq.getContainerIpAddress(),
                    rabbitmq.getMappedPort(5672),
                    username,
                    password
            );
        }

        try {
            network.getApis();
        } catch (Exception e) {
            network.start();
        }
    }

    public static void stop(){
        if(rabbitmq.isRunning()) {
            rabbitmq.stop();
        }
        network.stop();
    }

    public static String getPeerAddresses() {
        return network.getToriiAddresses().stream()
                .map(address -> address.getHost() + ":" + address.getPort())
                .collect(Collectors.joining(","));
    }

    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(@NotNull ConfigurableApplicationContext configurableApplicationContext) {
            TestContainersMock.start();
            TestPropertyValues values = TestPropertyValues.of(
                    "camel.component.rabbitmq.hostname=" + rmqConfig.getHost(),
                    "camel.component.rabbitmq.port-number=" + rmqConfig.getPort(),
                    "iroha.peers=" + getPeerAddresses()
            );
            values.applyTo(configurableApplicationContext);
        }
    }
}
