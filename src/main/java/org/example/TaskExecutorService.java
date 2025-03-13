package org.example;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskExecutorService implements Main.TaskExecutor {
    private final ExecutorService executorService;
    private final AtomicInteger activeTasks = new AtomicInteger(0);
    private final int concurrency;
    private volatile boolean isShutdown = false;
    private final Map<Main.TaskGroup,GroupTask> map = new ConcurrentHashMap<>();
    private final BlockingQueue<TaskVsFuture<?>> queue = new LinkedBlockingQueue<>();

    public TaskExecutorService(int concurrency) {
        if(concurrency<0){
            throw  new IllegalArgumentException("Illegal concurrency");
        }
        this.executorService = Executors.newFixedThreadPool(concurrency);
        this.concurrency = concurrency;

        Thread mainThread = new Thread(this::startTask);
        mainThread.start();
    }

    private void startTask(){
        while (!isShutdown || !queue.isEmpty()){
            try {
                TaskVsFuture<?> taskVsFuture = queue.take();
                processTask(taskVsFuture);
            }catch (InterruptedException e){
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private <T> void processTask(TaskVsFuture<T> taskVsFuture) {
        Main.Task<T> task = taskVsFuture.task;
        Main.TaskGroup taskGroup = task.taskGroup();

        GroupTask groupTask = map.computeIfAbsent(taskGroup, a -> new GroupTask());
        groupTask.addTask(taskVsFuture);
        scheduleExecution();
    }

    private void scheduleExecution() {
        if(activeTasks.get()>=concurrency){
            return;
        }

        for(GroupTask groupTask: map.values()){
            TaskVsFuture<?> taskVsFuture = groupTask.nextTask();
            if(taskVsFuture!=null && activeTasks.incrementAndGet()<=concurrency){
                executeTask(taskVsFuture,groupTask);
            }else if(taskVsFuture !=null){
                // couldn't execute the task
                groupTask.reOrderTask(taskVsFuture);
                activeTasks.decrementAndGet();
                break;
            }
        }

    }

    private <T> void executeTask(TaskVsFuture<T> taskVsFuture, GroupTask groupTask) {
        Main.Task<T> task = taskVsFuture.task;
        CompletableFuture<T> completableFuture = taskVsFuture.completableFuture;

        executorService.submit(()->{
           try {
               groupTask.setExecute(true);
               T result = task.taskAction().call();
               completableFuture.complete(result);
           }catch (Exception e){
               completableFuture.completeExceptionally(e);
           } finally {
               activeTasks.decrementAndGet();
               groupTask.setExecute(false);

               scheduleExecution();// if more tasks can be scheduled or not
           }
        });
    }

    public void shutdown(){
        isShutdown = true;
        executorService.shutdown();
    }

    @Override
    public <T> Future<T> submitTask(Main.Task<T> task) {
        if(isShutdown){
            throw new RejectedExecutionException("Task Executor is in shut down mode");
        }

        CompletableFuture<T> completableFuture = new CompletableFuture<>();
        TaskVsFuture<T> taskVsFuture = new TaskVsFuture<>(task,completableFuture);

        queue.add(taskVsFuture);
        return  completableFuture;
    }


}
