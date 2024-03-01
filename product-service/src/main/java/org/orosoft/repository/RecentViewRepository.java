package org.orosoft.repository;

import org.orosoft.entity.RecentView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface RecentViewRepository extends JpaRepository<RecentView, Integer> {


    @Query("FROM RecentView WHERE userId = :userId ORDER BY viewDate DESC LIMIT 6")
    List<RecentView> findAllRecentViewForUserInDescending(@Param("userId") String userId);

   /* @Transactional
    @Modifying
    @Query("DELETE FROM RecentView WHERE userId = :userId")
    void deleteOldRecentViews(@Param("userId") String userId);*/

    @Transactional
    @Modifying
    @Query("DELETE FROM RecentView WHERE userId = :userId AND viewDate NOT IN (:latestViewDates)")
    void deleteOldRecentViews(@Param("userId") String userId,@Param("latestViewDates") List<String> latestViewDates);
}
