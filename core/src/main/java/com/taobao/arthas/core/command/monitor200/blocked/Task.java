package com.taobao.arthas.core.command.monitor200.blocked;

import java.util.concurrent.atomic.AtomicLong;

public class Task {
    static AtomicLong SEQ = new AtomicLong(0);
    static long DEFAULT_MAX_EXEC_TIME = 1000L;
    long id = SEQ.addAndGet(1L);
    long startTime = System.currentTimeMillis();
    long maxExecTime = DEFAULT_MAX_EXEC_TIME;
    long haveExecTime;
    Thread thread;
    Object target;
    Object[] args;
}
