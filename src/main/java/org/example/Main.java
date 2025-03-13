package org.example;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
        public enum TaskType {
            READ,
            WRITE,
        }

        public interface TaskExecutor {
            /**
             * Submit new task to be queued and executed.
             *
             * @param task Task to be executed by the executor. Must not be null.
             * @return Future for the task asynchronous computation result.
             */
            <T> Future<T> submitTask(Task<T> task);
        }

        /**
         * Representation of computation to be performed by the {@link TaskExecutor}.
         *
         * @param taskUUID Unique task identifier.
         * @param taskGroup Task group.
         * @param taskType Task type.
         * @param taskAction Callable representing task computation and returning the result.
         * @param <T> Task computation result value type.
         */
        public record Task<T>(
                UUID taskUUID,
                TaskGroup taskGroup,
                TaskType taskType,
                Callable<T> taskAction
        ) {
            public Task {
                if (taskUUID == null || taskGroup == null || taskType == null || taskAction == null) {
                    throw new IllegalArgumentException("All parameters must not be null");
                }
            }
        }

        /**
         * Task group.
         *
         * @param groupUUID Unique group identifier.
         */
        public record TaskGroup(
                UUID groupUUID
        ) {
            public TaskGroup {
                if (groupUUID == null) {
                    throw new IllegalArgumentException("All parameters must not be null");
                }
            }
        }


        public static void main(String[] args) throws Exception {
            TaskExecutorService taskExecutorService = new TaskExecutorService(4);
            TaskGroup group = new TaskGroup(UUID.randomUUID());
            Task<String> readTask = new Task<>(UUID.randomUUID(),group,TaskType.READ, ()->
            {
                System.out.println("Read Thread"+Thread.currentThread().getName()+"-> is executing");
               Thread.sleep(2000);
               return "Read task completed";
            });

            Task<String> writeTask = new Task<>(UUID.randomUUID(),group,TaskType.WRITE, ()->
            {
                System.out.println("Write Thread"+Thread.currentThread().getName()+"-> is executing");
                Thread.sleep(1000);
                return "Write task completed";
            });

            System.out.println("Submitting Read task");
            Future<String> future1 = taskExecutorService.submitTask(readTask);
            System.out.println("Result loading");
            String result1 = future1.get();
            System.out.println("Read Result ->" +result1);

            System.out.println("Submitting Write task");
            Future<String> future2 = taskExecutorService.submitTask(writeTask);
            System.out.println("Result loading");
            String result2 = future2.get();
            System.out.println("Write Result ->" +result2);
            taskExecutorService.shutdown();

        }
    }