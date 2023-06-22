package com.taobao.arthas.core.command.monitor200.bigkey;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.alibaba.bytekit.asm.binding.Binding;
import com.alibaba.bytekit.asm.interceptor.annotation.AtEnter;
import com.alibaba.bytekit.asm.interceptor.annotation.AtExceptionExit;
import com.alibaba.bytekit.asm.interceptor.annotation.AtExit;

public class JedisMethodInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(JedisMethodInterceptor.class);
    public static BigKeyCommand bigKeyCommand;
    @AtEnter(inline = false)
    public static void atEnter(@Binding.This Object target,
                               @Binding.Args Object[] args,
                               @Binding.MethodName String methodName,
                               @Binding.MethodDesc String methodDesc) {
        CommandContext context = InvocationContext.get();
        if(context == null){
            context = new CommandContext();
            context.beginTime = System.currentTimeMillis();
        }else {
            logger.error("unhoped error happend!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        }

        InvocationContext.set(context);
        InvocationContext.set(Thread.currentThread(),context);
    }

    @AtExit(inline = false)
    public static void atExit(@Binding.This Object target,
                              @Binding.Args Object[] args,
                              @Binding.MethodName String methodName,
                              @Binding.MethodDesc String methodDesc,
                              @Binding.Return Object returnObject) {
        CommandContext context = InvocationContext.get();
        try{
            if(context == null){
                logger.error("unhoped error happend!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                context = InvocationContext.get(Thread.currentThread());
            }
            context.endTime = System.currentTimeMillis();

            long writeSize = 0;
            StringBuilder writeCommand = null;
            if(context.args != null){
                writeCommand = new StringBuilder();
                int length = context.args.length;
                if(length >= 2){
                    writeCommand.append(context.args[1]);
                }
                if(length >= 3){
                    byte[][] cmdArgs = (byte[][])context.args[2];
                    for(byte[] cmdArg : cmdArgs){
                        if(writeSize > 586){
                            break;
                        }
                        writeCommand.append(" ").append(new String(cmdArg));
                        writeSize += CommandContext.objectSize(cmdArg);
                    }
                }
            }

            long allElapsedTime = context.endTime - context.beginTime;
            if(writeSize > bigKeyCommand.getWriteSize() ||
                    allElapsedTime > bigKeyCommand.getElapsed() ||
                    context.resultSize > bigKeyCommand.getReadSize()){
                StringBuilder message = new StringBuilder();
                if(writeCommand != null){
                    message.append(writeCommand).append(",");
                }
                message.append("write size ").append(writeSize).append(" bytes")
                        .append(",read size ").append(context.resultSize).append(" bytes")
                        .append(",writeElapsedTime ").append(context.writeElapsedTime).append(" ms")
                        .append(",readElapsedTime ").append(context.readElapsedTime).append(" ms")
                        .append(",allElapsedTime ").append(allElapsedTime).append(" ms");
                BigKeyException e = new BigKeyException();
                e.setStackTrace(Thread.currentThread().getStackTrace());
                logger.error(message.toString(),e);
            }
        }catch (Throwable t){
            logger.error("",t);
        }finally {
            InvocationContext.remove();
            InvocationContext.remove(Thread.currentThread());
        }
    }

    @AtExceptionExit(inline = false, onException = Throwable.class)
    public static void atExceptionExit(@Binding.This Object target,
                                       @Binding.Args Object[] args,
                                       @Binding.MethodName String methodName,
                                       @Binding.MethodDesc String methodDesc,
                                       @Binding.Throwable Throwable throwable) {
        try{
            InvocationContext.remove();
            InvocationContext.remove(Thread.currentThread());
            logger.error("inner throwable",throwable);
        }catch (Throwable t){
            logger.error("",t);
        }
    }
}
