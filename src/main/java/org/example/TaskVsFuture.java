package org.example;

import java.util.concurrent.CompletableFuture;

public class TaskVsFuture<T> {
    final Main.Task<T> task;
    final CompletableFuture<T> completableFuture;

    public TaskVsFuture(Main.Task<T> task, CompletableFuture<T> completableFuture) {
        this.task = task;
        this.completableFuture = completableFuture;
    }
}
