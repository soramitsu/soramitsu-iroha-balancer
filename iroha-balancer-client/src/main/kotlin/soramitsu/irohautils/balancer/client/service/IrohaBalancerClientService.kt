package soramitsu.irohautils.balancer.client.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.impl.DefaultExceptionHandler
import iroha.protocol.TransactionOuterClass
import jp.co.soramitsu.iroha.java.Utils
import mu.KLogging
import soramitsu.irohautils.balancer.client.config.RMQConfig
import java.io.Closeable
import kotlin.system.exitProcess

open class IrohaBalancerClientService @JvmOverloads constructor(
        val rmqConfig: RMQConfig,
        private val onRmqFail: () -> Unit = {
            logger.error { "RMQ failure. Exit." }
            exitProcess(1)
        }
) : Closeable {

    private val factory = ConnectionFactory()

    private val objectMapper = ObjectMapper()

    private val connection by lazy {
        if (rmqConfig.username != null && rmqConfig.password != null) {
            logger.info { "Authenticate RMQ user: ${rmqConfig.username}" }
            factory.username = rmqConfig.username
            factory.password = rmqConfig.password
        }

        // Handle RMQ connection errors
        factory.exceptionHandler = object : DefaultExceptionHandler() {
            override fun handleConnectionRecoveryException(conn: Connection, exception: Throwable) {
                logger.error { "RMQ connection error: $exception" }
                onRmqFail()
            }

            override fun handleUnexpectedConnectionDriverException(conn: Connection, exception: Throwable) {
                logger.error { "RMQ connection error: $exception" }
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
        logger.info { "Submitting transaction to balancer: ${Utils.toHexHash(transaction)}" }
        channel.basicPublish(
                "",
                RABBITMQ_TRANSACTIONS_PRODUCER_TORII,
                null,
                transaction.toByteArray()
        )
    }

    /**
     * * This function sends list of Iroha transactions to RMQ of Iroha balancer
     */
    fun balanceToListTorii(transactions: List<TransactionOuterClass.Transaction>) {
        val byteListTorii: ArrayList<ByteArray> = ArrayList(transactions
                .map { it.toByteArray() })

        logger.info { "Submitting batch of transactions to balancer" }
        channel.basicPublish(
                "",
                RABBITMQ_TRANSACTIONS_PRODUCER_LIST_TORII,
                null,
                objectMapper.writeValueAsBytes(byteListTorii))
    }

    override fun close() {
        connection.close()
    }

    companion object : KLogging() {
        private const val AMQP_COMMON = "&exchangeType=topic&durable=true&autoDelete=false"

        private const val RABBITMQ_TRANSACTIONS_PRODUCER_TORII = "rabbitmq:iroha-balancer?queue=torii&routingKey=torii$AMQP_COMMON"
        private const val RABBITMQ_TRANSACTIONS_PRODUCER_LIST_TORII = "rabbitmq:iroha-balancer?queue=list-torii&routingKey=list-torii$AMQP_COMMON"
    }
}
