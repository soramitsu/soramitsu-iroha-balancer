package soramitsu.irohautils.balancer

import iroha.protocol.TransactionOuterClass
import jp.co.soramitsu.iroha.java.Transaction
import jp.co.soramitsu.iroha.java.Utils
import jp.co.soramitsu.iroha.testcontainers.detail.GenesisBlockBuilder
import org.junit.ClassRule
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.testcontainers.containers.GenericContainer
import soramitsu.irohautils.balancer.client.config.RMQConfig
import soramitsu.irohautils.balancer.client.service.IrohaBalancerClientService
import java.util.*
import javax.xml.bind.DatatypeConverter

@SpringBootTest
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClientIntegrationTest {

    companion object {
        @ClassRule
        val rabbitMq: RabbitMqContainer = RabbitMqContainer("rabbitmq:management")
                .withExposedPorts(5672)

        fun runContainer() {
            rabbitMq.start()
        }
    }

    @BeforeAll
    fun init() {
        runContainer()
    }

    @Test
    fun createClientService() {
        val rmqConfig = RMQConfig(
                rabbitMq.host,
                rabbitMq.getMappedPort(5672),
                "guest",
                "guest"
        )
        println(rmqConfig)
        val client = IrohaBalancerClientService(rmqConfig)
        val transaction1 = Transaction.builder(GenesisBlockBuilder.defaultAccountId)
                .addAssetQuantity("usd#" + GenesisBlockBuilder.defaultDomainName, "1000")
                .build()
        val hashes = Arrays.asList(DatatypeConverter.printHexBinary(Utils.reducedHash(transaction1)))
        val tx1 = transaction1.makeMutable()
                .setBatchMeta(TransactionOuterClass.Transaction.Payload.BatchMeta.BatchType.ATOMIC, hashes)
                .build()
                .sign(GenesisBlockBuilder.defaultKeyPair)
                .build()
        client.balanceToTorii(tx1)
    }

}

class RabbitMqContainer(imageName: String): GenericContainer<RabbitMqContainer>(imageName)