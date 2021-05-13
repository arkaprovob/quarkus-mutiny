package org.acme.getting.started.async.mockio;

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
                .requestHandler(req -> {

                    if (random.nextBoolean()) {
                        String param = req.getParam("name");

                        vertx.executeBlocking(promise -> {
                            try {
                                Thread.sleep(3000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            promise.complete("sleeping operation was successful");
                        },true).subscribe().with(res->{
                            log.info("output of executeBlocking operation is {}",res);
                            req.response().endAndForget("Hello! there "+param);
                        });


                    } else {
                        req.response().setStatusCode(500).endAndForget("error simulation");
                    }
                })
                .listenAndAwait(80);
    }
}
