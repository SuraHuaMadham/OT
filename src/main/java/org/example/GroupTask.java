package org.example;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class GroupTask {
    private final Queue<TaskVsFuture<?>> queue = new LinkedList<>();
    private final Lock lock = new ReentrantLock();
    private boolean isExecuting = false;

    public void setExecute(boolean execute){
        lock.lock();
        try{
            isExecuting=execute;
        }finally {
            lock.unlock();
        }
    }

    public <T> void addTask(TaskVsFuture<T> taskVsFuture){
        lock.lock();
        try{
            queue.add(taskVsFuture);
        }finally {
            lock.unlock();
        }
    }

    public <T> void reOrderTask(TaskVsFuture<T> taskVsFuture){
        lock.lock();
        try{
            LinkedList<TaskVsFuture<?>> newQ = new LinkedList<>();
            newQ.add(taskVsFuture);
            newQ.addAll(queue);
            queue.clear();
            queue.addAll(newQ);
        }finally {
            lock.unlock();
        }
    }

    public TaskVsFuture<?> nextTask(){
        lock.lock();
        try{
            if(isExecuting || queue.isEmpty()){
                return null;
            }
            return queue.poll();
        }finally {
            lock.unlock();
        }
    }

}
