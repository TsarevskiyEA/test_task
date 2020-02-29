package com.example;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerParameters {
    private static final int DEFAULT_PORT = 8080;

    private List<String> states;
    private Map<String, Person> persons;
    private int port;

    private ServerParameters() { }

    public List<String> getStates() {
        return states;
    }

    public Map<String, Person> getPersons() {
        return persons;
    }

    public int getPort() {
        return port;
    }

    public static ServerParameters build(String... args) {
        ServerParameters result = new ServerParameters();
        if (args.length == 0) {
            System.out.println("No parameters passed. Defaults will be used instead.");
            result.states = Collections.singletonList("default_state");
            result.persons = Collections.emptyMap();
            result.port = 8080;
            return result;
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

        result.states = stateParameters;
        result.persons = buildPersons(nameParameters, stateParameters);
        result.port = portParameters.isEmpty() ? DEFAULT_PORT : Integer.parseInt(portParameters.get(0));
        return result;
    }

    private static Map<String, Person> buildPersons(List<String> names, List<String> states) {
        Map<String, Person> result = new HashMap<>();
        for (int i = 0; i < names.size(); i++) {
            Person person = new Person(names.get(i));
            result.put(person.getName(), person);
            if (states.size() > i)
                person.setState(states.get(i));

        }

        return result;
    }
}
