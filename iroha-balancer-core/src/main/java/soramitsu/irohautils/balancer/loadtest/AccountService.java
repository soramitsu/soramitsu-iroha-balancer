package soramitsu.irohautils.balancer.loadtest;

import iroha.protocol.TransactionOuterClass;
import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3;
import jp.co.soramitsu.iroha.java.Transaction;
import jp.co.soramitsu.iroha.java.Utils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.rabbitmq.RabbitMQConstants;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static javax.xml.bind.DatatypeConverter.parseHexBinary;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountService {

    private final ResourceLoader resourceLoader;
    private final ProducerTemplate producerTemplate;
    private final List<AccountKeyPair> accountKeyPairList = new ArrayList<>();
    private static final String RABBITMQ_TRANSACTIONS_PRODUCER =
            "rabbitmq:iroha-balancer?exchangeType=topic&durable=true&autoDelete=false&skipQueueDeclare=true";
    private static final String TORII_ROUTING_KEY = "torii";

    @Data
    public static class AccountKeyPair {
        private final String accountId;
        private final String publicKey;
        private final String privateKey;
        private final KeyPair keyPair;

        public AccountKeyPair(String accountId, String publicKey, String privateKey) {
            this.accountId = accountId;
            this.publicKey = publicKey;
            this.privateKey = privateKey;
            this.keyPair = Ed25519Sha3.keyPairFromBytes(
                    parseHexBinary(privateKey),
                    parseHexBinary(publicKey)
            );
        }
    }

    public void init() throws Exception {
        Resource resource = resourceLoader.getResource("classpath:data/bakong_public_bank_users_tst2.csv");
        BufferedReader br = new BufferedReader(new InputStreamReader(resource.getInputStream()));
        String line;
        while ((line = br.readLine()) != null) {
            String[] values = line.split(",");
            accountKeyPairList.add(new AccountKeyPair(values[0],values[1],values[2]));
        }
        log.info("Read {} accounts",accountKeyPairList.size());
        runTest();
    }

    public void runTest() throws Exception { //TODO......
        Random random = new Random();

        while (true) {

            AccountKeyPair sender = accountKeyPairList.get(random.nextInt(accountKeyPairList.size()));
            AccountKeyPair receiver = accountKeyPairList.get(random.nextInt(accountKeyPairList.size()));

            String senderAccountId = sender.getAccountId();
            String receiverAccountId = receiver.getAccountId();
            String description = String.format("Desc-%f", random.nextDouble());


            while (sender.equals(receiver)) {
                receiver = accountKeyPairList.get(random.nextInt(accountKeyPairList.size()));
            }

            TransactionOuterClass.Transaction tx = Transaction.builder(senderAccountId)
                    .transferAsset(senderAccountId, receiverAccountId, "usd#nbc", description, "1.00")
                    .setQuorum(1)
                    .sign(sender.getKeyPair())
                    .build();
            log.info("Send trx {}", Utils.toHexHash(tx));
            sendTransaction(tx.toByteArray());
            Thread.sleep(1000);

        }
    }

    public void sendTransaction(byte[] tx) {

        producerTemplate.send(RABBITMQ_TRANSACTIONS_PRODUCER, exchange -> {
            exchange.getMessage().setBody(tx);
            exchange.getMessage().setHeader(RabbitMQConstants.ROUTING_KEY, TORII_ROUTING_KEY);
        });
    }

}
