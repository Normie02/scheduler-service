package com.example.scheduler.repository;

import com.example.scheduler.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    @Query(value = "SELECT * FROM tasks WHERE status = 'PENDING' AND scheduled_time <= NOW() " +
            "ORDER BY scheduled_time ASC LIMIT :limit FOR UPDATE SKIP LOCKED",
            nativeQuery = true)
    List<Task> findAndLockNextTasks(@Param("limit") int limit);
}