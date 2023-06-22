package com.taobao.arthas.core.command.monitor200.bigkey;

import java.util.List;

public class CommandContext {
    Object[] args;
    long beginTime;
    long endTime;
    long readBeginTime;
    long readEndTime;
    long readElapsedTime;
    boolean readFail;
    long writeBeginTime;
    long writeEndTime;
    long writeElapsedTime;
    boolean writeFail;
    boolean isRead;
    long resultSize;
    boolean readNull;
    Throwable throwable;

    public static long objectSize(Object result){
        long size = 0L;
        if(result == null){
            return size;
        }
        if (result instanceof Long) {
            size = 8L;
        } else if (result instanceof String) {
            size = ((((String)result).getBytes()).length);
        } else if (result instanceof byte[]) {
            size = (((byte[])result).length);
        } else if (result instanceof List) {
            List<?> list = (List<?>)result;
            for (Object item : list) {
                if (item instanceof String) {
                    size += ((String) item).getBytes().length;
                } else if (item instanceof byte[]) {
                    size += ((byte[]) item).length;
                } else {
                    System.out.println(result.getClass().getName());
                }
            }
        } else {
            System.out.println(result.getClass().getName());
        }

        return size;
    }
}
