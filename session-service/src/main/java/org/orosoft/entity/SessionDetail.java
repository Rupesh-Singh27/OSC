package org.orosoft.entity;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "session_details")
public class SessionDetail {

    @Id
    private String sessionId;
    private String userId;
    private String device;
    private String loginTime;
    private String logoutTime;

    @Transient
    private Boolean loginStatus;
}
