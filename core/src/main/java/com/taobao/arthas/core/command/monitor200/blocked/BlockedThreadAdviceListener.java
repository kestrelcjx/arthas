package com.taobao.arthas.core.command.monitor200.blocked;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.advisor.AdviceListenerAdapter;
import com.taobao.arthas.core.advisor.ArthasMethod;
import com.taobao.arthas.core.shell.command.CommandProcess;

class BlockedThreadAdviceListener extends AdviceListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(BlockedThreadAdviceListener.class);
    private BlockedThreadCommand command;
    private CommandProcess process;

    public BlockedThreadAdviceListener(BlockedThreadCommand command, CommandProcess process, boolean verbose) {
        this.command = command;
        this.process = process;
        super.setVerbose(verbose);
    }

    @Override
    public void before(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args)
            throws Throwable {
        try{
            Thread thread = Thread.currentThread();
            Task task = new Task();
            task.thread = thread;
            task.target = target;
            task.args = args;
            task.maxExecTime = this.command.getMaxExeTime();
            BlockedThreadChecker.getInstance().registerThread(thread,task);
        }catch (Throwable t){
            logger.error("before",t);
        }
    }

    @Override
    public void afterReturning(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args,
                               Object returnObject) throws Throwable {
        try {
            BlockedThreadChecker.getInstance().unregisterThread(Thread.currentThread());
        }catch (Throwable t){
            logger.error("afterReturning",t);
        }
    }

    @Override
    public void afterThrowing(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args,
                              Throwable throwable) {
        try {
            BlockedThreadChecker.getInstance().unregisterThread(Thread.currentThread());
        }catch (Throwable t){
            logger.error("afterThrowing",t);
        }
    }
}
