package com.sibanarayan.submission.config.container;

import com.sibanarayan.shared_package.enums.ProgrammingLanguage;
import com.sibanarayan.submission.exceptions.PoolTimeoutException;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
public class ContainerPoolManager {

    private static final Logger log = LoggerFactory.getLogger(ContainerPoolManager.class);

    private final Map<ProgrammingLanguage, LanguagePool> pools = new EnumMap<>(ProgrammingLanguage.class);

    public ContainerPoolManager(
            DockerExecutor dockerExecutor,
            @Value("${judge.pool.java.size:5}")        int javaPoolSize,
            @Value("${judge.pool.python.size:5}")      int pythonPoolSize,
            @Value("${judge.pool.borrow-timeout-sec:30}") int borrowTimeoutSec) {

        pools.put(ProgrammingLanguage.JAVA,
                new LanguagePool(ProgrammingLanguage.JAVA, javaPoolSize, borrowTimeoutSec, dockerExecutor));

        pools.put(ProgrammingLanguage.PYTHON,
                new LanguagePool(ProgrammingLanguage.PYTHON, pythonPoolSize, borrowTimeoutSec, dockerExecutor));

        log.info("ContainerPoolManager initialized: JAVA={}, PYTHON={}", javaPoolSize, pythonPoolSize);
    }

    public String borrow(ProgrammingLanguage language) throws PoolTimeoutException, InterruptedException {
        LanguagePool pool = getPool(language);
        return pool.borrow();
    }

    public void returnContainer(ProgrammingLanguage language, String containerId) {
        if (containerId == null) return;
        getPool(language).returnContainer(containerId);
    }

    @Scheduled(fixedDelayString = "${judge.pool.eviction-interval-ms:300000}")
    public void evictDead() {
        log.debug("Running scheduled eviction across all pools");
        pools.values().forEach(LanguagePool::evictDead);
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down all container pools...");
        pools.values().forEach(LanguagePool::shutdown);
    }


    public int idleCount(ProgrammingLanguage language) {
        return getPool(language).idleCount();
    }

    public int totalCount(ProgrammingLanguage language) {
        return getPool(language).totalCount();
    }

    private LanguagePool getPool(ProgrammingLanguage language) {
        LanguagePool pool = pools.get(language);
        if (pool == null) {
            throw new IllegalArgumentException("No container pool configured for language: " + language);
        }
        return pool;
    }
}

