package org.acme.getting.started.async.mockio;

import io.vertx.ext.web.Router;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;

import java.util.Random;


public class MockPickyWebService {
    private static final Vertx vertx = Vertx.vertx();
    public final static WebClient client = WebClient.create(vertx);


    public static void startPickyService() {
        Random random = new Random();
        vertx.createHttpServer()
                .requestHandler(req -> {
                    if (random.nextBoolean()) {
                        String param = req.getParam("name");
                        req.response().endAndForget("Hello! there "+param);
                    } else {
                        req.response().setStatusCode(500).endAndForget("error simulation");
                    }
                })
                .listenAndAwait(80);
    }
}
