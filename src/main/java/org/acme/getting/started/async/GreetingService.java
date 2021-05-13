package org.acme.getting.started.async;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import io.smallrye.mutiny.Uni;
import org.acme.getting.started.async.mockio.MockServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

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

        return Uni
                .createFrom()
                .item(name) //synchronous now imagine you have retrieve a value from an I/O call you will have to pass a supplier, ref README.md#Links.1
                .emitOn(emitExecutor)
                .onItem()
                .transform(parameter-> {
                    log.debug("`(p)>Transform` invoked on Thread {}",Thread.currentThread().getName());
                    assert Thread.currentThread().getName().equals(threadName);
                    try {
                        return ioSimulation(parameter,Thread.currentThread().getName()).subscribeAsCompletionStage().get();
                    } catch (InterruptedException | ExecutionException e) {
                        log.error("failed to execute ioSimulation due to {}",e.getMessage());
                        throw new RuntimeException("failed to communicate with client {}"+e.getMessage());
                    }
                }).onFailure()
                .retry() // warning! if your system can handle duplicate requests or entries only then use it function ref README.md#Links.2
                //.when()
        .atMost(2);

    }


    public Uni<String> ioSimulation(String param,String threadName){
        log.debug("`ioSimulation(String param)` Executing on Thread {}",Thread.currentThread().getName());
        assert Thread.currentThread().getName().equals(threadName);
        return MockServer.client
                .getAbs("http://localhost:80")
                .addQueryParam("name",param)
                .send()
                .onItem().transform(response-> {
                    if (response.statusCode() == 200){
                        return response.bodyAsString();
                    }else{
                        throw  new IllegalStateException(response.bodyAsString());
                    }
        });

    }
}
