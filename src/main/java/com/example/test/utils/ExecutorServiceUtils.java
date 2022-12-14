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
    //线程池
    private ExecutorService executorService;
    private final Logger logger = LoggerFactory.getLogger(ExecutorServiceUtils.class);

    ExecutorServiceUtils() {
        executorService = initThreadPool("id_mapping_", ApplicationConfig.INSTANCE.getThreadPoolSize(), 10000);
    }

    //初始化线程池
    private ExecutorService initThreadPool(String threadName, int threadNum, int queueSize) {
        AtomicInteger i = new AtomicInteger(0);
        return executorService = new ThreadPoolExecutor(threadNum/*线程池容量*/, threadNum/*最大线程数*/, 1000/*线程空闲时长*/,
                TimeUnit.MILLISECONDS/*时间单位*/, new LinkedBlockingDeque<>(queueSize)/*任务队列*/, r -> {
            Thread t = new Thread(r);
            t.setName(threadName + i.getAndIncrement());
            return t;
        }/*线程工厂*/, new CustomRejectedExecutionHandler())/*拒绝策略*/;
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
