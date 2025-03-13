import org.example.Main;
import org.example.TaskExecutorService;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class TaskExecutorServiceTest {
    public static void main(String[] args) throws Exception{
        TaskExecutorService taskExecutorService = new TaskExecutorService(4);

        testReadSanity(taskExecutorService);
        testReadConcurrency(taskExecutorService);
        testWriteSanity(taskExecutorService);
        testWriteConcurrency(taskExecutorService);
        taskExecutorService.shutdown();
    }

    private static void testWriteSanity(TaskExecutorService taskExecutorService) throws Exception{
        Main.TaskGroup group = new Main.TaskGroup(UUID.randomUUID());
        Main.Task<String> task = new Main.Task<>(UUID.randomUUID(),group, Main.TaskType.WRITE,()->{
            Thread.sleep(100);
            return "Task completed";
        });

        Future<String> future = taskExecutorService.submitTask(task);
        String result = future.get(1, TimeUnit.SECONDS);

        if(result.equals("Task completed")){
            System.out.println("Write test case passed");
        }else{
            throw new AssertionError("Write test case failed");
        }
    }


    private static void testReadSanity(TaskExecutorService taskExecutorService) throws Exception{
        Main.TaskGroup group = new Main.TaskGroup(UUID.randomUUID());
        Main.Task<String> task = new Main.Task<>(UUID.randomUUID(),group, Main.TaskType.READ,()->{
            Thread.sleep(100);
            return "Task completed";
        });

        Future<String> future = taskExecutorService.submitTask(task);
        String result = future.get(1, TimeUnit.SECONDS);

        if(result.equals("Task completed")){
            System.out.println("Read test case passed");
        }else{
            throw new AssertionError("Read test case failed");
        }
    }

    private static void testReadConcurrency(TaskExecutorService taskExecutor) throws Exception {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(10);
        List<Future<String>> futures = new CopyOnWriteArrayList<>();


        for (int i = 0; i < 10; i++) {
            final int threadNum = i;
            Thread thread = new Thread(() -> {
                try {
                    startLatch.await();
                    Main.TaskGroup group = new Main.TaskGroup(UUID.randomUUID());
                    Main.Task<String> task = new Main.Task<>(
                            UUID.randomUUID(),
                            group,
                            Main.TaskType.READ,
                            () -> "Task " + threadNum
                    );

                    futures.add(taskExecutor.submitTask(task));
                    doneLatch.countDown();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            thread.start();
        }


        startLatch.countDown();


        doneLatch.await(1, TimeUnit.SECONDS);


        for (Future<String> future : futures) {
            future.get(1, TimeUnit.SECONDS);
        }

        System.out.println("Read Concurrent submission test passed");
    }

    private static void testWriteConcurrency(TaskExecutorService taskExecutor) throws Exception {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(10);
        List<Future<String>> futures = new CopyOnWriteArrayList<>();


        for (int i = 0; i < 10; i++) {
            final int threadNum = i;
            Thread thread = new Thread(() -> {
                try {
                    startLatch.await();
                    Main.TaskGroup group = new Main.TaskGroup(UUID.randomUUID());
                    Main.Task<String> task = new Main.Task<>(
                            UUID.randomUUID(),
                            group,
                            Main.TaskType.WRITE,
                            () -> "Task " + threadNum
                    );

                    futures.add(taskExecutor.submitTask(task));
                    doneLatch.countDown();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            thread.start();
        }


        startLatch.countDown();


        doneLatch.await(1, TimeUnit.SECONDS);


        for (Future<String> future : futures) {
            future.get(1, TimeUnit.SECONDS);
        }

        System.out.println("Write Concurrent submission test passed");
    }

}
