package soramitsu.iroha.balancer.integration_test

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.testcontainers.containers.Network
import org.testcontainers.junit.jupiter.Testcontainers
import soramitsu.irohautils.balancer.helper.ContainerHelper

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

    @BeforeAll
    fun init() {
        irohaBalancerCoreContainer
                .withEnv("CAMEL_COMPONENT_RABBITMQ_HOSTNAME", TestContainersMock.rabbitmq.host)
                .withEnv("CAMEL_COMPONENT_RABBITMQ_PORT_NUMBER", TestContainersMock.rabbitmq.getMappedPort(5672).toString())
                .withEnv("CAMEL_COMPONENT_RABBITMQ_USERNAME", "guest")
                .withEnv("CAMEL_COMPONENT_RABBITMQ_PASSWORD", "guest")
                .start()
    }

    @Test
    fun dummy() {
        println(TestContainersMock.rabbitmq.getMappedPort(5672))
        println(TestContainersMock.network.peers)
        println(irohaBalancerCoreContainer.getContainerInfo())
    }

}
