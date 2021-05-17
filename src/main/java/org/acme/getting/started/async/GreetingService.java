package org.acme.getting.started.async;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import org.acme.getting.started.async.mockio.MockServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@ApplicationScoped
public class GreetingService {

    private static final Logger log = LoggerFactory.getLogger(GreetingService.class.getName());
    private static final Random random = new Random();
    private static String threadName = "CUSTOM_TASK_EMIT_ON_EXECUTION_THREAD";
    ThreadFactory emitOnThreadFactory = new NameableThreadFactory(threadName);
    ExecutorService emitExecutor = Executors.newFixedThreadPool(10, emitOnThreadFactory);

    @PostConstruct
    void init() {
        MockServer.startPickyService();
    }

    public Uni<String> greeting(String name) {
        log.info("\n\n");
        log.info("\t`greeting(String name)` Executing on Thread {}", Thread.currentThread().getName());
        Uni<String> future = Uni
                // Create from a Completion Stage
                .createFrom().completionStage(CompletableFuture.supplyAsync(() -> "hello"));
        return Uni
                .createFrom()
                .item(name) //this is synchronous, but if you have retrieve a value from an I/O call you will have to pass a supplier, ref README.md#Links.1
                .emitOn(emitExecutor)
                .onItem() //not required just used for experimental purpose
                .transform(k -> k) //not required just used for experimental purpose
                .onItem()
                .transformToUni(parameter -> ioSimulation(parameter, Thread.currentThread()
                        .getName())
                        .map(HttpResponse::bodyAsString) // Transforming the Uni of HttpResponse<Buffer> to String
                )
                .onFailure()
                .retry()
                .atMost(2)
                .map(item -> "Operation completed with item " + item);

    }

    public Uni<String> callbackGreeting(String name) {
        log.info("\n\n");
        log.info("\t`callbackGreeting(String name)` Executing on Thread {}", Thread.currentThread().getName());

        return Uni
                .createFrom()
                .item(name) //synchronous now imagine you have retrieve a value from an I/O call you will have to pass a supplier, ref README.md#Links.1
                .emitOn(emitExecutor)
                .onItem()
                .transformToUni(param -> {
                    return Uni.createFrom().emitter(em -> {
                        ioSimulation(param, Thread.currentThread().getName())
                                .subscribe()
                                .with(success -> em.complete(success.bodyAsString()), //putting result of uni ioSimulation into the callback, also works in rest apis  on return type Uni<T> like in case of failure return the stack trace to the user
                                        exception -> {
                                            log.info("the following error occurred before sending to em.complete {}", exception.getMessage());
                                            em.fail(exception);
                                        });

                    });
                })
                .onFailure()
                .retry()
                .atMost(2)
                .map(item -> "Operation completed with item " + item);

    }

    public String emitterExample(String input) {
        log.info("method emitterExample executing on thread {}", Thread.currentThread().getName());
        Uni<String> cache = Uni.createFrom().emitter(em -> new Thread(() -> {
            try {
                log.info("emitter executing on thread {}", Thread.currentThread().getName());
                Thread.sleep(2000);
                if (random.nextBoolean()) {
                    String result = "have a great day " + input;
                    em.complete(result);
                } else {
                    em.fail(new RuntimeException("random boolean returned false"));
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start());
        return cache.await().indefinitely();
    }

    public Uni<HttpResponse<Buffer>> ioSimulation(String param, String threadName) {
        log.debug("`ioSimulation(String param)` Executing on Thread {}", Thread.currentThread().getName());
        assert Thread.currentThread().getName().equals(threadName);
        return MockServer.client
                .getAbs("http://localhost:80")
                .addQueryParam("name", param)
                .send()
                .onItem().transform(response -> {
                    if (response.statusCode() == 200) {
                        return response;
                    } else {
                        throw new IllegalStateException(response.bodyAsString());
                    }
                });

    }
}
