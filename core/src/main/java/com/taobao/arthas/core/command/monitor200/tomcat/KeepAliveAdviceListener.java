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
        try {
            Class cls = target.getClass();
            Method toString = method(cls,"toString");
            String str = (String)toString.invoke(target);
            if(str.contains(command.host)){
                Field keepAliveLeft = field(cls,"keepAliveLeft");
                keepAliveLeft.setAccessible(true);
                int left = (int)keepAliveLeft.get(target);

                Method setKeepAliveLeft = method(cls,"setKeepAliveLeft",int.class);
                setKeepAliveLeft.invoke(target,command.getLeft());

                int afterLeft = (int)keepAliveLeft.get(target);
                logger.info("keepAliveLeft value before {} after {} , socket {}", left, afterLeft, str);
            }
        }catch (Throwable t){
            logger.error("{} {}",args[0],args[1],t);
        }
    }

    @Override
    public void afterReturning(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args,
                               Object returnObject) throws Throwable {
        try {
            Class cls = target.getClass();
            Method toString = method(cls,"toString");
            String str = (String)toString.invoke(target);
            if(str.contains(command.host)) {
                Field keepAliveLeft = field(cls, "keepAliveLeft");
                keepAliveLeft.setAccessible(true);
                int left = (int) keepAliveLeft.get(target);
                logger.info("keepAliveLeft value {} , socket {}", left, str);
            }
        }catch (Throwable t){
            logger.error("{} {}",args[0],args[1],t);
        }
    }

    @Override
    public void afterThrowing(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args,
                              Throwable throwable) {
        logger.info("{} {} {} {}",clazz.getName(),method.getName(),target,args.length);
    }

    private static Method method(Class<?> clazz, String methodName,Class<?>... parameterTypes){
        if(clazz == null){
            return null;
        }
        try{
            return clazz.getDeclaredMethod(methodName,parameterTypes);
        } catch (NoSuchMethodException e) {
            return method(clazz.getSuperclass(),methodName);
        }
    }
    private static Field field(Class<?> clazz, String fieldName){
        if(clazz == null){
            return null;
        }
        try{
            return clazz.getDeclaredField(fieldName);
        }catch (NoSuchFieldException exception){
            return field(clazz.getSuperclass(),fieldName);
        }
    }

}
