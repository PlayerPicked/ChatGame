package com.playerpicked.chatgame.network;

import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public interface NetworkBus {
    void startListening(@NotNull Consumer<NetworkMessage> handler);

    void publish(@NotNull NetworkMessage message);

    void shutdown();
}
