package com.taobao.arthas.core.command.monitor200.blocked;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class CoralTimer {
    private static CoralTimer INSTANCE = new CoralTimer();
    private CoralTimer(){
    }

    public static CoralTimer getInstance(){
        return INSTANCE;
    }
    private AtomicReference<ScheduledExecutor> executor = new AtomicReference<>();

    public static void reset() {
        ScheduledExecutor ex = INSTANCE.executor.getAndSet(null);
        if (ex != null && ex.getThreadPool() != null) {
            ex.getThreadPool().shutdownNow();
        }
    }

    public Reference<TimerListener> addTimerListener(final TimerListener listener) {
        startThreadIfNeeded();
        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    listener.tick();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        int initialDelayInMilliseconds = listener.getInitialDelayInMilliseconds();
        int intervalTimeInMilliseconds = listener.getIntervalTimeInMilliseconds();
        ScheduledFuture<?> f = executor.get()
                .getThreadPool()
                .scheduleAtFixedRate(r, initialDelayInMilliseconds, intervalTimeInMilliseconds, TimeUnit.MILLISECONDS);
        return new TimerReference(listener, f);
    }

    private static class TimerReference extends SoftReference<TimerListener> {
        private final ScheduledFuture<?> f;
        TimerReference(TimerListener referent, ScheduledFuture<?> f) {
            super(referent);
            this.f = f;
        }

        @Override
        public void clear() {
            super.clear();
            f.cancel(false);
        }
    }

    protected void startThreadIfNeeded() {
        while (executor.get() == null || ! executor.get().isInitialized()) {
            if (executor.compareAndSet(null, new ScheduledExecutor())) {
                executor.get().initialize();
            }
        }
    }
    static class ScheduledExecutor {
        volatile ScheduledThreadPoolExecutor executor;
        private volatile boolean initialized;

        /**
         * We want this only done once when created in compareAndSet so use an initialize method
         */
        public void initialize() {
            int coreSize = Runtime.getRuntime().availableProcessors();
            ThreadFactory threadFactory = new ThreadFactory() {
                final AtomicInteger counter = new AtomicInteger();
                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r, "CoralTimer-" + counter.incrementAndGet());
                    thread.setDaemon(true);
                    return thread;
                }
            };

            executor = new ScheduledThreadPoolExecutor(coreSize, threadFactory);
            initialized = true;
        }

        public ScheduledThreadPoolExecutor getThreadPool() {
            return executor;
        }

        public boolean isInitialized() {
            return initialized;
        }
    }
    public interface TimerListener {
        void tick();
        int getIntervalTimeInMilliseconds();
        int getInitialDelayInMilliseconds();
    }
}
