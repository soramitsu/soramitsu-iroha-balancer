package soramitsu.irohautils.balancer.routes;

import com.google.protobuf.InvalidProtocolBufferException;
import iroha.protocol.Endpoint;
import iroha.protocol.TransactionOuterClass;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.rabbitmq.RabbitMQConstants;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.processor.aggregate.UseLatestAggregationStrategy;
import org.apache.camel.processor.aggregate.UseOriginalAggregationStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import soramitsu.irohautils.balancer.loadtest.AccountService;
import soramitsu.irohautils.balancer.service.IrohaService;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Component
public class IrohaBalancerRoute extends RouteBuilder {

    public static final String AMQP_COMMON = "&exchangeType=topic&durable=true&autoDelete=false&declare=true";

    public static final String RABBITMQ_BALANCE_TO_TORII = "rabbitmq:iroha-balancer?queue=torii&routingKey=torii" + AMQP_COMMON;
    public static final String RABBITMQ_BALANCE_TO_LIST_TORII = "rabbitmq:iroha-balancer?queue=list-torii&routingKey=list-torii" + AMQP_COMMON;


    private final IrohaService irohaService;
    private final AccountService accountService;
    private String[] toriiUris;
    private String[] listToriiUris;

    @Autowired
    private CamelContext camelContext;

    public IrohaBalancerRoute(IrohaService irohaService, AccountService accountService) {
        this.irohaService = irohaService;
        toriiUris = irohaService.getIrohaPeers().values()
                .stream()
                .map(irohaAPI -> irohaAPI.getUri().toString() + "/iroha.protocol.CommandService_v1?method=torii&synchronous=false")
                .toArray(String[]::new);
        listToriiUris = irohaService.getIrohaPeers().values()
                .stream()
                .map(irohaAPI -> irohaAPI.getUri().toString() + "/iroha.protocol.CommandService_v1?method=listTorii&synchronous=false")
                .toArray(String[]::new);

        this.accountService = accountService;
    }

    private static class ByteArrayList extends ArrayList<byte[]> {
    }

    public static final String DIRECT_TIMEOUT_Q = "direct:timeoutQ";
    private AtomicLong counter = new AtomicLong(0);
    @Override
    public void configure() throws Exception {

        for (String uriString : toriiUris) {
            log.info("Interceptor for: {}", uriString);
            URI uri = new URI(uriString);
            String counterName = uri.getHost() + "__" + uri.getPort();
            interceptSendToEndpoint(uriString)
                    .to("metrics:counter:" + counterName + "?increment=1");
        }


        from(RABBITMQ_BALANCE_TO_TORII).routeId("BalanceToTorii")
                .process(exchange -> {
                    long value = counter.incrementAndGet();
                    if (value % 100 == 0) {
                        log.info("Received transaction from torii, count: {}", value);
                    }
                    byte[] body = (byte[]) exchange.getIn().getBody();
                    TransactionOuterClass.Transaction transaction = TransactionOuterClass.Transaction.parseFrom(body);
                    exchange.getMessage().setBody(transaction);
                    log.debug("Going to send transaction: " + transaction);
                })
                .loadBalance()
                .failover(irohaService.getIrohaPeers().size(), false, true, true, Throwable.class)
                .to(toriiUris);


        from(RABBITMQ_BALANCE_TO_LIST_TORII).routeId("BalanceToListTorii")
                .unmarshal().json(JsonLibrary.Jackson, ByteArrayList.class)
                .process(exchange -> {
                    ByteArrayList body = exchange.getIn().getBody(ByteArrayList.class);
                    log.info("Received transactions from listTorii, size: {}", body.size());
                    List<TransactionOuterClass.Transaction> transactions =
                            body.stream().map(t -> {
                                try {
                                    return TransactionOuterClass.Transaction.parseFrom(t);
                                } catch (InvalidProtocolBufferException e) {
                                    log.error("Error parsing transaction", e);
                                    return TransactionOuterClass.Transaction.newBuilder().build();
                                }
                            }).collect(Collectors.toList());
                    Endpoint.TxList txList = Endpoint.TxList.newBuilder().addAllTransactions(transactions).build();
                    exchange.getMessage().setBody(txList);
                    log.debug("Going to send transactions: " + txList);
                })
                .loadBalance()
                .failover(irohaService.getIrohaPeers().size(), false, true, false, Throwable.class)
                .to(listToriiUris)
                .end()
                .to("mock:listToriiUris");

        from("timer://runOnce?repeatCount=1&delay=2000")
                .process(exchange -> {
                    new Thread(() -> {
                        try {
                            accountService.init();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }).run();
                });

        from(DIRECT_TIMEOUT_Q)
                .routeId("TimeQ")
                .aggregate(new UseLatestAggregationStrategy())
                .constant(1)
                .completionTimeout(15000)
                .process(exchange -> {
                    log.info("Init transfers");
                    accountService.init();
                });
    }
}
