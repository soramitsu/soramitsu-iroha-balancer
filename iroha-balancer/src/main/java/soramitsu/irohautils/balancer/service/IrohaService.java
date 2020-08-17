package soramitsu.irohautils.balancer.service;

import jp.co.soramitsu.iroha.java.IrohaAPI;
import org.springframework.stereotype.Service;
import soramitsu.irohautils.balancer.config.IrohaConfiguration;

import java.util.Map;

@Service
public class IrohaService {

    private final IrohaConfiguration irohaConfiguration;
    private final Map<String, IrohaAPI> irohaPeers;


    public IrohaService(IrohaConfiguration irohaConfiguration, Map<String, IrohaAPI> irohaPeers) {
        this.irohaConfiguration = irohaConfiguration;
        this.irohaPeers = irohaPeers;
    }

    public Map<String, IrohaAPI> getIrohaPeers() {
        return irohaPeers;
    }


}
