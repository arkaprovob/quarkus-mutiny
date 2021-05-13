package org.acme.getting.started.async;

import javax.enterprise.context.ApplicationScoped;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOError;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Supplier;

@ApplicationScoped
public class GreetingService {

    private static final Logger log = LoggerFactory.getLogger(GreetingService.class.getName());
    private static String threadName = "CUSTOM_TASK_EMIT_ON_EXECUTION_THREAD";
    ThreadFactory emitOnThreadFactory = new NameableThreadFactory(threadName);
    ExecutorService emitExecutor = Executors.newFixedThreadPool(10, emitOnThreadFactory);

    public Uni<String> greeting(String name) {
        log.info("\n\n");
        log.info("\t`greeting(String name)` Executing on Thread {}",Thread.currentThread().getName());

        return Uni
                .createFrom()
                .item(name)
                .emitOn(emitExecutor)
                .onItem()
                .transform(parameter-> {
                    log.debug("`(p)>Transform` invoked on Thread {}",Thread.currentThread().getName());
                    assert Thread.currentThread().getName().equals(threadName);
                    return ioSimulation(parameter);
                }).onFailure()
                .recoverWithItem(()->{
                    log.debug("`recoverWithItem` executing on thread {}",Thread.currentThread().getName());
                    assert Thread.currentThread().getName().equals(threadName);
                    return "something went wrong.. please bear with us";
                });

    }


    public String ioSimulation(String param){
        log.debug("`ioSimulation(String param)` Executing on Thread {}",Thread.currentThread().getName());
        assert Thread.currentThread().getName().equals(threadName);
        try {
            Thread.sleep(8000);
            if (new Random().nextBoolean())
                throw new RuntimeException("something went wrong");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "hello "+param;

    }
}
