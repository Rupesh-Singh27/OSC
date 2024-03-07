package org.orosoft.hazelcastmap;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import jakarta.annotation.PostConstruct;
import org.orosoft.common.AppConstants;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class TaskIdMapOperationService {

    private final HazelcastInstance hazelcastInstance;
    private IMap<String, Long> taskIdMap;

    public TaskIdMapOperationService(HazelcastInstance hazelcastInstance) {
        this.hazelcastInstance = hazelcastInstance;
    }

    @PostConstruct
    public void initHazelcastMap(){
        taskIdMap = hazelcastInstance.getMap(AppConstants.TASK_ID_MAP);
    }

    public void insertIntoMap(String customWebsocketId, long periodicTaskId){
        taskIdMap.put(customWebsocketId, periodicTaskId);
    }

    public long getValueForKeyFromMap(String customWebsocketId){

        return Optional
                .ofNullable(taskIdMap.get(customWebsocketId))
                .orElse(-1L);
    }
}
