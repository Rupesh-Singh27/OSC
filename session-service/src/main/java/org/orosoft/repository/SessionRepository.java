package org.orosoft.repository;

import org.orosoft.entity.SessionDetail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository extends JpaRepository<SessionDetail, String> {
}
