package soramitsu.irohautils.balancer.routes;

import com.google.protobuf.InvalidProtocolBufferException;
import iroha.protocol.Endpoint;
import iroha.protocol.TransactionOuterClass;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import soramitsu.irohautils.balancer.service.IrohaService;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class IrohaBalancerRoute extends RouteBuilder {

    public static final String AMQP_COMMON = "&exchangeType=topic&durable=true&autoDelete=false";

    public static final String RABBITMQ_BALANCE_TO_TORII = "rabbitmq:iroha-balancer?queue=torii&routingKey=torii" + AMQP_COMMON;
    public static final String RABBITMQ_BALANCE_TO_LIST_TORII = "rabbitmq:iroha-balancer?queue=list-torii&routingKey=list-torii" + AMQP_COMMON;


    private final IrohaService irohaService;
    private String[] toriiUris;
    private String[] listToriiUris;

    @Autowired
    private CamelContext camelContext;

    public IrohaBalancerRoute(IrohaService irohaService) {
        this.irohaService = irohaService;
        toriiUris = irohaService.getIrohaPeers().values()
                .stream()
                .map(irohaAPI -> irohaAPI.getUri().toString() + "/iroha.protocol.CommandService_v1?method=torii&synchronous=false")
                .toArray(String[]::new);
        listToriiUris = irohaService.getIrohaPeers().values()
                .stream()
                .map(irohaAPI -> irohaAPI.getUri().toString() + "/iroha.protocol.CommandService_v1?method=listTorii&synchronous=false")
                .toArray(String[]::new);

    }
    private static class ByteArrayList extends ArrayList<byte[]> {}

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
                    log.debug("Received transaction to torii");
                    byte[] body = (byte[]) exchange.getIn().getBody();
                    TransactionOuterClass.Transaction transaction = TransactionOuterClass.Transaction.parseFrom(body);
                    exchange.getMessage().setBody(transaction);
                })
                .loadBalance()
                .failover(irohaService.getIrohaPeers().size(), false, true, false, Throwable.class)
                .to(toriiUris)
                .end()
                .to("mock:toriiUris");

        from(RABBITMQ_BALANCE_TO_LIST_TORII).routeId("BalanceToListTorii")
                .unmarshal().json(JsonLibrary.Jackson, ByteArrayList.class)
                .process(exchange -> {
                    ByteArrayList body = exchange.getIn().getBody(ByteArrayList.class);
                    log.debug("Receive transactions to listTorii, size: {}", body.size());
                    List<TransactionOuterClass.Transaction> transactions =
                            body.stream().map(t -> {
                                try {
                                    return TransactionOuterClass.Transaction.parseFrom(t);
                                } catch (InvalidProtocolBufferException e) {
                                    log.error("Error parsing transaction");
                                    log.debug("Error parsing transaction", e);
                                    return TransactionOuterClass.Transaction.newBuilder().build();
                                }
                            }).collect(Collectors.toList());
                    Endpoint.TxList txList = Endpoint.TxList.newBuilder().addAllTransactions(transactions).build();
                    exchange.getMessage().setBody(txList);
                })
                .loadBalance()
                .failover(irohaService.getIrohaPeers().size(), false, true, true, Throwable.class)
                .to(listToriiUris)
                .end()
                .to("mock:listToriiUris");

    }
}
