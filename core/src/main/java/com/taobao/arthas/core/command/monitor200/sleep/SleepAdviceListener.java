package com.taobao.arthas.core.command.monitor200.sleep;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.advisor.AdviceListenerAdapter;
import com.taobao.arthas.core.advisor.ArthasMethod;
import com.taobao.arthas.core.shell.command.CommandProcess;

class SleepAdviceListener extends AdviceListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(SleepAdviceListener.class);

    private SleepCommand command;
    private CommandProcess process;

    public SleepAdviceListener(SleepCommand command, CommandProcess process, boolean verbose) {
        this.command = command;
        this.process = process;
        super.setVerbose(verbose);
    }

    @Override
    public void before(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args)
            throws Throwable {
        try{
            Thread.sleep(command.getMillis());
        }catch (Throwable t){
            logger.error("{} {}",method ,args ,t);
        }
    }

    @Override
    public void afterReturning(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args, Object returnObject) throws Throwable {

    }

    @Override
    public void afterThrowing(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args, Throwable throwable) throws Throwable {

    }
}
