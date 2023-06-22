package com.taobao.arthas.core.command.monitor200.toomany;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.advisor.AdviceListenerAdapter;
import com.taobao.arthas.core.advisor.ArthasMethod;
import com.taobao.arthas.core.shell.command.CommandProcess;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;

class TooManyResultAdviceListener extends AdviceListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(TooManyResultAdviceListener.class);
    private TooManyResultCommand command;
    private CommandProcess process;
    private static ThreadLocal<Long> HOLDER = new ThreadLocal<>();

    public TooManyResultAdviceListener(TooManyResultCommand command, CommandProcess process, boolean verbose) {
        this.command = command;
        this.process = process;
        super.setVerbose(verbose);
    }

    @Override
    public void before(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args)
            throws Throwable {
        HOLDER.set(System.currentTimeMillis());
    }

    @Override
    public void afterReturning(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args,
                               Object returnObject) throws Throwable {
        try{
            doAtExit(target,args,method.getName());
        }catch (Throwable t){
            logger.error("",t);
        }
    }

    @Override
    public void afterThrowing(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args, Throwable throwable) throws Throwable {

    }

    private void doAtExit(Object target,Object[] args, String methodName) throws SQLException, IllegalAccessException, InvocationTargetException {
        Field resultsField = field(target.getClass(),"results");
        if(resultsField == null){
            logger.warn("{} has no field results",target.getClass().getName());
            return;
        }
        resultsField.setAccessible(true);
        Object obj = resultsField.get(target);
        if(logger.isDebugEnabled()){
            logger.debug("{} {}",target.getClass().getName(),obj.getClass().getName());
        }

        Method getUpdateCount = method(obj.getClass(),"getUpdateCount");
        getUpdateCount.setAccessible(true);
        long updateCount = (long)getUpdateCount.invoke(obj);

        Method getBytesSize = method(obj.getClass(),"getBytesSize");
        getBytesSize.setAccessible(true);
        int byteSize = (int)getBytesSize.invoke(obj);

        long elapsed = System.currentTimeMillis() - HOLDER.get();
        if(updateCount > command.getCount() || byteSize > command.getSize() || elapsed > command.getElapsed()){
            String sql = (args.length >= 1) ? (String) args[0] : "";
            Method asSql = method(target.getClass(),"asSql");
            if(asSql != null){
                asSql.setAccessible(true);
                sql = (String) asSql.invoke(target);
            }

            String message = target.getClass().getName() + "." + methodName +
                    "," + sql +
                    "," + byteSize + " bytes"+
                    ",amount " + updateCount +
                    ",elapsed " + elapsed + " ms";

            TooManyResultException e = new TooManyResultException();
            e.setStackTrace(Thread.currentThread().getStackTrace());
            logger.error(message,e);
        }
    }

    private static Field field(Class<?> clazz,String fieldName){
        if(clazz == null){
            return null;
        }
        try{
            return clazz.getDeclaredField(fieldName);
        }catch (NoSuchFieldException exception){
            return field(clazz.getSuperclass(),fieldName);
        }
    }

    private static Method method(Class<?> clazz, String methodName){
        if(clazz == null){
            return null;
        }
        try{
            return clazz.getDeclaredMethod(methodName);
        } catch (NoSuchMethodException e) {
            return method(clazz.getSuperclass(),methodName);
        }
    }
}
