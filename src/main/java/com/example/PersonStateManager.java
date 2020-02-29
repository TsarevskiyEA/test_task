package com.example;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PersonStateManager {
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(3);
    private final Random random = new Random();
    private final Map<String, Person> personMap;
    private final List<String> states;

    public PersonStateManager(Map<String, Person> personMap, List<String> states) {
        this.personMap = personMap;
        this.states = states;
    }

    public void start() {
        for (Person person : personMap.values()) {
            scheduleStateUpdate(person);
        }
    }

    private void updateStateAndScheduleNext(Person person) {
        int stateIndex = random.nextInt(states.size());
        person.setState(states.get(stateIndex));
        scheduleStateUpdate(person);
    }

    private void scheduleStateUpdate(Person person){
        int delay = (random.nextInt(5) + 6) * 500;
        executor.schedule(() -> updateStateAndScheduleNext(person), delay, TimeUnit.MILLISECONDS);
    }
}
