package net.rp.rpessentials;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Executor unique pour toutes les écritures disque asynchrones.
 * Remplace les CompletableFuture.runAsync() épars qui créaient un thread par sauvegarde.
 */
public final class RpEssentialsIO {

    private static volatile ExecutorService EXECUTOR = createExecutor();
    private RpEssentialsIO() {}

    private static ExecutorService createExecutor() {
        return Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "rpessentials-io");
            t.setDaemon(true);
            return t;
        });
    }

    public static void submit(Runnable task) {
        if (EXECUTOR.isShutdown()) {
            EXECUTOR = createExecutor();
        }
        EXECUTOR.submit(task);
    }

    public static void shutdown() {
        EXECUTOR.shutdown();
        try {
            if (!EXECUTOR.awaitTermination(5, TimeUnit.SECONDS))
                EXECUTOR.shutdownNow();
        } catch (InterruptedException e) {
            EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}