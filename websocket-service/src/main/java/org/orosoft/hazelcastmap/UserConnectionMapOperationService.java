package org.orosoft.hazelcastmap;

import org.orosoft.dto.UserConnectionInfo;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class UserConnectionMapOperationService {

    private final Map<String, UserConnectionInfo> userConnectionMap;

    public UserConnectionMapOperationService(
            Map<String, UserConnectionInfo> userConnectionMap
    ) {
        this.userConnectionMap = userConnectionMap;
    }

    /*private final HazelcastInstance hazelcastInstance;
    private IMap<String, UserConnectionInfo> userConnectionMap;

    public UserConnectionMapOperationService(
            @Qualifier("hazelcastInstance") HazelcastInstance hazelcastInstance
    ) {
        this.hazelcastInstance = hazelcastInstance;
    }

    @PostConstruct
    public void initHazelcastMap(){
        userConnectionMap = hazelcastInstance.getMap("userConnectionMap");
    }*/


    public void insertIntoMap(String customUserIdForWebsocket, UserConnectionInfo userConnectionInfo){
        userConnectionMap.put(customUserIdForWebsocket, userConnectionInfo);
    }

    public UserConnectionInfo fetchFromMap(String customUserIdForWebsocket){
        return userConnectionMap.get(customUserIdForWebsocket);
    }

    public void removeFromMap(String customUserIdForWebsocket){
        userConnectionMap.remove(customUserIdForWebsocket);
    }
}
