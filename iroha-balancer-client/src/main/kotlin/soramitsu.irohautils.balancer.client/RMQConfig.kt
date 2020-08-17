package soramitsu.irohautils.balancer.client

data class RMQConfig (
        val host: String,
        val port: Int,
        val username: String?,
        val password: String?

)