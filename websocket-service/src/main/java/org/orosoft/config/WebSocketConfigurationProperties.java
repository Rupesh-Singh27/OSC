package org.orosoft.config;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = "websocket")
public class WebSocketConfigurationProperties {
    private String hostname;
    private String protocol;
    private String requestHeader;
    private String logoutUrl;
}
