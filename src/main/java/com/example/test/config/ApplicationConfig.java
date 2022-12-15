package com.example.test.config;


import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @Auther: CAI
 * @Date: 2022/11/9 - 11 - 09 - 0:33
 * @Description: com.example.test.config
 * @version: 1.0
 */
@Component
public class ApplicationConfig implements InitializingBean {

    public int maxFlow = 1000;



    private Logger logger = LoggerFactory.getLogger(ApplicationConfig.class);
    public static ApplicationConfig INSTANCE;

    public ApplicationConfig() {
        INSTANCE = this;
    }

    private boolean openLog = false;

    private int batchSize = 200;

    private int threadPoolSize = 32;

    private boolean openBizNameValidate = true;

    private String bizNameString = "oppo,idmmaping,soloop,game";

    private Set<String> bizNameSet = new HashSet<>();
    public int getMaxFlow() {
        return maxFlow;
    }

    public void setMaxFlow(int maxFlow) {
        this.maxFlow = maxFlow;
    }

    public boolean isOpenLog() {
        return openLog;
    }

    public void setOpenLog(boolean openLog) {
        this.openLog = openLog;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    public void setThreadPoolSize(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }

    public boolean isOpenBizNameValidate() {
        return openBizNameValidate;
    }

    public Set<String> getBizNameSet() {
        return bizNameSet;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        init();
    }

    private void init() {
        if (StringUtils.isNotBlank(bizNameString)) {
            Set<String> stringSet = new HashSet<>(Arrays.asList(bizNameString.split("[,;]")));
            bizNameSet = stringSet;
        }
        logger.info("bizNameSet:{}", bizNameSet);
    }
}
