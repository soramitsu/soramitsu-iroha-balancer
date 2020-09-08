package soramitsu.iroha.balancer.integration_test

import iroha.protocol.Endpoint.TxStatus
import iroha.protocol.TransactionOuterClass
import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3
import jp.co.soramitsu.crypto.ed25519.Utils
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.Transaction
import jp.co.soramitsu.iroha.java.Utils.toHexHash
import jp.co.soramitsu.iroha.testcontainers.detail.GenesisBlockBuilder.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.testcontainers.containers.Network
import org.testcontainers.junit.jupiter.Testcontainers
import soramitsu.iroha.balancer.integration_test.TestContainersMock.network
import soramitsu.irohautils.balancer.client.config.RMQConfig
import soramitsu.irohautils.balancer.client.service.IrohaBalancerClientService
import soramitsu.irohautils.balancer.helper.ContainerHelper
import javax.xml.bind.DatatypeConverter

@Testcontainers
@SpringBootTest
@ContextConfiguration(initializers = [TestContainersMock.Initializer::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IntegrationTests {

    private val containerHelper = ContainerHelper()

    private val irohaBalancerCoreContextFolder = "${containerHelper.userDir}/../iroha-balancer-core/build/docker/"
    private val irohaBalancerCoreDockerfile = "${containerHelper.userDir}/../iroha-balancer-core/build/docker/Dockerfile"

    private val irohaBalancerCoreContainer =
            containerHelper.createIrohaBalancerCoreContainer(irohaBalancerCoreContextFolder, irohaBalancerCoreDockerfile)
                    .withNetwork(Network.SHARED)

    val crypto = Ed25519Sha3()
    private lateinit var client: IrohaBalancerClientService

    @BeforeAll
    fun init() {
        irohaBalancerCoreContainer
                .withEnv("CAMEL_COMPONENT_RABBITMQ_HOSTNAME", TestContainersMock.rabbitmq.host)
                .withEnv("CAMEL_COMPONENT_RABBITMQ_PORT_NUMBER", TestContainersMock.rabbitmq.getMappedPort(5672).toString())
                .withEnv("CAMEL_COMPONENT_RABBITMQ_USERNAME", "guest")
                .withEnv("CAMEL_COMPONENT_RABBITMQ_PASSWORD", "guest")
                .start()

        val rmqConfig = RMQConfig(
                TestContainersMock.rabbitmq.host,
                TestContainersMock.rabbitmq.getMappedPort(5672),
                "guest",
                "guest"
        )
        client = IrohaBalancerClientService(rmqConfig)
    }

    @Test
    fun dummy() {
        val transaction: TransactionOuterClass.Transaction = Transaction.builder(defaultAccountId)
                .createAccount("account_", defaultDomainName, crypto.generateKeypair().getPublic())
                .build()
                .sign(defaultKeyPair)
                .build()
        client.balanceToTorii(transaction)

        val iroha = IrohaAPI(network.toriiAddresses[0])
        checkCommitted(toHexHash(transaction), iroha)
    }
    private fun checkCommitted(trxHash1: String, iroha: IrohaAPI) {
        var txStatus = iroha.txStatusSync(trxHash1.toByteArray()).txStatus
        while (TxStatus.COMMITTED != txStatus) {
            try {
                Thread.sleep(500)
            } catch (e: InterruptedException) {
            }
            txStatus = iroha.txStatusSync(trxHash1.toByteArray()).txStatus
            println("Status for $trxHash1: $txStatus")
        }
    }
}
