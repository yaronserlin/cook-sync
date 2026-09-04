package com.cooksync_server.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Configuration class defining a bounded thread pool for background work that fans out to many
 * recipients at once (system-announcement broadcasts, and any similarly-shaped future work) —
 * deliberately separate from {@code @EnableAsync}'s implicit default executor
 * ({@link SimpleAsyncTaskExecutor}, unbounded, one new thread per call), which is fine for the
 * app's existing one-off {@code @Async} work (e.g. a single Cloudinary upload/delete in
 * {@code CloudinaryServiceImp}) but would spawn unboundedly many threads if used directly for a
 * loop over every registered device token.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/09/2026
 */
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    /** Threads kept alive even when idle, ready to pick up a broadcast immediately. */
    private static final int CORE_POOL_SIZE = 4;
    /** Hard ceiling on concurrently in-flight broadcast/notification tasks. */
    private static final int MAX_POOL_SIZE = 8;
    /** Backlog of queued tasks allowed once every core/max thread is busy. */
    private static final int QUEUE_CAPACITY = 500;

    /**
     * Builds the bounded executor bean that notification-sending code injects explicitly by type
     * — not picked up implicitly by bare {@code @Async} methods elsewhere (see
     * {@link #getAsyncExecutor()}).
     *
     * @return configured, started ThreadPoolTaskExecutor bean named "notificationExecutor"
     */
    @Bean(name = "notificationExecutor")
    public ThreadPoolTaskExecutor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CORE_POOL_SIZE);
        executor.setMaxPoolSize(MAX_POOL_SIZE);
        executor.setQueueCapacity(QUEUE_CAPACITY);
        executor.setThreadNamePrefix("notif-");
        executor.initialize();
        return executor;
    }

    /**
     * Explicitly preserves the framework's implicit default ({@link SimpleAsyncTaskExecutor}) as
     * the executor for every plain {@code @Async} method elsewhere in the app. Without this
     * override, {@link #notificationExecutor()} being the context's only
     * {@link org.springframework.core.task.TaskExecutor} bean would make Spring silently adopt it
     * as the default for <em>every</em> {@code @Async} method — including
     * {@code CloudinaryServiceImp}'s unrelated image upload/delete calls — rather than only the
     * notification code that injects it explicitly. This keeps that scope intentional.
     *
     * @return a fresh, unbounded SimpleAsyncTaskExecutor, matching pre-existing {@code @Async} behavior
     */
    @Override
    public Executor getAsyncExecutor() {
        return new SimpleAsyncTaskExecutor();
    }
}
