package com.sibanarayan.submission.config.container;

import com.sibanarayan.shared_package.enums.ProgrammingLanguage;
import com.sibanarayan.submission.exceptions.PoolTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

public class LanguagePool {

    private static final Logger log = LoggerFactory.getLogger(LanguagePool.class);

    private final ProgrammingLanguage language;
    private final int poolSize;
    private final int borrowTimeoutSec;
    private final DockerExecutor dockerExecutor;
    private final BlockingQueue<String> idle;
    private final Set<String> all = ConcurrentHashMap.newKeySet();
    private final ExecutorService replacementExecutor;

    public LanguagePool(ProgrammingLanguage language,
                        int poolSize,
                        int borrowTimeoutSec,
                        DockerExecutor dockerExecutor) {
        this.language = language;
        this.poolSize = poolSize;
        this.borrowTimeoutSec = borrowTimeoutSec;
        this.dockerExecutor = dockerExecutor;
        this.idle = new LinkedBlockingQueue<>(poolSize);
        this.replacementExecutor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "pool-replacement-" + language);
            t.setDaemon(true);
            return t;
        });
        warmUp();
    }



    public String borrow() throws PoolTimeoutException, InterruptedException {
        String id = idle.poll(borrowTimeoutSec, TimeUnit.SECONDS);
        if (id == null) {
            throw new PoolTimeoutException(
                    "No" + language + " container available after " + borrowTimeoutSec + "s. " +
                            "Pool size: " + poolSize + ", idle: 0"
            );
        }
        log.debug("Borrowed container {} [{}], idle remaining: {}", id, language, idle.size());
        return id;
    }

    public void returnContainer(String id) {
        replacementExecutor.submit(() -> cleanAndReturn(id));
    }

    public void evictDead() {
        log.debug("Running eviction scan for {} pool ({} idle)", language, idle.size());

        List<String> snapshot = new ArrayList<>();
        idle.drainTo(snapshot);

        for (String id : snapshot) {
            if (isAlive(id)) {
                idle.offer(id);
            } else {
                log.warn("Evicting dead idle container {} [{}]", id, language);
                destroySilently(id);
                all.remove(id);
                spawnReplacement();
            }
        }
    }

    public void shutdown() {
        replacementExecutor.shutdownNow();
        for (String id : all) {
            destroySilently(id);
        }
        all.clear();
        idle.clear();
        log.info("Shut down {} pool ({} containers destroyed)", language, all.size());
    }

    public int idleCount()  { return idle.size(); }

    public int totalCount() { return all.size(); }

    private void warmUp() {
        log.info("Warming up {} containers for [{}]...", poolSize, language);
        for (int i = 0; i < poolSize; i++) {
            try {
                String id = dockerExecutor.startContainer(language);
                idle.offer(id);
                all.add(id);
                log.info("  [{}/{}] started container {}", i + 1, poolSize, id);
            } catch (Exception e) {
                log.error("Failed to warm up container {}/{} for [{}]", i + 1, poolSize, language, e);
            }
        }
        log.info("Pool ready for [{}]: {}/{} containers started", language, idle.size(), poolSize);
    }

    private void cleanAndReturn(String id) {
        try {
            dockerExecutor.cleanWorkDir(id);
        } catch (Exception e) {
            log.warn("Failed to clean container {} — evicting", id, e);
            destroySilently(id);
            all.remove(id);
            spawnReplacement();
            return;
        }

        if (isAlive(id)) {
            idle.offer(id);
            log.debug("Returned container {} [{}], idle now: {}", id, language, idle.size());
        } else {
            log.warn("Container {} [{}] died during use — replacing", id, language);
            destroySilently(id);
            all.remove(id);
            spawnReplacement();
        }
    }

    private void spawnReplacement() {
        replacementExecutor.submit(() -> {
            try {
                log.info("Spawning replacement container for [{}]", language);
                String id = dockerExecutor.startContainer(language);
                all.add(id);
                idle.offer(id);
                log.info("Replacement container {} ready for [{}]", id, language);
            } catch (Exception e) {
                log.error("Failed to spawn replacement for [{}] — pool is now undersized", language, e);
            }
        });
    }

    private boolean isAlive(String id) {
        try {
            ProcessBuilder pb = new ProcessBuilder("docker", "exec", id, "echo", "ok");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            boolean done = p.waitFor(3, TimeUnit.SECONDS);
            return done && p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private void destroySilently(String id) {
        try {
            dockerExecutor.destroy(id);
        } catch (Exception e) {
            log.warn("Failed to destroy container {} — it may be leaking", id, e);
        }
    }
}
