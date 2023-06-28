package com.taobao.arthas.core.command.monitor200.offline;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.advisor.AdviceListenerAdapter;
import com.taobao.arthas.core.advisor.ArthasMethod;
import com.taobao.arthas.core.shell.command.CommandProcess;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;

class OfflineAdviceListener extends AdviceListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(OfflineAdviceListener.class);
    private OffLineCommand command;
    private CommandProcess process;

    public OfflineAdviceListener(OffLineCommand command, CommandProcess process, boolean verbose) {
        this.command = command;
        this.process = process;
        super.setVerbose(verbose);
    }

    @Override
    public void before(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args)
            throws Throwable {
        //logger.info("{} {} {}",clazz.getName(),method.getName(),target);
        try {
            if(args.length < 2){
                return;
            }
            Method method1 = method(args[0].getClass(),"getRemoteHost");
            method1.setAccessible(true);
            String host = (String) method1.invoke(args[0]);
            if (command.hosts.containsKey(host)) {
                Object response = args[1];
                Class cls = response.getClass();
                Method setHeader = method(cls,"setHeader",String.class,String.class);
                setHeader.invoke(response,"Connection","close");

                logger.info("before remote host is {} ,setHeader Connection close,isClose {}",host,command.isClose());
            }
        }catch (Throwable t){
            logger.error("{} {}",args[0],args[1],t);
        }
    }

    @Override
    public void afterReturning(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args,
                               Object returnObject) throws Throwable {
        //logger.info("{} {} {} {}",clazz.getName(),method.getName(),target,args.length);
        try {
            if(args.length < 2){
                return;
            }
            Method method1 = method(args[0].getClass(),"getRemoteHost");
            method1.setAccessible(true);
            String host = (String) method1.invoke(args[0]);
            if (command.hosts.containsKey(host)) {
                Object response = args[1];
                Class cls = response.getClass();

                Method getHeader = method(cls,"getHeader",String.class);

                Method getHeaderNames = method(cls,"getHeaderNames");
                Collection<String> collection = (Collection<String>)getHeaderNames.invoke(response);

                StringBuilder stringBuilder = new StringBuilder();
                if(collection != null){
                    Iterator<String> iterator = collection.iterator();
                    while (iterator.hasNext()){
                        String header = iterator.next();
                        String value = (String) getHeader.invoke(response,header);
                        if(stringBuilder.length() == 0){
                            stringBuilder.append(header).append(":").append(value);
                        }else {
                            stringBuilder.append(",").append(header).append(":").append(value);
                        }
                    }
                }

                if(command.isClose()){
                    Method getWriter = method(cls,"getWriter");
                    Object printWriter = getWriter.invoke(response);

                    Method close = method(printWriter.getClass(),"close");
                    close.invoke(printWriter);
                }
                logger.info("after remote host is {} ,setHeader Connection close,isClose {},headers {}",
                        host,command.isClose(),stringBuilder.toString());
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
}
