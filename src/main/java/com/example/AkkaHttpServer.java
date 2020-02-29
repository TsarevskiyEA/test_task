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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import static akka.http.javadsl.server.Directives.*;

public class AkkaHttpServer implements AutoCloseable{
    private static final int DEFAULT_PORT = 8080;

    private final ActorSystem system;
    private final CompletionStage<ServerBinding> binding;
    private List<String> states;
    private Map<String, Person> persons;
    private int port;

    public AkkaHttpServer(String[] args) {
        processArgs(args);
        PersonStateManager personStateManager = new PersonStateManager(persons, states);
        system = ActorSystem.create("routes");

        final Http http = Http.get(system);
        final ActorMaterializer materializer = ActorMaterializer.create(system);

        final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = personsRoute().flow(system, materializer);
        binding = http.bindAndHandle(routeFlow,
                ConnectHttp.toHost("localhost", port), materializer);
        personStateManager.start();
    }

    private Route personsRoute() {
        return get(() ->
                persons.entrySet().stream().map(entry ->
                                path(entry.getKey(), () -> complete(entry.getValue().toString()))
                        ).reduce(reject(), Route::orElse)
        );
    }

    @Override
    public void close() {
        binding
                .thenCompose(ServerBinding::unbind) // trigger unbinding from the port
                .thenAccept(unbound -> system.terminate()); // and shutdown when done
    }

    private void processArgs(String[] args) {
        if (args.length == 0) {
            System.out.println("No parameters passed. Defaults will be used instead.");
            states = Collections.singletonList("default_state");
            persons = Collections.emptyMap();
            port = 8080;
            return;
        }

        List<String> portParameters = new ArrayList<>();
        List<String> nameParameters = new ArrayList<>();
        List<String> stateParameters = new ArrayList<>();
        List<String> currentList = portParameters;
        Map<String, List<String>> parameterKeysMap = new HashMap<>();
        parameterKeysMap.put("-p", portParameters);
        parameterKeysMap.put("-n", nameParameters);
        parameterKeysMap.put("-s", stateParameters);
        for (String arg : args) {
            List<String> list = parameterKeysMap.get(arg);
            if (list != null) {
                currentList = list;
                continue;
            }

            currentList.add(arg.replaceAll("(\\s|,)", ""));
        }

        if (portParameters.size() > 1) {
            System.out.println("To much port parameters passed. Only FIRST will be used and others will be ignored");
        }

        states = stateParameters;
        persons = buildPersons(nameParameters, states);
        port = portParameters.isEmpty() ? DEFAULT_PORT : Integer.parseInt(portParameters.get(0));
    }

    Map<String, Person> buildPersons(List<String> names, List<String> states) {
        Map<String, Person> result = new HashMap<>();
        for (int i = 0; i < names.size(); i++) {
            Person person = new Person(names.get(i));
            result.put(person.getName(), person);
            if (states.size() > i)
                person.setState(states.get(i));

        }

        return result;
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

        try (AkkaHttpServer server = new AkkaHttpServer(args)) {
            System.out.println("Server online at http://localhost:" + server.port + "/\nPress RETURN to stop...");
            System.in.read(); // let it run until user presses return
        }
    }
}
