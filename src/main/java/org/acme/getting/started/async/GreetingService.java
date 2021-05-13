package org.acme.getting.started.async;

import javax.enterprise.context.ApplicationScoped;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;

import java.io.IOError;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Supplier;

@ApplicationScoped
public class GreetingService {

    ThreadFactory threadFactory = new NameableThreadFactory("CUSTOM_TASK_SUBSCRIPTION_EXECUTION_THREAD");
    ExecutorService subscriptionExecutor = Executors.newFixedThreadPool(10, threadFactory);
    ThreadFactory emitOnThreadFactory = new NameableThreadFactory("CUSTOM_TASK_EMIT_ON_EXECUTION_THREAD");
    ExecutorService emitExecutor = Executors.newFixedThreadPool(10, emitOnThreadFactory);

    public Uni<String> greeting(String name) {
        System.out.println("\n\n");
        System.out.println("`greeting(String name)` Executing on Thread "+Thread.currentThread().getName());

        // Creating an UNI
        return Uni
                .createFrom()
                .item(() -> {
                    System.out.println("`()Supplier` invoked on Thread "+Thread.currentThread().getName());
                    return GreetingService.this.ioSimulation(name);
                })
                .runSubscriptionOn(subscriptionExecutor) //Infrastructure.getDefaultExecutor()
                .emitOn(emitExecutor)
                .onItem()
                .transform(s-> {
                    System.out.println("`(p)>Transform` invoked on Thread "+Thread.currentThread().getName());
                    return s+" Further Transformation XXX";
                });

    }


    public String ioSimulation(String param){
        System.out.println("`ioSimulation(String param)` Executing on Thread "+Thread.currentThread().getName());
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
