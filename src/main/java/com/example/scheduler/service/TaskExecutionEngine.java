package com.example.scheduler.service;

import com.example.scheduler.model.Task;
import com.example.scheduler.model.TaskStatus;
import com.example.scheduler.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class TaskExecutionEngine {

    private final TaskRepository taskRepository;

    @Value("${app.scheduler.batch-size}")
    private int batchSize;

    public TaskExecutionEngine(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    // 1. Polling mechanism running on a fixed schedule
    @Scheduled(fixedDelayString = "${app.scheduler.polling-interval-ms}")
    public void pollAndExecuteTasks() {
        List<Task> tasksToProcess = fetchAndMarkInProgress();

        for (Task task : tasksToProcess) {
            processTaskAsync(task)
                    .exceptionally(ex -> {
                        handleTaskFailure(task);
                        return null;
                    });
        }
    }

    // 2. Transactional method to lock rows and update status atomically
    @Transactional
    public List<Task> fetchAndMarkInProgress() {
        List<Task> tasks = taskRepository.findAndLockNextTasks(batchSize);
        tasks.forEach(task -> task.setStatus(TaskStatus.IN_PROGRESS));
        return taskRepository.saveAll(tasks);
    }

    // 3. Multithreaded execution using CompletableFuture
    @Async("taskExecutor")
    public CompletableFuture<Void> processTaskAsync(Task task) {
        try {
            // SIMULATE WORK: E.g., Parsing payload and sending an email/notification
            System.out.println("Executing Task: " + task.getName() + " on thread: " + Thread.currentThread().getName());
            Thread.sleep(2000); // Simulated delay

            // Mark as complete
            task.setStatus(TaskStatus.COMPLETED);
            taskRepository.save(task);

            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    // 4. Retry Mechanism
    @Transactional
    public void handleTaskFailure(Task task) {
        if (task.getRetryCount() < task.getMaxRetries()) {
            task.setRetryCount(task.getRetryCount() + 1);
            task.setStatus(TaskStatus.PENDING); // Push back to queue
        } else {
            task.setStatus(TaskStatus.FAILED); // Mark as dead-letter
        }
        taskRepository.save(task);
        System.err.println("Task failed: " + task.getName() + ". Retry count: " + task.getRetryCount());
    }
}