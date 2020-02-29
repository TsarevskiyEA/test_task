package com.example;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.server.Route;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;

import java.util.Map;
import java.util.concurrent.CompletionStage;

import static akka.http.javadsl.server.Directives.*;
import static akka.http.javadsl.server.PathMatchers.segment;

public class AkkaHttpServer implements AutoCloseable{

    private final ActorSystem system;
    private final CompletionStage<ServerBinding> binding;
    private Map<String, Person> persons;
    private int port;

    public AkkaHttpServer(ServerParameters args) {
        this.persons = args.getPersons();
        this.port = args.getPort();
        system = ActorSystem.create("routes");

        final Http http = Http.get(system);
        final ActorMaterializer materializer = ActorMaterializer.create(system);
        final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = personsRoute().flow(system, materializer);
        binding = http.bindAndHandle(routeFlow,
                ConnectHttp.toHost("localhost", port), materializer);

        new PersonStateManager(persons, args.getStates()).start();
    }

    private Route personsRoute() {
        return get(() ->
                persons.entrySet().stream().map(entry ->
                                path(segment("check").slash().concat(entry.getKey()), () -> complete(entry.getValue().toString()))
                        ).reduce(reject(), Route::orElse)
        );
    }

    @Override
    public void close() {
        binding
                .thenCompose(ServerBinding::unbind) // trigger unbinding from the port
                .thenAccept(unbound -> system.terminate()); // and shutdown when done
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 0 && "help".equalsIgnoreCase(args[0].replaceAll("-", ""))) {
            System.out.println("" +
                    "Usage: myserver [-p SERVICE_PORT] [-n NAME...] [-s STATE...]\n" +
                    "Default values:\n" +
                    "  PORT             8080\n" +
                    "  STATE            default_state\n" +
                    "Examples:\n" +
                    "  myserver -p 8090 -n Alice, Bob - s good, bad.\n" +
                    "");
            return;
        }

        ServerParameters serverParameters = ServerParameters.build(args);
        try (AkkaHttpServer server = new AkkaHttpServer(serverParameters)) {
            System.out.println("Server online at http://localhost:" + server.port + "/\nPress RETURN to stop...");
            System.in.read(); // let it run until user presses return
        }
    }
}
