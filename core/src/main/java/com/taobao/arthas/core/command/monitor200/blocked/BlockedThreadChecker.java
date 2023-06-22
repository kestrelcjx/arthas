package com.taobao.arthas.core.command.monitor200.blocked;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BlockedThreadChecker {
    private static final Logger logger = LoggerFactory.getLogger(BlockedThreadChecker.class);

    private static volatile BlockedThreadChecker instance;
    private final static int DELAY = 10;
    private final static int PERIOD = 1000;
    private final Map<Thread, Task> threads = new ConcurrentHashMap<>();
    private BlockedThreadChecker(){
        logger.info("init BlockedThreadChecker... ...classloader:" + this.getClass().getClassLoader() + ",parent classloader:" + this.getClass().getClassLoader().getParent());
        CoralTimer.getInstance().addTimerListener(new CoralTimer.TimerListener() {
            @Override
            public void tick() {
                long now = System.currentTimeMillis();
                for(Map.Entry<Thread,Task> entry : threads.entrySet()){
                    Thread thread = entry.getKey();
                    Task task = entry.getValue();
                    task.haveExecTime = now - task.startTime;
                    if(task.haveExecTime >= task.maxExecTime){
                        String message = thread.getName() + ":" + thread.getId() + " has been blocked " + task.haveExecTime + " ms for task [" + task.id +"]";
                        BlockedThreadException e = new BlockedThreadException();
                        e.setStackTrace(thread.getStackTrace());
                        logger.error(message,e);
                    }
                }
            }
            @Override
            public int getIntervalTimeInMilliseconds() {
                return PERIOD;
            }
            @Override
            public int getInitialDelayInMilliseconds() {
                return DELAY;
            }
        });
    }
    public static BlockedThreadChecker getInstance(){
        if(instance != null){
            return instance;
        }

        synchronized (BlockedThreadChecker.class){
            if(instance != null){
                return instance;
            }
            instance = new BlockedThreadChecker();
        }
        return instance;
    }
    public void registerThread(Thread thread) {
        registerThread(thread, new Task());
    }
    public void registerThread(Thread thread,Task task) {
        Task pre = threads.put(thread, task);
        if(pre != null){
            logger.error("put unexpected thread {}:{}",thread.getName(),thread.getId());
        }
    }
    public void unregisterThread(Thread thread) {
       Task pre = threads.remove(thread);
       if(pre == null){
           logger.error("remove unexpected thread {}:{}",thread.getName(),thread.getId());
       }
    }
}
