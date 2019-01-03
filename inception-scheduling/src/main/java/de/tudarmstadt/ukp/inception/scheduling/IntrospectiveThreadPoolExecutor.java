package de.tudarmstadt.ukp.inception.scheduling;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;

public class IntrospectiveThreadPoolExecutor
        extends ThreadPoolExecutor
{
    private final BiConsumer<Thread, Runnable> beforeExecuteCallback;
    private final BiConsumer<Runnable, Throwable> afterExecuteCallback;

    public IntrospectiveThreadPoolExecutor(int aNumberOfThreads,
                                           int queueSize,
                                           BiConsumer<Thread, Runnable> aBeforeExecuteCallback,
                                           BiConsumer<Runnable, Throwable> aAfterExecuteCallback)
    {
        super(aNumberOfThreads, aNumberOfThreads, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(queueSize), buildThreadFactory());

        beforeExecuteCallback = aBeforeExecuteCallback;
        afterExecuteCallback = aAfterExecuteCallback;
    }

    @Override
    protected void beforeExecute(Thread aThread, Runnable aRunnable)
    {
        super.beforeExecute(aThread, aRunnable);

        beforeExecuteCallback.accept(aThread, aRunnable);
    }

    @Override
    protected void afterExecute(Runnable aRunnable, Throwable aThrowable)
    {
        super.afterExecute(aRunnable, aThrowable);

        afterExecuteCallback.accept(aRunnable, aThrowable);
    }

    private static ThreadFactory buildThreadFactory()
    {
        return new BasicThreadFactory.Builder()
                .daemon(true)
                .priority(Thread.MIN_PRIORITY)
                .build();
    }
}
