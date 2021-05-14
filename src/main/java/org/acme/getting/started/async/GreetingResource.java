package org.acme.getting.started.async;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.smallrye.mutiny.subscription.Cancellable;
import org.jboss.resteasy.annotations.jaxrs.PathParam;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@Path("/v1")
public class GreetingResource {
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
