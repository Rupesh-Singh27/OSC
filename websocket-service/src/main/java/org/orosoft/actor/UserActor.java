package org.orosoft.actor;

import akka.actor.AbstractActor;
import akka.actor.Props;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;

public class UserActor extends AbstractActor {

    HazelcastInstance hazelcastClient = HazelcastClient.newHazelcastClient();

    public static Props getProps(){
        return Props.create(UserActor.class);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(String.class, this::storeDataIntoCache)
                .matchAny((Object object) -> System.out.println("Unexpected message type"))
                .build();
    }

    private void storeDataIntoCache(String header) {

        String[] headerValues = header.split(",");

        String userId = headerValues[1];

        System.out.println("Inside storeDataIntoCache of AKKA Actor for user with user id: " + userId);

        IMap<String, String> headerCache = hazelcastClient.getMap("headerCache");
        headerCache.put(userId, header);
    }
}
