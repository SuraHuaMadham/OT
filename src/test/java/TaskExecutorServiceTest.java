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
        MultiGroupTaskExecutorTest();
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

     private static void MultiGroupTaskExecutorTest() throws Exception {
        TaskExecutorService executor = TaskExecutorService.create(3);
        
       
        Main.TaskGroup readGroup = new Main.TaskGroup(UUID.randomUUID());
        Main.TaskGroup writeGroup = new Main.TaskGroup(UUID.randomUUID());
        
        
        AtomicInteger sharedCounter = new AtomicInteger(0);
        
        
        List<Future<String>> futures = new ArrayList<>();
        
        
        for (int i = 0; i < 2; i++) {
            final int taskId = i;
            Main.Task<String> readTask = new Main.Task<>(
                UUID.randomUUID(),
                readGroup,
                Main.TaskType.READ,
                () -> {
                    System.out.println("Read Task " + taskId + " - Current Counter: " + sharedCounter.get());
                    Thread.sleep(300);
                    return "Read Task " + taskId + " Result";
                }
            );
            futures.add(executor.submitTask(readTask));
        }
        
        
        for (int i = 0; i < 2; i++) {
            final int taskId = i;
            Main.Task<String> writeTask = new Main.Task<>(
                UUID.randomUUID(),
                writeGroup,
                Main.TaskType.WRITE,
                () -> {
                    int newValue = sharedCounter.incrementAndGet();
                    System.out.println("Write Task " + taskId + " - Updated Counter: " + newValue);
                    Thread.sleep(400);
                    return "Write Task " + taskId + " Result";
                }
            );
            futures.add(executor.submitTask(writeTask));
        }
        
       
        System.out.println("\nRetrieving Results:");
        for (Future<String> future : futures) {
            try {
                String result = future.get(2, TimeUnit.SECONDS);
                System.out.println("Result: " + result);
            } catch (Exception e) {
                System.err.println("Task execution failed: " + e.getMessage());
            }
        }
        
        
        System.out.println("\nFinal Counter Value: " + sharedCounter.get());
        
        
        executor.shutdown();
    }

}
