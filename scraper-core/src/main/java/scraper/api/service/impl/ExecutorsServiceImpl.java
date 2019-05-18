package scraper.api.service.impl;


import org.slf4j.Logger;
import scraper.api.service.ExecutorsService;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ExecutorsServiceImpl implements ExecutorsService {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ExecutorsServiceImpl.class);

    private Map<String, ExecutorService> executorServiceMap = new ConcurrentHashMap<>();

    private boolean warned = false;

    @Override
    public synchronized ExecutorService getService(String group, Integer count) {
        return getService(count, group, true);
    }

    @Override
    public synchronized ExecutorService getService(String group) {
        return getService(100, group, true);
    }


    public synchronized ExecutorService getService(int count, String group, boolean show){
        // one time warnings for l formatting
        if(!warned && count > 999) {
            log.warn("Using more than 999 threads the group {}. Log formatting will be affected.", group);
            warned = true;
        }
        if(!warned && group.length() > 8) {
            log.warn("Thread group name is longer than  8 characters, '{}'. Log formatting will be affected.", group);
            warned = true;
        }

        ExecutorService pool = executorServiceMap.get(group);
        if(pool == null) {
            pool = createExecutorService(count, group, show);
        }

        return pool;
    }

    private synchronized ExecutorService createExecutorService(int count, String group, boolean show) {
        BlockingQueue<Runnable> arrayBlockingQueue = new ArrayBlockingQueue<>(count);
        ThreadPoolExecutor executorService =
                new ThreadPoolExecutor(0, count, 1, TimeUnit.SECONDS, arrayBlockingQueue, new DefaultThreadFactory(group, show));

        // when the blocking queue is full, this tries to put into the queue which blocks
        executorService.setRejectedExecutionHandler((r, executor) -> {
            try {
                // block until there's room
                executor.getQueue().put(r);
            } catch (InterruptedException e) {
                // TODO how to handle interrupted exception
                Thread.currentThread().interrupt();
                throw new RejectedExecutionException("Producer thread interrupted", e);
            }
        });

        executorServiceMap.put(group, executorService);
        return executorService;
    }



    public static class DefaultThreadFactory implements ThreadFactory {
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;
        private final boolean show;

        DefaultThreadFactory(String name, boolean show) {
            this.show = show;
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() :
                    Thread.currentThread().getThreadGroup();

            poolNumber.getAndIncrement();

            namePrefix = name;
        }

        public synchronized Thread newThread(Runnable r) {
            int number = threadNumber.getAndIncrement();

            Thread t;
            if(!show) {
                t = new Thread(group, r,
                        namePrefix,
                        0);
            } else {
                t = new Thread(group, r,
                        namePrefix+ "-" + number,
                        0);
            }

            t.setDaemon(false);
//            if (t.isDaemon())
//                t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);

            return t;
        }
    }
}
