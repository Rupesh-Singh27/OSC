package org.orosoft.userservice.repository;

import org.orosoft.userservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmail(String emailId);

    Optional<User> findByUserId(String userid);

    Optional<User> findByUserIdAndPassword(String userId, String password);

    boolean existsByEmail(String userId);
    boolean existsByUserId(String userId);

    boolean existsByUserIdAndPassword(String userId, String passWord);
}
