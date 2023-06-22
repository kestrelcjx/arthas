package com.taobao.arthas.core.command.monitor200.bigkey;

import com.taobao.arthas.core.advisor.AdviceListenerAdapter;
import com.taobao.arthas.core.advisor.ArthasMethod;
import com.taobao.arthas.core.shell.command.CommandProcess;

class BigKeyAdviceListener extends AdviceListenerAdapter {
    private BigKeyCommand command;
    private CommandProcess process;

    public BigKeyAdviceListener(BigKeyCommand command, CommandProcess process, boolean verbose) {
        this.command = command;
        this.process = process;
        super.setVerbose(verbose);
    }

    @Override
    public void before(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args)
            throws Throwable {
        JedisProtocolInterceptor.atEnter(target,args,method.getName(),method.getMethodDesc());
    }

    @Override
    public void afterReturning(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args,
                               Object returnObject) throws Throwable {
        JedisProtocolInterceptor.atExit(target,args,method.getName(),method.getMethodDesc(),returnObject);
    }

    @Override
    public void afterThrowing(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args, Throwable throwable) throws Throwable {
        JedisProtocolInterceptor.atExceptionExit(target,args,method.getName(),method.getMethodDesc(),throwable);
    }
}
