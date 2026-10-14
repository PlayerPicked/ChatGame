package com.playerpicked.chatgame.network;

import com.playerpicked.chatgame.game.GameService;
import com.playerpicked.chatgame.game.RoundFollower;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class NetworkMessageRouter implements Consumer<NetworkMessage> {
    private final String ownServerId;
    private final GameService games;
    private final RoundFollower follower;
    private final Logger logger;
    private final AtomicBoolean warnedAboutSecondMaster = new AtomicBoolean();

    public NetworkMessageRouter(@NotNull String ownServerId, @NotNull GameService games, @NotNull RoundFollower follower,
                                @NotNull Logger logger) {
        this.ownServerId = ownServerId;
        this.games = games;
        this.follower = follower;
        this.logger = logger;
    }

    @Override
    public void accept(@NotNull NetworkMessage message) {
        if (message.sourceServerId().equals(ownServerId)) return;

        if (message instanceof NetworkMessage.AnswerSubmitted submitted) {
            games.acceptRemoteAnswer(submitted);
            return;
        }

        if (games.isAuthority()) {
            if (warnedAboutSecondMaster.compareAndSet(false, true)) {
                logger.warn("Server {} also runs chat game rounds; only one server should use redis.server-role: master",
                        message.sourceServerId());
            }
            return;
        }

        switch (message) {
            case NetworkMessage.RoundStarted started -> follower.followStart(started);
            case NetworkMessage.RoundProgressed progressed -> follower.followProgress(progressed);
            case NetworkMessage.RoundWon won -> follower.followWin(won);
            case NetworkMessage.RoundExpired expired -> follower.followExpiry(expired);
            case NetworkMessage.RoundCancelled cancelled -> follower.followCancel(cancelled);
            case NetworkMessage.AnswerSubmitted ignored -> { }
        }
    }
}
