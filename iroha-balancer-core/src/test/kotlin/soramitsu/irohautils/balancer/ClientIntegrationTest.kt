package soramitsu.irohautils.balancer

import iroha.protocol.TransactionOuterClass
import jp.co.soramitsu.iroha.java.Transaction
import jp.co.soramitsu.iroha.java.Utils
import jp.co.soramitsu.iroha.testcontainers.detail.GenesisBlockBuilder
import org.apache.camel.EndpointInject
import org.apache.camel.component.mock.MockEndpoint
import org.apache.camel.test.spring.junit5.CamelSpringTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.testcontainers.containers.GenericContainer
import soramitsu.irohautils.balancer.client.config.RMQConfig
import soramitsu.irohautils.balancer.client.service.IrohaBalancerClientService
import java.util.*
import javax.xml.bind.DatatypeConverter

@SpringBootTest
//@CamelSpringTest
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ContextConfiguration(initializers = [TestContainersMock.Initializer::class])
class ClientIntegrationTest {

//    companion object {
//        @ClassRule
//        val rabbitMq: RabbitMqContainer = RabbitMqContainer("rabbitmq:management")
//                .withExposedPorts(5672)
//
//        fun runContainer() {
//            rabbitMq.start()
//        }
//    }

    @EndpointInject(uri = "mock:toriiUris")
    private val mockToriiUris: MockEndpoint? = null

    @EndpointInject(uri = "mock:listToriiUris")
    private val mockListToriiUris: MockEndpoint? = null

    @Test
    fun submitTrxViaClient() {
//        val rmqConfig = RMQConfig(
//            rabbitMq.host,
//            rabbitMq.getMappedPort(5672),
//            "guest",
//            "guest"
//        )

        val client = IrohaBalancerClientService(TestContainersMock.rmqConfig)
        val transaction = Transaction.builder(GenesisBlockBuilder.defaultAccountId)
                .addAssetQuantity("usd#" + GenesisBlockBuilder.defaultDomainName, "1000")
                .sign(GenesisBlockBuilder.defaultKeyPair)
                .build()
        client.balanceToTorii(transaction)

        mockToriiUris?.expectedMessageCount(1)
        mockToriiUris?.assertPeriod = 1000
        mockToriiUris?.assertIsSatisfied()
        mockToriiUris?.reset()
    }

    @Test
    fun submitBatchTrxViaClient() {

    }

//    class Initializer: ApplicationContextInitializer<ConfigurableApplicationContext> {
//        override fun initialize(applicationContext: ConfigurableApplicationContext) {
//            runContainer()
//            TestPropertyValues.of(
//                    "camel.component.rabbitmq.hostname=${rabbitMq.host}",
//                    "camel.component.rabbitmq.port-number=${rabbitMq.getMappedPort(5672)}"
//            ).applyTo(applicationContext.environment)
//        }
//    }
}

class RabbitMqContainer(imageName: String): GenericContainer<RabbitMqContainer>(imageName)