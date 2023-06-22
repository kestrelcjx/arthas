package com.taobao.arthas.core.command.monitor200.bigkey;

import com.taobao.arthas.core.advisor.AdviceListenerAdapter;
import com.taobao.arthas.core.advisor.ArthasMethod;

class JedisMethodAdviceListener extends AdviceListenerAdapter {
    public JedisMethodAdviceListener() {
    }

    @Override
    public void before(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args)
            throws Throwable {
        JedisMethodInterceptor.atEnter(target,args,method.getName(),method.getMethodDesc());
    }

    @Override
    public void afterReturning(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args,
                               Object returnObject) throws Throwable {
        JedisMethodInterceptor.atExit(target,args,method.getName(),method.getMethodDesc(),returnObject);
    }

    @Override
    public void afterThrowing(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args, Throwable throwable) throws Throwable {
        JedisMethodInterceptor.atExceptionExit(target,args,method.getName(),method.getMethodDesc(),throwable);
    }
}
