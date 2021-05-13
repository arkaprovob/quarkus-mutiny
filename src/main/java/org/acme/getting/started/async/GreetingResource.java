package org.acme.getting.started.async;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.annotations.jaxrs.PathParam;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@Path("/hello")
public class GreetingResource {
    ThreadFactory threadFactory = new NameableThreadFactory("RESOURCE_EMIT_ON_THREAD");
    ExecutorService executor = Executors.newFixedThreadPool(10, threadFactory);

    @Inject
    GreetingService service;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/greeting/{name}")
    public Uni<String> greeting(@PathParam String name) {
        System.out.println("inside rest resource");
        return service.greeting(name).onItem()
                .transform(s ->{
                    System.out.println("`transform(s ->{})`  is running on Thread "+Thread.currentThread().getName());
                    return s.toUpperCase(Locale.ROOT);
                })
                .emitOn(executor)
                .onFailure(throwable -> {
                    System.out.println("exception was "+throwable);
                    return true;
                }).recoverWithItem(()->{
                    System.out.println("`recoverWithItem` is on Thread "+Thread.currentThread().getName());
                    return "Damn not again...";
                })/*.subscribe().with(s-> {
                    System.out.println("`subscription` is on Thread "+Thread.currentThread().getName());
                    System.out.println("output is "+s);
                })*/;
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<String> hello() {
        return Uni.createFrom().item(() -> "hello")
                .emitOn(Infrastructure.getDefaultExecutor());
    }
}
