package com.examly.springapp.repository;

import com.examly.springapp.model.ActivityLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityLogRepo extends JpaRepository<ActivityLog, Long> {
    /** Newest first; the page size limits how many rows come back. */
    List<ActivityLog> findAllByOrderByLoggedAtDescActivityLogIdDesc(Pageable pageable);
}
