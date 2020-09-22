package soramitsu.irohautils.balancer.client.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.rabbitmq.client.BuiltinExchangeType
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.impl.DefaultExceptionHandler
import iroha.protocol.TransactionOuterClass
import jp.co.soramitsu.iroha.java.Utils
import mu.KLogging
import soramitsu.irohautils.balancer.client.config.RMQConfig
import java.io.Closeable

open class IrohaBalancerClientService @JvmOverloads constructor(
        val rmqConfig: RMQConfig,
        private val onRmqFail: () -> Unit = {
            logger.error { "RMQ failure. Exit." }
            throw ClientErrorException("RMQ has failed. Please, check settings and the error.")
        }
) : Closeable {

    private val factory = ConnectionFactory()

    private val objectMapper = ObjectMapper()

    private val connection by lazy {
        if (rmqConfig.username != null && rmqConfig.password != null) {
            factory.username = rmqConfig.username
            factory.password = rmqConfig.password
        }

        // Handle RMQ connection errors
        factory.exceptionHandler = object : DefaultExceptionHandler() {
            override fun handleUnexpectedConnectionDriverException(conn: Connection, exception: Throwable) {
                logger.error { "RMQ connection error: $exception" }
                onRmqFail()
            }
        }

        factory.host = rmqConfig.host
        factory.port = rmqConfig.port
        factory.newConnection()
    }

    private val channel = connection.createChannel()

    init {
        channel.exchangeDeclare(IROHA_BALANCER_EXCHANGER_NAME, BuiltinExchangeType.TOPIC, true, false, emptyMap())
        channel.queueDeclare(TORII_QUEUE_NAME, true, false, false, emptyMap())
        channel.queueDeclare(LIST_TORII_QUEUE_NAME, true, false, false, emptyMap())
        channel.queueBind(TORII_QUEUE_NAME, IROHA_BALANCER_EXCHANGER_NAME, TORII_QUEUE_NAME)
        channel.queueBind(LIST_TORII_QUEUE_NAME, IROHA_BALANCER_EXCHANGER_NAME, LIST_TORII_QUEUE_NAME)
    }

    /**
     * This function sends Iroha transaction to RMQ of Iroha balancer
     */
    fun balanceToTorii(transaction: TransactionOuterClass.Transaction) {
        logger.info { "Submitting transaction to balancer: ${Utils.toHexHash(transaction)}" }
        channel.basicPublish(
                IROHA_BALANCER_EXCHANGER_NAME,
                TORII_QUEUE_NAME,
                null,
                transaction.toByteArray()
        )
    }

    /**
     * This function sends list of Iroha transactions to RMQ of Iroha balancer
     */
    fun balanceToListTorii(transactions: Iterable<TransactionOuterClass.Transaction>) {
        val byteListTorii = ArrayList(transactions
                .map { it.toByteArray() }
        )

        logger.info { "Submitting batch of transactions to balancer" }
        channel.basicPublish(
                IROHA_BALANCER_EXCHANGER_NAME,
                LIST_TORII_QUEUE_NAME,
                null,
                objectMapper.writeValueAsBytes(byteListTorii))
    }

    override fun close() {
        channel.close()
        connection.close()
    }

    companion object : KLogging() {
        const val IROHA_BALANCER_EXCHANGER_NAME = "iroha-balancer"
        const val TORII_QUEUE_NAME = "torii"
        const val LIST_TORII_QUEUE_NAME = "list-torii"
    }
}

class ClientErrorException : Exception {
    constructor(errorMessage: String) : super("$errorMessage")
}
