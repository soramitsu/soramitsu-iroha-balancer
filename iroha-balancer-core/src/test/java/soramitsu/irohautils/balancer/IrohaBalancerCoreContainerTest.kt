package soramitsu.irohautils.balancer

import iroha.protocol.TransactionOuterClass
import jp.co.soramitsu.iroha.java.Transaction
import jp.co.soramitsu.iroha.java.Utils
import jp.co.soramitsu.iroha.testcontainers.detail.GenesisBlockBuilder
import org.junit.Assert
import org.junit.ClassRule
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.Network
import soramitsu.irohautils.balancer.helper.ContainerHelper
import java.util.*
import javax.xml.bind.DatatypeConverter

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IrohaBalancerCoreContainerTest {

    private val containerHelper = ContainerHelper()

    private val irohaBalancerCoreContextFolder = "${containerHelper.userDir}/build/docker/"
    private val irohaBalancerCoreDockerfile = "${containerHelper.userDir}/build/docker/Dockerfile"

    private val irohaBalancerCoreContainer =
            containerHelper.createIrohaBalancerCoreContainer(irohaBalancerCoreContextFolder, irohaBalancerCoreDockerfile)
                    .withNetwork(Network.SHARED)

    @ClassRule
    val rabbitMq: RabbitMqContainer = RabbitMqContainer("rabbitmq:management")
            .withExposedPorts(5672)

    @BeforeAll
    fun init() {
        rabbitMq.start()

        irohaBalancerCoreContainer
                .withEnv("CAMEL_COMPONENT_RABBITMQ_HOSTNAME", rabbitMq.host)
                .withEnv("CAMEL_COMPONENT_RABBITMQ_PORT_NUMBER", rabbitMq.getMappedPort(5672).toString())
                .withEnv("CAMEL_COMPONENT_RABBITMQ_USERNAME", "guest")
                .withEnv("CAMEL_COMPONENT_RABBITMQ_PASSWORD", "guest")
                .start()
    }

    @AfterAll
    fun tearDown() {
        irohaBalancerCoreContainer.stop()
    }

    @Test
    fun testContainerRunning() {
        Assert.assertTrue(irohaBalancerCoreContainer.isRunning())
    }

}