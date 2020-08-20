package soramitsu.irohautils.balancer;

import com.codahale.metrics.Counter;
import com.fasterxml.jackson.databind.ObjectMapper;
import iroha.protocol.Endpoint;
import iroha.protocol.TransactionOuterClass;
import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3;
import jp.co.soramitsu.iroha.java.Transaction;
import jp.co.soramitsu.iroha.java.Utils;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.metrics.MetricsComponent;
import org.apache.camel.component.rabbitmq.RabbitMQConstants;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;
import soramitsu.irohautils.balancer.client.config.RMQConfig;
import soramitsu.irohautils.balancer.service.IrohaService;

import javax.xml.bind.DatatypeConverter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedMap;

import static iroha.protocol.Endpoint.TxStatus.COMMITTED;
import static jp.co.soramitsu.iroha.testcontainers.detail.GenesisBlockBuilder.*;

@Testcontainers
@SpringBootTest
@ContextConfiguration(initializers = TestContainersMock.Initializer.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@AutoConfigureWebTestClient
@Slf4j
public class IntegrationTests {

    public static final String RABBITMQ_TRANSACTIONS_PRODUCER =
            "rabbitmq:iroha-balancer?exchangeType=topic&durable=true&autoDelete=false&skipQueueDeclare=true";

    public static final String TORII_ROUTING_KEY = "torii";
    public static final String LIST_TORII_ROUTING_KEY = "list-torii";
    public static final Ed25519Sha3 crypto = new Ed25519Sha3();

    @Autowired
    private IrohaService irohaService;
    @Autowired
    private ProducerTemplate producerTemplate;
    @Autowired
    private CamelContext camelContext;
    @Autowired
    private ObjectMapper objectMapper;

    @Order(10)
    @Test
    @Timeout(30)
    void createAccountsTest() throws Exception {
        List<byte[]> txs = new ArrayList<>();
        List<String> hashes = new ArrayList<>();
        int totalTx = 1000;
        for (int i = 0; i < totalTx; i++) {
            TransactionOuterClass.Transaction createAccountTransaction = Transaction.builder(defaultAccountId)
                    .createAccount("account_" + i, defaultDomainName, crypto.generateKeypair().getPublic())
                    .build()
                    .sign(defaultKeyPair)
                    .build();
            txs.add(createAccountTransaction.toByteArray());
            hashes.add(Utils.toHexHash(createAccountTransaction));
        }
        txs.forEach(tx -> {
            producerTemplate.send(RABBITMQ_TRANSACTIONS_PRODUCER, exchange -> {
                exchange.getMessage().setBody(tx);
                exchange.getMessage().setHeader(RabbitMQConstants.ROUTING_KEY, TORII_ROUTING_KEY);
            });
        });
        hashes.parallelStream().forEach(this::checkCommitted);

        SortedMap<String, Counter> metrics = ((MetricsComponent) camelContext.getComponent("metrics")).getMetricRegistry().getCounters();
        metrics.values().forEach(metric ->
                Assertions.assertTrue(metric.getCount() > (totalTx * .9)/irohaService.getIrohaPeers().size()));

    }

    @Order(20)
    @Test
    @Timeout(30)
    void batchTransactionTest() throws Exception {
        Transaction transaction1 = Transaction.builder(defaultAccountId)
                .createAsset("usd", defaultDomainName, 2)
                .build();
        Transaction transaction2 = Transaction.builder(defaultAccountId)
                .addAssetQuantity("usd#" + defaultDomainName, "1000")
                .build();
        List<String> hashes = Arrays.asList(DatatypeConverter.printHexBinary(Utils.reducedHash(transaction1)),
                DatatypeConverter.printHexBinary(Utils.reducedHash(transaction2)));
        TransactionOuterClass.Transaction tx1 = transaction1.makeMutable()
                .setBatchMeta(TransactionOuterClass.Transaction.Payload.BatchMeta.BatchType.ATOMIC, hashes)
                .build()
                .sign(defaultKeyPair)
                .build();
        TransactionOuterClass.Transaction tx2 = transaction2.makeMutable()
                .setBatchMeta(TransactionOuterClass.Transaction.Payload.BatchMeta.BatchType.ATOMIC, hashes)
                .build()
                .sign(defaultKeyPair)
                .build();
        byte[][] txs = new byte[][]{tx1.toByteArray(), tx2.toByteArray()};

        producerTemplate.send(RABBITMQ_TRANSACTIONS_PRODUCER, exchange -> {
            exchange.getMessage().setBody(objectMapper.writeValueAsString(txs));
            exchange.getMessage().setHeader(RabbitMQConstants.ROUTING_KEY, LIST_TORII_ROUTING_KEY);
        });
        checkCommitted(Utils.toHexHash(tx1));
        checkCommitted(Utils.toHexHash(tx2));
    }

    private void checkCommitted(String trxHash1) {
        Endpoint.TxStatus txStatus = getTxStatus(trxHash1);
        while (COMMITTED != txStatus) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
            }
            txStatus = getTxStatus(trxHash1);
            log.info("Status for {}: {}", trxHash1, txStatus);

        }
    }

    private Endpoint.TxStatus getTxStatus(String trxHash1) {
        return irohaService.getIrohaPeers().values().stream()
                .findAny().get()
                .txStatusSync(DatatypeConverter.parseHexBinary(trxHash1))
                .getTxStatus();
    }

}
