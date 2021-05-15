package org.acme.getting.started.async;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import org.acme.getting.started.async.mockio.MockServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

@ApplicationScoped
public class GreetingService {

    private static final Logger log = LoggerFactory.getLogger(GreetingService.class.getName());
    private static String threadName = "CUSTOM_TASK_EMIT_ON_EXECUTION_THREAD";
    ThreadFactory emitOnThreadFactory = new NameableThreadFactory(threadName);
    ExecutorService emitExecutor = Executors.newFixedThreadPool(10, emitOnThreadFactory);

    @PostConstruct
    void init(){
        MockServer.startPickyService();
    }

    public Uni<String> greeting(String name) {
        log.info("\n\n");
        log.info("\t`greeting(String name)` Executing on Thread {}",Thread.currentThread().getName());
        Uni<String> future = Uni
                // Create from a Completion Stage
                .createFrom().completionStage(CompletableFuture.supplyAsync(() -> "hello"));
        return Uni
                .createFrom()
                .item(name) //synchronous now imagine you have retrieve a value from an I/O call you will have to pass a supplier, ref README.md#Links.1
                .emitOn(emitExecutor)
                .onItem() //not required just used for experimental purpose
                .transform(k->k) //not required just used for experimental purpose
                .onItem()
                .transformToUni(parameter -> ioSimulation(parameter, Thread.currentThread()
                        .getName())
                        .map(HttpResponse::bodyAsString) // Transforming the Uni of HttpResponse<Buffer> to String
                )
                .onFailure()
                .retry()
                .atMost(2)
                .map(item-> "Operation completed with item "+item);

    }


    public Uni<HttpResponse<Buffer>> ioSimulation(String param, String threadName){
        log.debug("`ioSimulation(String param)` Executing on Thread {}",Thread.currentThread().getName());
        assert Thread.currentThread().getName().equals(threadName);
        return MockServer.client
                .getAbs("http://localhost:80")
                .addQueryParam("name",param)
                .send()
                .onItem().transform(response-> {
                    if (response.statusCode() == 200){
                        return response;
                    }else{
                        throw  new IllegalStateException(response.bodyAsString());
                    }
        });

    }
}
