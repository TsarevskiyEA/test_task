package com.example;

public class Person {
    public static final String DEFAULT_STATE = "default_state";

    private final String name;
    private String state;
    private long stateChangeTime;

    public Person(String name) {
        this.name = name;
        this.state = DEFAULT_STATE;
        this.stateChangeTime = System.currentTimeMillis();
    }

    public String getName() {
        return name;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
        this.stateChangeTime = System.currentTimeMillis();
    }

    public long getStateChangeTime() {
        return stateChangeTime;
    }

    @Override
    public String toString() {
        return '{' +
                "\"name\":\"" + name + '\"' +
                ",\"state\":\"" + state + '\"' +
                ",\"since\":\"" + stateChangeTime  + '\"' +
                '}';
    }
}
