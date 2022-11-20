package com.example.test.utils;

import com.example.test.config.ApplicationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Auther: CAI
 * @Date: 2022/11/9 - 11 - 09 - 1:04
 * @Description: com.example.test.utils
 * @version: 1.0
 */
public enum ExecutorServiceUtils {

    INSTANCE;
    private ExecutorService executorService;
    private final Logger logger = LoggerFactory.getLogger(ExecutorServiceUtils.class);

    ExecutorServiceUtils() {
        executorService = initThreadPool("id_mapping_", ApplicationConfig.INSTANCE.getThreadPoolSize(), 10000);
    }

    private ExecutorService initThreadPool(String threadName, int threadNum, int queueSize) {
        AtomicInteger i = new AtomicInteger(0);
        return executorService = new ThreadPoolExecutor(threadNum, threadNum, 1000,
                TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>(queueSize), r -> {
            Thread t = new Thread(r);
            t.setName(threadName + i.getAndIncrement());
            return t;
        }, new CustomRejectedExecutionHandler());
    }

    private class CustomRejectedExecutionHandler implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            try {
                executor.getQueue().put(r);
            } catch (InterruptedException e) {
                logger.error("put take toq queue error", e);
            }

        }
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }
}
