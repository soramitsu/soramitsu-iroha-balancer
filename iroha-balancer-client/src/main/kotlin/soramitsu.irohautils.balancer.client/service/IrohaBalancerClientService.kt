package soramitsu.irohautils.balancer.client.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.impl.DefaultExceptionHandler
import iroha.protocol.TransactionOuterClass
import mu.KotlinLogging
import org.springframework.stereotype.Service
import soramitsu.irohautils.balancer.client.config.RMQConfig
import java.io.Closeable
import kotlin.system.exitProcess

private val logging = KotlinLogging.logger {  }

open class IrohaBalancerClientService @JvmOverloads constructor (
        val rmqConfig: RMQConfig,
        private val onRmqFail: () -> Unit = {
            logging.error{ "RMQ failure. Exit." }
            exitProcess(1)
        }
) : Closeable {

    private val RABBITMQ_TRANSACTIONS_PRODUCER = "rabbitmq:iroha-balancer?exchangeType=topic&durable=true&autoDelete=false&skipQueueDeclare=true"

    private val TORII_ROUTING_KEY = "torii"
    private val LIST_TORII_ROUTING_KEY = "list-torii"

    private val factory = ConnectionFactory()

    private val objectMapper = ObjectMapper()

    private val connection by lazy {
        if (rmqConfig.username != null && rmqConfig.password != null) {
            logging.info { "Authenticate RMQ user: ${rmqConfig.username}" }
            factory.username = rmqConfig.username
            factory.password = rmqConfig.password
        }

        // Handle RMQ connection errors
        factory.exceptionHandler = object : DefaultExceptionHandler() {
            override fun handleConnectionRecoveryException(conn: Connection, exception: Throwable) {
                logging.error{ "RMQ connection error: $exception" }
                onRmqFail()
            }

            override fun handleUnexpectedConnectionDriverException(conn: Connection, exception: Throwable) {
                logging.error{ "RMQ connection error: $exception" }
                onRmqFail()
            }
        }

        factory.host = rmqConfig.host
        factory.port = rmqConfig.port
        factory.newConnection()
    }

    private val channel by lazy { connection.createChannel() }

    /**
     * This function sends Iroha transaction to RMQ of Iroha balancer
     */
    fun balanceToTorii(transaction: TransactionOuterClass.Transaction) {
        logging.info { "Submitting transaction to balancer" }
        channel.basicPublish(RABBITMQ_TRANSACTIONS_PRODUCER, TORII_ROUTING_KEY, null, transaction.toByteArray())
    }

    /**
     * * This function sends list of Iroha transactions to RMQ of Iroha balancer
     */
    fun balanceToListTorii(transactions: List<TransactionOuterClass.Transaction>) {
        val byteListTorii: ArrayList<ByteArray> = ArrayList(transactions
                .map { it.toByteArray() })

        logging.info { "Submitting batch of transactions to balancer" }
        channel.basicPublish(
                RABBITMQ_TRANSACTIONS_PRODUCER,
                LIST_TORII_ROUTING_KEY,
                null,
                objectMapper.writeValueAsBytes(byteListTorii))
    }

    override fun close() {
        connection.close()
    }
}