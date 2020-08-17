package soramitsu.irohautils.balancer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import soramitsu.irohautils.balancer.config.IrohaConfiguration;

@SpringBootApplication
@EnableConfigurationProperties({IrohaConfiguration.class})
public class IrohaBalancerApplication {

	public static void main(String[] args) {
		SpringApplication.run(IrohaBalancerApplication.class, args);
	}

}
