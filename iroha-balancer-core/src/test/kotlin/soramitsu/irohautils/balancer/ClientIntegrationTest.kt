package soramitsu.irohautils.balancer

import com.fasterxml.jackson.databind.ObjectMapper
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.Envelope
import jp.co.soramitsu.iroha.java.Transaction
import jp.co.soramitsu.iroha.testcontainers.detail.GenesisBlockBuilder
import org.junit.Assert
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.testcontainers.containers.GenericContainer
import soramitsu.irohautils.balancer.TestContainersMock.rabbitmq
import soramitsu.irohautils.balancer.TestContainersMock.rmqConfig
import soramitsu.irohautils.balancer.client.service.IrohaBalancerClientService
import java.io.IOException
import java.util.*


@SpringBootTest
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ContextConfiguration(initializers = [TestContainersMock.Initializer::class])
class ClientIntegrationTest {

    private val factory = ConnectionFactory()

    private val connection by lazy {
        factory.username = "guest"
        factory.password = "guest"

        factory.host = rmqConfig.host
        factory.port = rmqConfig.port
        factory.newConnection()
    }

    private val channel by lazy { connection.createChannel() }

    @BeforeAll
    fun init(){
        channel.exchangeDeclare("iroha-balancer", "topic", true)
        channel.queueDeclare("torii", true, false, false, null)
        channel.queueBind("torii", "iroha-balancer", "torii")
        channel.basicQos(1)
    }

    @Test
    fun submitTrxViaClient() {
        var messages: ArrayList<String> = ArrayList()
        channel.basicConsume("torii",
                object : DefaultConsumer(channel) {
                    @Throws(IOException::class)
                    override fun handleDelivery(consumerTag: String?,
                                                envelope: Envelope,
                                                properties: AMQP.BasicProperties?,
                                                body: ByteArray?) {
                        val deliveryTag = envelope.deliveryTag
                        println(consumerTag)
                        messages.add(body.toString())
                        channel.basicAck(deliveryTag, false)
                    }
                })

        val client = IrohaBalancerClientService(rmqConfig)
        val transaction = Transaction.builder(GenesisBlockBuilder.defaultAccountId)
                .addAssetQuantity("usd#" + GenesisBlockBuilder.defaultDomainName, "1000")
                .sign(GenesisBlockBuilder.defaultKeyPair)
                .build()
        client.balanceToTorii(transaction)
        Thread.sleep(5000)

        Assert.assertTrue(messages.size > 0)
    }

    @Test
    fun submitBatchTrxViaClient() {

    }
}

class RabbitMqContainer(imageName: String): GenericContainer<RabbitMqContainer>(imageName)
