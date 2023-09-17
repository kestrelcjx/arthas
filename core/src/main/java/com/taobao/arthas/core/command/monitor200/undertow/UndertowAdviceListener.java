package com.taobao.arthas.core.command.monitor200.undertow;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.advisor.AdviceListenerAdapter;
import com.taobao.arthas.core.advisor.ArthasMethod;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

class UndertowAdviceListener extends AdviceListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(UndertowAdviceListener.class);
    private UndertowCommand command;
    private CommandProcess process;

    public UndertowAdviceListener(UndertowCommand command, CommandProcess process, boolean verbose) {
        this.command = command;
        this.process = process;
        super.setVerbose(verbose);
    }

    @Override
    public void before(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args)
            throws Throwable {
        try {
            Object exchange = args[0];
            Class exchangeCls = exchange.getClass();
            logger.info("HttpServerExchange {}",exchange);

            Object requestHeaders = MethodUtils.invokeMethod(exchange,"getRequestHeaders");
            logger.info("requestHeaders {}",requestHeaders);

            Object connectionHeader = MethodUtils.invokeMethod(requestHeaders,"getFirst","Connection");
            logger.info("Connection {}",connectionHeader);

            ClassLoader classLoader = exchangeCls.getClassLoader();
            Method addFirst = requestHeaders.getClass().getDeclaredMethod("addFirst", classLoader.loadClass("io.undertow.util.HttpString"),String.class);
            Field field = FieldUtils.getField(classLoader.loadClass("io.undertow.util.Headers"),"CONNECTION");
            addFirst.invoke(requestHeaders, FieldUtils.readStaticField(field),"close");

            Object serverConnection = MethodUtils.invokeMethod(exchange,"getConnection");
            Object peerAddress = MethodUtils.invokeMethod(serverConnection,"getPeerAddress");
            logger.info("RequestHeader Connection value before {} after {},socket peer address {}",
                    connectionHeader, "close",peerAddress);
        }catch (Throwable t){
            logger.error("{}",args[0],t);
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
