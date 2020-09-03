package soramitsu.irohautils.balancer

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.Delivery
import iroha.protocol.TransactionOuterClass
import jp.co.soramitsu.iroha.java.Transaction
import jp.co.soramitsu.iroha.java.Utils
import jp.co.soramitsu.iroha.testcontainers.detail.GenesisBlockBuilder
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.testcontainers.containers.GenericContainer
import soramitsu.irohautils.balancer.client.config.RMQConfig
import soramitsu.irohautils.balancer.client.service.IrohaBalancerClientService
import soramitsu.irohautils.balancer.client.service.IrohaBalancerClientService.Companion.LIST_TORII_QUEUE_NAME
import soramitsu.irohautils.balancer.client.service.IrohaBalancerClientService.Companion.TORII_QUEUE_NAME
import java.util.*
import javax.xml.bind.DatatypeConverter

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClientIntegrationTest {

    private val rmq = RabbitMqContainer()

    private val factory = ConnectionFactory()

    private lateinit var connection: Connection

    private lateinit var channel: Channel

    private lateinit var rmqConfig: RMQConfig

    private lateinit var client: IrohaBalancerClientService

    @BeforeAll
    fun startRmq() {
        rmq.withExposedPorts(DEFAULT_RMQ_PORT).start()
        rmqConfig = RMQConfig(
                rmq.host,
                rmq.getMappedPort(DEFAULT_RMQ_PORT),
                DEFAULT_USER,
                DEFAULT_PASSWORD
        )
        factory.username = rmqConfig.username
        factory.password = rmqConfig.password
        factory.host = rmqConfig.host
        factory.port = rmqConfig.port

        connection = factory.newConnection()

        client = IrohaBalancerClientService(rmqConfig)
    }

    @BeforeEach
    fun init() {
        channel = connection.createChannel()
        channel.basicQos(1)
    }

    @AfterEach
    fun closeChannel() {
        channel.close()
    }

    @AfterAll
    fun closeConnection() {
        connection.close()
    }

    @Test
    fun submitTrxViaClient() {
        val messages: ArrayList<String> = ArrayList()
        channel.basicConsume(TORII_QUEUE_NAME, true,
                { _: String, delivery: Delivery ->
                    messages.add(String(delivery.body))
                },
                { _ -> }
        )
        val transaction = Transaction.builder(GenesisBlockBuilder.defaultAccountId)
                .addAssetQuantity("usd#" + GenesisBlockBuilder.defaultDomainName, "1000")
                .sign(GenesisBlockBuilder.defaultKeyPair)
                .build()
        client.balanceToTorii(transaction)

        Thread.sleep(5000)

        assertEquals(1, messages.size)
    }

    @Test
    fun submitBatchTrxViaClient() {
        val messages: ArrayList<String> = ArrayList()
        channel.basicConsume(LIST_TORII_QUEUE_NAME, true,
                { _: String, delivery: Delivery ->
                    messages.add(String(delivery.body))
                },
                { _ -> }
        )

        val transaction1 = Transaction.builder(GenesisBlockBuilder.defaultAccountId)
                .createAsset("usd", GenesisBlockBuilder.defaultDomainName, 2)
                .sign(GenesisBlockBuilder.defaultKeyPair)
                .build()
        val transaction2 = Transaction.builder(GenesisBlockBuilder.defaultAccountId)
                .addAssetQuantity("usd#" + GenesisBlockBuilder.defaultDomainName, "1000")
                .sign(GenesisBlockBuilder.defaultKeyPair)
                .build()
        val listOfTransactions = arrayListOf<TransactionOuterClass.Transaction>(transaction1, transaction2)
        client.balanceToListTorii(listOfTransactions)

        Thread.sleep(5000)
        assertEquals(1, messages.size)
    }

    companion object {
        const val DEFAULT_RMQ_PORT = 5672
        const val DEFAULT_USER = "guest"
        const val DEFAULT_PASSWORD = DEFAULT_USER
    }
}

class RabbitMqContainer : GenericContainer<RabbitMqContainer>("rabbitmq:management")
