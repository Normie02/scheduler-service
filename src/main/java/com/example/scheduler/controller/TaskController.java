package com.example.scheduler.controller;

import com.example.scheduler.model.Task;
import com.example.scheduler.model.TaskStatus;
import com.example.scheduler.repository.TaskRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskRepository taskRepository;

    public TaskController(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @PostMapping
    public ResponseEntity<Task> scheduleTask(@Valid @RequestBody Task task) {
        if (task.getScheduledTime() == null) {
            task.setScheduledTime(LocalDateTime.now()); // Default to immediate execution
        }
        task.setStatus(TaskStatus.PENDING);
        Task savedTask = taskRepository.save(task);
        return new ResponseEntity<>(savedTask, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Task> getTaskStatus(@PathVariable Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found with ID: " + id));
        return ResponseEntity.ok(task);
    }
}