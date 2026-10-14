package com.playerpicked.chatgame.network;

import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public final class StandaloneNetworkBus implements NetworkBus {

    @Override
    public void startListening(@NotNull Consumer<NetworkMessage> handler) { }

    @Override
    public void publish(@NotNull NetworkMessage message) { }

    @Override
    public void shutdown() { }
}
