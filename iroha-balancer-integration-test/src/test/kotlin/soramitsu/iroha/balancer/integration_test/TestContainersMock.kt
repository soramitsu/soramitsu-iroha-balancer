package soramitsu.iroha.balancer.integration_test

import jp.co.soramitsu.iroha.testcontainers.network.IrohaNetwork
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.testcontainers.containers.GenericContainer
import java.net.URI
import java.util.function.Function
import java.util.stream.Collectors

object TestContainersMock {

    var rabbitmq = RabbitMqTestContainer()
            .withExposedPorts(5672)

    var network: IrohaNetwork = IrohaNetwork(5)
            .addDefaultTransaction()

    fun start() {
        if (!rabbitmq!!.isRunning()) {
            rabbitmq!!.start()
        }
        try {
            network.getApis()
        } catch (e: Exception) {
            network.start()
        }
    }

    class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext?> {
        override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
            start()
            val values: TestPropertyValues = TestPropertyValues.of(
                    "camel.component.rabbitmq.hostname=" + rabbitmq!!.getContainerIpAddress(),
                    "camel.component.rabbitmq.port-number=" + rabbitmq!!.getMappedPort(5672),
                    "iroha.peers=" + network.getToriiAddresses().stream()
                            .map(Function { address: URI -> address.host + ":" + address.port })
                            .collect(Collectors.joining(","))
            )
            values.applyTo(configurableApplicationContext)
        }
    }

    class RabbitMqTestContainer(): GenericContainer<RabbitMqTestContainer>("rabbitmq:management")
}
