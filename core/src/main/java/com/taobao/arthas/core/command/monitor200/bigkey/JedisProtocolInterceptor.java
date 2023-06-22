package com.taobao.arthas.core.command.monitor200.bigkey;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.alibaba.bytekit.asm.binding.Binding;
import com.alibaba.bytekit.asm.interceptor.annotation.AtEnter;
import com.alibaba.bytekit.asm.interceptor.annotation.AtExceptionExit;
import com.alibaba.bytekit.asm.interceptor.annotation.AtExit;

public class JedisProtocolInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(JedisProtocolInterceptor.class);
    private final static String METHOD_SEND = "sendCommand";
    private final static String METHOD_SEND_DESC = "(Lredis/clients/util/RedisOutputStream;Lredis/clients/jedis/Protocol$Command;[[B)V";
    private final static String METHOD_READ = "read";
    @AtEnter(inline = false)
    public static void atEnter(@Binding.This Object target,
                               @Binding.Args Object[] args,
                               @Binding.MethodName String methodName,
                               @Binding.MethodDesc String methodDesc) {
        try{
            CommandContext context = InvocationContext.get();
            if(context == null){
                return;
            }
            if(METHOD_SEND.equals(methodName) && METHOD_SEND_DESC.equals(methodDesc)){
                context.args = args;
                context.writeBeginTime = System.currentTimeMillis();
            }else if(METHOD_READ.equals(methodName)){
                context.readBeginTime = System.currentTimeMillis();
            }
        }catch (Throwable t){
            logger.error("",t);
        }
    }

    @AtExit(inline = false)
    public static void atExit(@Binding.This Object target,
                              @Binding.Args Object[] args,
                              @Binding.MethodName String methodName,
                              @Binding.MethodDesc String methodDesc,
                              @Binding.Return Object returnObject) {
        try {
            CommandContext context = InvocationContext.get();
            if(context == null){
                return;
            }
            long currentTimeMillis = System.currentTimeMillis();
            if(METHOD_SEND.equals(methodName)  && METHOD_SEND_DESC.equals(methodDesc)){
                context.writeEndTime = currentTimeMillis;
                context.writeElapsedTime += (context.writeEndTime - context.writeBeginTime);
            }else if(METHOD_READ.equals(methodName)) {
                context.readEndTime = currentTimeMillis;
                context.readElapsedTime += (context.readEndTime - context.readBeginTime);
                context.isRead = true;
                context.readNull = (returnObject == null);
                context.resultSize += CommandContext.objectSize(returnObject);
            }
        }catch (Throwable  t){
            logger.error("",t);
        }
    }

    @AtExceptionExit(inline = false, onException = Throwable.class)
    public static void atExceptionExit(@Binding.This Object target,
                                       @Binding.Args Object[] args,
                                       @Binding.MethodName String methodName,
                                       @Binding.MethodDesc String methodDesc,
                                       @Binding.Throwable Throwable throwable) {
        try{
            CommandContext context = InvocationContext.get();
            if(context == null){
                return;
            }
            if(METHOD_SEND.equals(methodName) && METHOD_SEND_DESC.equals(methodDesc)){
                context.writeFail = true;
            }else if(METHOD_READ.equals(methodName)){
                context.readFail = true;
                context.isRead = true;
            }
            context.throwable = throwable;
        }catch (Throwable t){
            logger.error("",t);
        }
    }
}