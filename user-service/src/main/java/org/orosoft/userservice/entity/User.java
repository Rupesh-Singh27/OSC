package org.orosoft.userservice.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Column(name = "user_id")
    @Id
    private String userId;

    @Column(name = "user_name")
    private String name;

    @Column(name = "user_email")
    private String email;

    @Column(name = "mobile_number")
    private long contact;

    @Column(name = "password")
    private String password;

    @Column(name = "date_of_birth")
    @JsonProperty("DOB")
    private String dataOfBirth;

    @Transient
    private long otp;
}
