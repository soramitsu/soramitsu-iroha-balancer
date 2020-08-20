package soramitsu.irohautils.balancer.client.config

data class RMQConfig (
        val host: String,
        val port: Int,
        val username: String? = null,
        val password: String? = null

)