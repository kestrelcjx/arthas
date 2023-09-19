package com.taobao.arthas.core.command.monitor200.jetty;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.advisor.AdviceListenerAdapter;
import com.taobao.arthas.core.advisor.ArthasMethod;
import com.taobao.arthas.core.shell.command.CommandProcess;

import java.lang.reflect.Field;

class JettyAdviceListener extends AdviceListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(JettyAdviceListener.class);
    private JettyCommand command;
    private CommandProcess process;

    public JettyAdviceListener(JettyCommand command, CommandProcess process, boolean verbose) {
        this.command = command;
        this.process = process;
        super.setVerbose(verbose);
    }

    @Override
    public void before(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args) throws Throwable {
        try {
            Field field = target.getClass().getDeclaredField("_persistent");
            field.setAccessible(true);
            Boolean _persistent = (Boolean) field.get(target);

            field.set(target,false);
            logger.info("_persistent value before {} after {}", _persistent, "false");
        }catch (Throwable t){
            logger.error("{}",target,t);
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
