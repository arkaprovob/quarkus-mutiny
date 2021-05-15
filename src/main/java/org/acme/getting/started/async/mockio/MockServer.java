package org.acme.getting.started.async.mockio;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Future;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;


public class MockServer {

    private static final Logger log = LoggerFactory.getLogger(MockServer.class.getName());

    private static final Vertx vertx = Vertx.vertx();
    public final static WebClient client = WebClient.create(vertx);


    public static void startPickyService() {
        Random random = new Random();
        vertx.createHttpServer()
                .requestHandler(req -> { //providing this handler as callback when the operation is done call back sends the response back to the request origin

                    if (random.nextBoolean()) {
                        String param = req.getParam("name");

                        executeMockedBlockingOperation().subscribe().with(res->{  //because of laziness without subscription the result wont return to the requester
                            log.info("output of executeBlocking operation is {}",res);
                            req.response().endAndForget("Hello! there "+param);
                        },throwable -> {
                            log.info("something went wrong {}",throwable.getMessage());
                            req.response().endAndForget("hi "+param+
                                    " sorry to inform you that the blocking call failed due to "
                                    +throwable.getMessage());
                        });


                    } else {
                        log.info("error simulation block reached");
                        req.response().setStatusCode(500).endAndForget("error simulation");
                    }
                })
                .listenAndAwait(80);
    }

    private static Uni<Object> executeMockedBlockingOperation() {
        return vertx.executeBlocking(promise -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                throw new RuntimeException("InterruptedException " +
                        "occurred with cause "+e.getMessage());  // Of course I don't want to handle it, :P
            }
            promise.complete("sleeping operation was successful");
        }, true);
    }
}
