package org.orosoft.repository;

import org.orosoft.entity.SessionDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface SessionRepository extends JpaRepository<SessionDetail, String> {

    @Modifying
    @Transactional
    @Query("update SessionDetail set logoutTime=:logoutTime where sessionId=:sessionId")
    void updateLogoutTimeBySessionId(@Param("sessionId") String sessionId, @Param("logoutTime") String logoutTime);
}
