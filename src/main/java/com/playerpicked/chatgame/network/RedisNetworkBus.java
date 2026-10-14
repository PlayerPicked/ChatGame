package com.playerpicked.chatgame.network;

import com.playerpicked.chatgame.config.RedisSettings;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import redis.clients.jedis.ConnectionPoolConfig;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.RedisClient;
import redis.clients.jedis.SslOptions;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class RedisNetworkBus implements NetworkBus {
    private static final Duration RECONNECT_DELAY = Duration.ofSeconds(5);

    private final RedisClient redis;
    private final String channel;
    private final NetworkMessageCodec codec = new NetworkMessageCodec();
    private final ExecutorService publisher = Executors.newSingleThreadExecutor(
            Thread.ofVirtual().name("ChatGame-redis-publisher").factory()
    );
    private final Logger logger;
    private final JedisPubSub subscription = new SubscriptionListener();

    private volatile Consumer<NetworkMessage> messageHandler;
    private volatile Thread subscriberThread;
    private volatile boolean closing;

    public RedisNetworkBus(@NotNull RedisSettings settings, @NotNull Logger logger) {
        this.logger = logger;
        this.channel = settings.channel();
        this.redis = RedisClient.builder()
                .hostAndPort(settings.host(), settings.port())
                .clientConfig(createClientConfig(settings))
                .poolConfig(createPoolConfig(settings))
                .build();
    }

    @Override
    public void startListening(@NotNull Consumer<NetworkMessage> handler) {
        if (subscriberThread != null) throw new IllegalStateException("Already listening");
        messageHandler = handler;
        subscriberThread = Thread.ofVirtual().name("ChatGame-redis-subscriber").start(this::subscribeUntilClosed);
    }

    @Override
    public void publish(@NotNull NetworkMessage message) {
        String json = codec.encode(message);
        // one publisher thread keeps messages in order, e.g. the last hangman letter before the win
        CompletableFuture.runAsync(() -> redis.publish(channel, json), publisher)
                .exceptionally(error -> {
                    logger.warn("Could not publish network message: {}", error.getMessage());
                    return null;
                });
    }

    @Override
    public void shutdown() {
        closing = true;
        publisher.shutdown();
        try {
            if (!publisher.awaitTermination(2, TimeUnit.SECONDS)) publisher.shutdownNow();
        } catch (InterruptedException exception) {
            publisher.shutdownNow();
            Thread.currentThread().interrupt();
        }
        if (subscription.isSubscribed()) subscription.unsubscribe();
        if (subscriberThread != null) subscriberThread.interrupt();
        redis.close();
    }

    private void subscribeUntilClosed() {
        while (!closing) {
            try {
                redis.subscribe(subscription, channel);
            } catch (RuntimeException exception) {
                if (closing) return;
                logger.warn("Lost the Redis subscription, retrying in {}s: {}", RECONNECT_DELAY.toSeconds(), exception.getMessage());
                sleepBeforeReconnect();
            }
        }
    }

    private void sleepBeforeReconnect() {
        try {
            Thread.sleep(RECONNECT_DELAY);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            closing = true;
        }
    }

    private final class SubscriptionListener extends JedisPubSub {
        @Override
        public void onSubscribe(String subscribedChannel, int subscribedChannels) {
            logger.info("Listening for chat game rounds on Redis channel {}", subscribedChannel);
        }

        @Override
        public void onMessage(String receivedChannel, String rawMessage) {
            if (closing) return;

            // handled in arrival order on the subscriber thread: a round must start before its result arrives
            codec.decode(rawMessage).ifPresentOrElse(message -> {
                try {
                    messageHandler.accept(message);
                } catch (RuntimeException exception) {
                    logger.error("Failed to handle network message {}", rawMessage, exception);
                }
            }, () -> logger.warn("Ignoring malformed network message: {}", rawMessage));
        }
    }

    private static @NotNull DefaultJedisClientConfig createClientConfig(@NotNull RedisSettings settings) {
        DefaultJedisClientConfig.Builder config = DefaultJedisClientConfig.builder()
                .clientName("ChatGame")
                .database(settings.database())
                .timeoutMillis(settings.timeoutMillis());
        if (settings.hasPassword()) config.password(settings.password());
        if (settings.ssl()) config.sslOptions(SslOptions.defaults());
        return config.build();
    }

    private static @NotNull ConnectionPoolConfig createPoolConfig(@NotNull RedisSettings settings) {
        ConnectionPoolConfig pool = new ConnectionPoolConfig();
        pool.setMaxTotal(8);
        pool.setMaxIdle(4);
        pool.setMinIdle(1);
        pool.setMaxWait(Duration.ofMillis(settings.timeoutMillis()));
        pool.setJmxEnabled(false);
        return pool;
    }
}
