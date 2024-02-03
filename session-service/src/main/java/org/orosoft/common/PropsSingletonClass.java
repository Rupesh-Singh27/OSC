package org.orosoft.common;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.orosoft.serdes.LoginLogoutSerdes;

import java.util.Properties;

public class PropsSingletonClass {


    public static PropsSingletonClass propsSingletonClassInstance = null;

    private static Properties propertiesInstance;

    private PropsSingletonClass(){
        propertiesInstance = initializeProperties();
    }

    public static PropsSingletonClass getPropsSingletonClassInstance(){

        if(propsSingletonClassInstance == null){
            propsSingletonClassInstance = new PropsSingletonClass();
        }

        return propsSingletonClassInstance;
    }

    public Properties getPropertiesClassInstance(){
        return propertiesInstance;
    }

    private Properties initializeProperties() {
        propertiesInstance = new Properties();
        propertiesInstance.put(StreamsConfig.APPLICATION_ID_CONFIG, "login-details");
        propertiesInstance.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        propertiesInstance.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        propertiesInstance.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, LoginLogoutSerdes.class.getName());

        return propertiesInstance;
    }
}
