package soramitsu.irohautils.balancer

import org.junit.Assert
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.test.context.ContextConfiguration
import org.testcontainers.containers.Network
import soramitsu.irohautils.balancer.TestContainersMock.rmqConfig
import soramitsu.irohautils.balancer.helper.ContainerHelper

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ContextConfiguration(initializers = [TestContainersMock.Initializer::class])
class IrohaBalancerCoreContainerTest {

    private val containerHelper = ContainerHelper()

    private val irohaBalancerCoreContextFolder = "${containerHelper.userDir}/build/docker/"
    private val irohaBalancerCoreDockerfile = "${containerHelper.userDir}/build/docker/Dockerfile"

    private val irohaBalancerCoreContainer =
            containerHelper.createIrohaBalancerCoreContainer(irohaBalancerCoreContextFolder, irohaBalancerCoreDockerfile)
                    .withNetwork(Network.SHARED)
    @BeforeAll
    fun init() {

        irohaBalancerCoreContainer
                .withEnv("CAMEL_COMPONENT_RABBITMQ_HOSTNAME", rmqConfig.host)
                .withEnv("CAMEL_COMPONENT_RABBITMQ_PORT_NUMBER", rmqConfig.port.toString())
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
