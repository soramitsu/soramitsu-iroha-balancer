package soramitsu.irohautils.balancer.client.service

import com.fasterxml.jackson.databind.ObjectMapper
import iroha.protocol.TransactionOuterClass
import org.apache.camel.ProducerTemplate
import org.apache.camel.component.rabbitmq.RabbitMQConstants
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class IrohaBalancerClientService {

    private val RABBITMQ_TRANSACTIONS_PRODUCER = "rabbitmq:iroha-balancer?exchangeType=topic&durable=true&autoDelete=false&skipQueueDeclare=true"

    private val TORII_ROUTING_KEY = "torii"
    private val LIST_TORII_ROUTING_KEY = "list-torii"

    @Autowired
    private lateinit var producerTemplate: ProducerTemplate

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    /**
     * This function sends Iroha transaction to RMQ of Iroha balancer
     */
    fun balanceToTorii(transaction: TransactionOuterClass.Transaction) {
        producerTemplate
                .send(RABBITMQ_TRANSACTIONS_PRODUCER)
                {
                    it.message.body = transaction
                    it.message.setHeader(RabbitMQConstants.ROUTING_KEY, TORII_ROUTING_KEY)
                }
    }

    /**
     * * This function sends list of Iroha transactions to RMQ of Iroha balancer
     */
    fun balanceToListTorii(transactions: List<TransactionOuterClass.Transaction>) {
        val byteListTorii: ArrayList<ByteArray> = ArrayList(transactions
                .map { it.toByteArray() })

        producerTemplate
                .send(RABBITMQ_TRANSACTIONS_PRODUCER)
                {
                    it.message.body = objectMapper.writeValueAsString(byteListTorii)
                    it.message.setHeader(RabbitMQConstants.ROUTING_KEY, LIST_TORII_ROUTING_KEY)
                }
    }
}