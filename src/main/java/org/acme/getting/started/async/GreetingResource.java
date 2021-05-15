package org.acme.getting.started.async;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import org.jboss.resteasy.annotations.jaxrs.PathParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@Path("/v1")
public class GreetingResource {
    private static final Logger log = LoggerFactory.getLogger(GreetingResource.class.getName());
    ThreadFactory threadFactory = new NameableThreadFactory("RESOURCE_EMIT_ON_THREAD");
    ExecutorService executor = Executors.newFixedThreadPool(10, threadFactory);
    @Inject
    GreetingService service;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/greeting/{name}")
    public Uni<String> greeting(@PathParam String name) {
        return service.greeting(name);
        //.subscribe().with(resp-> System.out.println(resp),failure->System.out.println(failure))
    }


    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/greeting/callback/{name}")
    public Uni<String> greetingCallback(@PathParam String name) {
        return service.callbackGreeting(name);

    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/wish/{name}")
    public String wish(@PathParam String name) {
        return service.emitterExample(name);
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/wish/blocking/{name}")
    public void wishBlocking(@PathParam String name) {
        Uni.createFrom()
                .item(() -> service.emitterExample(name))
                .subscribe()
                .with(consumer -> log.info("method wishBlocking executed on thread {}", Thread.currentThread().getName()));
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/wish/async/{name}")
    public void wishAsync(@PathParam String name) {
        Uni.createFrom()
                .item(service.emitterExample(name))
                .runSubscriptionOn(executor)
                .subscribe()
                .with(consumer -> log.info("method wishAsync executed on thread {}", Thread.currentThread().getName()));
    }

/*    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/greeting/appr2/{name}")
    public Cancellable appr2(@PathParam String name) {
        return service.greeting(name).
                subscribe().
                with(resp-> System.out.println(resp),
                        failure->System.out.println(failure)); // how do I send this failure to the requester, if i wanted to
    }*/

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<String> hello() {
        return Uni.createFrom().item(() -> "hello")
                .emitOn(Infrastructure.getDefaultExecutor());
    }
}
