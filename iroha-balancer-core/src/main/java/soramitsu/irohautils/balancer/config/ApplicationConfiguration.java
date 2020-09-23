package soramitsu.irohautils.balancer.config;

import jp.co.soramitsu.iroha.java.IrohaAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class ApplicationConfiguration {

    private final IrohaConfiguration irohaConfiguration;

    public ApplicationConfiguration(IrohaConfiguration irohaConfiguration) {
        this.irohaConfiguration = irohaConfiguration;
    }

    @Bean
    public Map<String, IrohaAPI> irohaAPI() {

        List<String> peers = irohaConfiguration.getPeers();
        Map<String, IrohaAPI> irohaPeers = new ConcurrentHashMap<>(peers.size());
        for (String peer : peers) {
            String[] split = peer.split(":");
            String host = split[0];
            int port = Integer.parseInt(split[1]);
            irohaPeers.put(peer, new IrohaAPI(host, port));
        }
        return irohaPeers;
    }
}
