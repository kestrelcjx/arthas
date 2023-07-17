package com.taobao.arthas.core.command.monitor200.tomcat;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.advisor.AdviceListenerAdapter;
import com.taobao.arthas.core.advisor.ArthasMethod;
import com.taobao.arthas.core.shell.command.CommandProcess;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

class KeepAliveAdviceListener extends AdviceListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(KeepAliveAdviceListener.class);
    private KeepAliveCommand command;
    private CommandProcess process;

    public KeepAliveAdviceListener(KeepAliveCommand command, CommandProcess process, boolean verbose) {
        this.command = command;
        this.process = process;
        super.setVerbose(verbose);
    }

    @Override
    public void before(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args)
            throws Throwable {
        Class<?> cls = target.getClass();
        try {
            Method toString = cls.getDeclaredMethod("toString");
            String string = (String) toString.invoke(target);

            Field keepAliveLeft = cls.getDeclaredField("keepAliveLeft");
            keepAliveLeft.setAccessible(true);
            int left = (int) keepAliveLeft.get(target);

            Method setKeepAliveLeft = cls.getDeclaredMethod("setKeepAliveLeft",int.class);
            setKeepAliveLeft.invoke(target, 1);

            int afterLeft = (int) keepAliveLeft.get(target);
            logger.info("keepAliveLeft value before {} after {} , socket {}", left, afterLeft, string);
        }catch (Throwable t){
            logger.error("{} {}",args[0],args[1],t);
        }
    }

    @Override
    public void afterReturning(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args,
                               Object returnObject) throws Throwable {

    }

    @Override
    public void afterThrowing(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args,
                              Throwable throwable) {
        logger.info("{} {} {} {}",clazz.getName(),method.getName(),target,args.length);
    }
}
