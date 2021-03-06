package soramitsu.iroha.balancer.integration

import com.google.common.collect.ImmutableList
import io.reactivex.Observable
import iroha.protocol.Endpoint.TxStatus
import iroha.protocol.TransactionOuterClass
import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.Transaction
import jp.co.soramitsu.iroha.java.Utils
import jp.co.soramitsu.iroha.java.subscription.WaitForTerminalStatus
import jp.co.soramitsu.iroha.testcontainers.detail.GenesisBlockBuilder.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.wait.strategy.Wait
import soramitsu.irohautils.balancer.TestContainersMock
import soramitsu.irohautils.balancer.TestContainersMock.network
import soramitsu.irohautils.balancer.TestContainersMock.rmqConfig
import soramitsu.irohautils.balancer.client.service.IrohaBalancerClientService
import soramitsu.irohautils.helper.ContainerHelper
import java.util.*
import java.util.concurrent.TimeUnit
import javax.xml.bind.DatatypeConverter

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IntegrationTests {

    private val irohaBalancerCoreContextFolder = "${ContainerHelper.userDir}/../iroha-balancer-core/build/docker/"
    private val irohaBalancerCoreDockerfile = "${ContainerHelper.userDir}/../iroha-balancer-core/build/docker/Dockerfile"

    private val irohaBalancerCoreContainer = ContainerHelper
            .createIrohaBalancerCoreContainer(
                    irohaBalancerCoreContextFolder,
                    irohaBalancerCoreDockerfile
            )

    private val subscriptionStrategy = WaitForTerminalStatus(
            listOf(
                    TxStatus.COMMITTED,
                    TxStatus.REJECTED,
                    TxStatus.UNRECOGNIZED
            )
    )
    private val crypto = Ed25519Sha3()
    private lateinit var client: IrohaBalancerClientService

    @BeforeAll
    fun init() {
        TestContainersMock.start()

        irohaBalancerCoreContainer
                .withEnv("CAMEL_COMPONENT_RABBITMQ_HOSTNAME", rmqConfig.host)
                .withEnv("CAMEL_COMPONENT_RABBITMQ_PORT_NUMBER", rmqConfig.port.toString())
                .withEnv("CAMEL_COMPONENT_RABBITMQ_USERNAME", rmqConfig.username)
                .withEnv("CAMEL_COMPONENT_RABBITMQ_PASSWORD", rmqConfig.password)
                .withEnv("IROHA_PEERS", TestContainersMock.getPeerAddresses())
                .waitingFor(Wait.forLogMessage(".*Started IrohaBalancerApplication.*", 1))
                .start()

        client = IrohaBalancerClientService(rmqConfig)
    }

    @AfterAll
    fun tearDown() {
        client.close()
        irohaBalancerCoreContainer.close()
        TestContainersMock.stop()
    }

    @Test
    fun createAccountTransactionTest() {
        val transaction: TransactionOuterClass.Transaction = Transaction.builder(defaultAccountId)
                .createAccount("account_", defaultDomainName, crypto.generateKeypair().public)
                .build()
                .sign(defaultKeyPair)
                .build()

        client.balanceToTorii(transaction)

        val iroha = IrohaAPI(network.toriiAddresses[0])

        checkCommitted(transaction, iroha)
    }

    @Test
    fun batchTransactionTest() {
        val transaction1 = Transaction.builder(defaultAccountId)
                .createAsset("usd", defaultDomainName, 2)
                .build()
                .build()

        val transaction2 = Transaction.builder(defaultAccountId)
                .createAsset("khr", defaultDomainName, 0)
                .build()
                .build()

        val batch = ImmutableList.copyOf(Utils.createTxAtomicBatch(listOf(transaction1, transaction2), defaultKeyPair))

        val tx1 = batch[0]
        val tx2 = batch[1]

        client.balanceToListTorii(batch)

        val iroha = IrohaAPI(network.toriiAddresses[0])
        checkCommitted(tx1, iroha)
        checkCommitted(tx2, iroha)
    }

    private fun checkCommitted(transaction: TransactionOuterClass.Transaction, iroha: IrohaAPI) {
        val txStatus = subscriptionStrategy.subscribe(iroha, Utils.hash(transaction))
                // timeout workaround
                .takeUntil(
                        Observable.interval(
                                10L,
                                1L,
                                TimeUnit.SECONDS
                        )
                )
                .blockingLast()
                .txStatus
        assertEquals(TxStatus.COMMITTED, txStatus)
    }
}
