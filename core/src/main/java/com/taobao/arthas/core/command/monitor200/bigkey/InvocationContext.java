package com.taobao.arthas.core.command.monitor200.bigkey;

import java.util.concurrent.ConcurrentHashMap;

public class InvocationContext {
    private final static ThreadLocal<CommandContext> HOLDER = new ThreadLocal<>();
    private final static ConcurrentHashMap<Long,CommandContext> map = new ConcurrentHashMap<>();
    public static CommandContext get(){
        return HOLDER.get();
    }

    public static void set(CommandContext context){
        HOLDER.set(context);
    }

    public static void remove(){
        HOLDER.remove();
    }

    public static CommandContext get(Thread thread){
        return map.get(thread.getId());
    }

    public static void set(Thread thread,CommandContext context){
        map.put(thread.getId(),context);
    }

    public static void remove(Thread thread){
        map.remove(thread.getId());
    }
}
