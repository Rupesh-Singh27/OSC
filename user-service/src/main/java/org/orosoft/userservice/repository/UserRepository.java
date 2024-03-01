package org.orosoft.userservice.repository;

import org.orosoft.userservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, String> {

    @Modifying
    @Query("update User set password=:password where email=:email")
    int updatePasswordByEmail(@Param("password") String password, @Param("email") String email);

    @Query("SELECT u.name FROM User u WHERE u.userId = :userId")
    String findNameByUserId(@Param("userId") String userId);

    boolean existsByEmail(String email);

    boolean existsByUserIdAndPassword(String userId, String passWord);

    /*boolean existsByUserId(String userId);*/

    /* @Query("SELECT u.password FROM User u WHERE u.userId = :userId")
     Optional<String> findPasswordByUserId(@Param("userId") String userId);*/
}
