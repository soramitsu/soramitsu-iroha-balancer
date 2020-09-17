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
import org.junit.jupiter.api.Assertions.assertTrue
import org.testcontainers.containers.GenericContainer
import soramitsu.irohautils.balancer.client.config.RMQConfig
import soramitsu.irohautils.balancer.client.service.IrohaBalancerClientService
import soramitsu.irohautils.balancer.client.service.IrohaBalancerClientService.Companion.LIST_TORII_QUEUE_NAME
import soramitsu.irohautils.balancer.client.service.IrohaBalancerClientService.Companion.TORII_QUEUE_NAME
import java.util.concurrent.atomic.AtomicReference

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
        val assetId = "usd#${GenesisBlockBuilder.defaultDomainName}"
        val amount = "1000"

        val messageReference = AtomicReference<ByteArray>()
        channel.basicConsume(TORII_QUEUE_NAME, true,
                { _: String, delivery: Delivery ->
                    messageReference.set(delivery.body)
                },
                { _ -> }
        )

        val transaction = Transaction.builder(GenesisBlockBuilder.defaultAccountId)
                .addAssetQuantity(assetId, amount)
                .sign(GenesisBlockBuilder.defaultKeyPair)
                .build()

        client.balanceToTorii(transaction)

        Thread.sleep(5000)

        val receivedTransaction = TransactionOuterClass.Transaction.parseFrom(messageReference.get())

        assertEquals(transaction.payload.reducedPayload.commandsCount, receivedTransaction.payload.reducedPayload.commandsCount)
        assertEquals(transaction.payload.reducedPayload.createdTime, receivedTransaction.payload.reducedPayload.createdTime)
        assertEquals(transaction.payload.reducedPayload.creatorAccountId, receivedTransaction.payload.reducedPayload.creatorAccountId)
        val receivedCommand = receivedTransaction.payload.reducedPayload.commandsList[0]
        val expectedCommand = transaction.payload.reducedPayload.commandsList[0]
        assertTrue(receivedCommand.hasAddAssetQuantity())
        assertEquals(expectedCommand.addAssetQuantity.amount, receivedCommand.addAssetQuantity.amount)
        assertEquals(expectedCommand.addAssetQuantity.assetId, receivedCommand.addAssetQuantity.assetId)
    }

    @Test
    fun submitBatchTrxViaClient() {
        val assetName = "usd"
        val assetId = "$assetName#${GenesisBlockBuilder.defaultDomainName}"
        val amount = "1000"
        val precision = 2

        val messagesArray = ArrayList<ByteArray>()
        channel.basicConsume(LIST_TORII_QUEUE_NAME, true,
                { _: String, delivery: Delivery ->
                    messagesArray.add(delivery.body)
                },
                { _ -> }
        )

        val transaction1 = Transaction.builder(GenesisBlockBuilder.defaultAccountId)
                .createAsset(assetName, GenesisBlockBuilder.defaultDomainName, precision)
                .build()
                .build()
        val transaction2 = Transaction.builder(GenesisBlockBuilder.defaultAccountId)
                .addAssetQuantity(assetId, amount)
                .build()
                .build()

        client.balanceToListTorii(
                Utils.createTxAtomicBatch(
                        listOf(transaction1, transaction2),
                        GenesisBlockBuilder.defaultKeyPair
                )
        )

        Thread.sleep(5000)

        val receivedTransaction1 = TransactionOuterClass.Transaction.parseFrom(messagesArray[0])
        val receivedTransaction2 = TransactionOuterClass.Transaction.parseFrom(messagesArray[1])

        assertEquals(transaction1.payload.reducedPayload.commandsCount, receivedTransaction1.payload.reducedPayload.commandsCount)
        assertEquals(transaction1.payload.reducedPayload.createdTime, receivedTransaction1.payload.reducedPayload.createdTime)
        assertEquals(transaction1.payload.reducedPayload.creatorAccountId, receivedTransaction1.payload.reducedPayload.creatorAccountId)
        val receivedCommand1 = receivedTransaction2.payload.reducedPayload.commandsList[0]
        val expectedCommand1 = transaction2.payload.reducedPayload.commandsList[0]
        assertTrue(receivedCommand1.hasCreateAsset())
        assertEquals(expectedCommand1.createAsset.assetName, receivedCommand1.createAsset.assetName)
        assertEquals(expectedCommand1.createAsset.domainId, receivedCommand1.createAsset.domainId)
        assertEquals(expectedCommand1.createAsset.precision, receivedCommand1.addAssetQuantity.assetId)

        assertEquals(transaction2.payload.reducedPayload.commandsCount, receivedTransaction2.payload.reducedPayload.commandsCount)
        assertEquals(transaction2.payload.reducedPayload.createdTime, receivedTransaction2.payload.reducedPayload.createdTime)
        assertEquals(transaction2.payload.reducedPayload.creatorAccountId, receivedTransaction2.payload.reducedPayload.creatorAccountId)
        val receivedCommand2 = receivedTransaction2.payload.reducedPayload.commandsList[0]
        val expectedCommand2 = transaction2.payload.reducedPayload.commandsList[0]
        assertTrue(receivedCommand2.hasAddAssetQuantity())
        assertEquals(expectedCommand2.addAssetQuantity.amount, receivedCommand2.addAssetQuantity.amount)
        assertEquals(expectedCommand2.addAssetQuantity.assetId, receivedCommand2.addAssetQuantity.assetId)
    }

    companion object {
        const val DEFAULT_RMQ_PORT = 5672
        const val DEFAULT_USER = "guest"
        const val DEFAULT_PASSWORD = DEFAULT_USER
    }
}

class RabbitMqContainer : GenericContainer<RabbitMqContainer>("rabbitmq:management")
