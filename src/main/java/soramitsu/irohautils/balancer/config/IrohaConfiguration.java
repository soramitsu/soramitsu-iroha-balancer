package soramitsu.irohautils.balancer.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;


@Getter
@Setter
@ConfigurationProperties(prefix = "iroha")
public class IrohaConfiguration {

    List<String> peers;

}
