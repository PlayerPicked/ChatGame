package com.playerpicked.chatgame;

import com.playerpicked.chatgame.command.CommandRegistrar;
import com.playerpicked.chatgame.command.MainCommand;
import com.playerpicked.chatgame.config.PluginConfig;
import com.playerpicked.chatgame.config.RedisSettings;
import com.playerpicked.chatgame.config.YamlFile;
import com.playerpicked.chatgame.game.AutoRoundStarter;
import com.playerpicked.chatgame.game.ChatAnswerListener;
import com.playerpicked.chatgame.game.GameService;
import com.playerpicked.chatgame.game.RoundAnnouncer;
import com.playerpicked.chatgame.game.RoundFollower;
import com.playerpicked.chatgame.game.RoundOutcomes;
import com.playerpicked.chatgame.game.RoundTracker;
import com.playerpicked.chatgame.game.crafting.CraftingTables;
import com.playerpicked.chatgame.game.puzzle.PuzzleFactory;
import com.playerpicked.chatgame.network.NetworkBus;
import com.playerpicked.chatgame.network.NetworkMessageRouter;
import com.playerpicked.chatgame.network.RedisNetworkBus;
import com.playerpicked.chatgame.network.StandaloneNetworkBus;
import com.playerpicked.chatgame.notification.ChatBroadcaster;
import com.playerpicked.chatgame.notification.SoundPlayer;
import com.playerpicked.chatgame.notification.ToastSender;
import com.playerpicked.chatgame.placeholder.ChatGamePlaceholders;
import com.playerpicked.chatgame.reward.RewardService;
import com.playerpicked.chatgame.scheduler.AsyncExecutor;
import com.playerpicked.chatgame.scheduler.ServerScheduler;
import com.playerpicked.chatgame.stats.StatsCache;
import com.playerpicked.chatgame.stats.StatsService;
import com.playerpicked.chatgame.storage.Database;
import com.playerpicked.chatgame.storage.PlayerStatsRepository;
import com.playerpicked.chatgame.streak.StreakService;
import com.playerpicked.chatgame.text.Messages;
import com.playerpicked.chatgame.update.UpdateChecker;
import com.playerpicked.pickly.paper.PaperMetrics;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import revxrsal.zapper.ZapperJavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class ChatGame extends ZapperJavaPlugin {
    private static final String PICKLY_PLUGIN_ID = "pck_QIhWNsE6Vs09WgLKSBN0EcIx";
    private static final int SPIGOT_RESOURCE_ID = 127146;
    private static final long STATS_REFRESH_TICKS = 30 * 20;

    private final List<AutoCloseable> resources = new ArrayList<>();

    private PluginConfig config;
    private Messages messages;

    @Override
    public void onEnable() {
        Logger logger = getSLF4JLogger();

        try {
            enable(logger);
            logger.info("ChatGame {} enabled", getPluginMeta().getVersion());
        } catch (RuntimeException exception) {
            logger.error("ChatGame could not start and will be disabled", exception);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        for (AutoCloseable resource : resources.reversed()) {
            try {
                resource.close();
            } catch (Exception exception) {
                getSLF4JLogger().warn("Failed to close {} cleanly", resource.getClass().getSimpleName(), exception);
            }
        }
        resources.clear();
    }

    private void enable(@NotNull Logger logger) {
        config = new PluginConfig(YamlFile.loadFromPluginFolder(this, "config.yml"));
        messages = new Messages(YamlFile.loadFromPluginFolder(this, "messages.yml"));

        AsyncExecutor async = new AsyncExecutor();
        ServerScheduler scheduler = new ServerScheduler(this);

        Database database = track(Database.connect(config.database(), getDataFolder().toPath()));
        PlayerStatsRepository statsRepository = new PlayerStatsRepository(database.sql());

        RedisSettings redis = config.redis();
        NetworkBus network = redis.enabled() ? new RedisNetworkBus(redis, logger) : new StandaloneNetworkBus();
        runOnShutdown(network::shutdown);

        ToastSender toasts = new ToastSender(this, scheduler, logger);
        runOnShutdown(toasts::shutdown);
        runOnShutdown(async::shutdown);

        StatsCache statsCache = new StatsCache(statsRepository, async, logger);
        StatsService stats = new StatsService(statsRepository, statsCache, async);
        ChatBroadcaster broadcaster = new ChatBroadcaster(scheduler);
        SoundPlayer sounds = new SoundPlayer(scheduler, logger);
        RewardService rewards = new RewardService(config, scheduler);
        StreakService streaks = new StreakService(config, statsRepository, statsCache, rewards, messages, broadcaster, async, logger);

        PuzzleFactory puzzles = new PuzzleFactory(config, logger);
        RoundTracker rounds = new RoundTracker();
        RoundAnnouncer announcer = new RoundAnnouncer(config, messages, broadcaster, sounds);
        RoundOutcomes outcomes = new RoundOutcomes(config, messages, announcer, stats, rewards, streaks, toasts, sounds, logger);
        GameService games = new GameService(config, puzzles, rounds, announcer, outcomes, network, scheduler, redis);
        RoundFollower follower = new RoundFollower(puzzles, rounds, announcer, outcomes, scheduler, logger);
        CraftingTables craftingTables = new CraftingTables(games, messages, scheduler);
        rounds.onRoundEnd(craftingTables::closeAll);
        runOnShutdown(craftingTables::closeAllNow);
        runOnShutdown(games::cancelCurrentRound);

        registerListeners(new ChatAnswerListener(games), craftingTables, statsCache);
        CommandRegistrar.register(this, config.commandAliases(), messages, new MainCommand(games, craftingTables, messages, this::reloadFiles));

        scheduler.runGlobalTimer(AutoRoundStarter.CHECK_INTERVAL_TICKS, AutoRoundStarter.CHECK_INTERVAL_TICKS,
                new AutoRoundStarter(config, games, rounds));
        scheduler.runGlobalTimer(1, STATS_REFRESH_TICKS, statsCache::refreshEverything);

        registerIntegrations(games, statsCache, scheduler, logger);
        network.startListening(new NetworkMessageRouter(redis.serverId(), games, follower, logger));
        logger.info("Chat games run in {} mode", !redis.enabled() ? "single server" : redis.master() ? "network master" : "network follower");
    }

    private void registerIntegrations(@NotNull GameService games, @NotNull StatsCache statsCache,
                                      @NotNull ServerScheduler scheduler, @NotNull Logger logger) {
        String version = getPluginMeta().getVersion();

        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new ChatGamePlaceholders(version, games, statsCache, messages).register();
        }

        PaperMetrics metrics = new PaperMetrics(this, PICKLY_PLUGIN_ID);
        runOnShutdown(metrics::shutdown);

        UpdateChecker updates = new UpdateChecker(SPIGOT_RESOURCE_ID, version, messages, scheduler, logger);
        registerListeners(updates);
        updates.start();
        runOnShutdown(updates::shutdown);
    }

    private void reloadFiles() {
        config.reload();
        messages.reload();
    }

    private void registerListeners(@NotNull Listener... listeners) {
        for (Listener listener : listeners) getServer().getPluginManager().registerEvents(listener, this);
    }

    private void runOnShutdown(@NotNull Runnable shutdownAction) {
        resources.add(shutdownAction::run);
    }

    private <T extends AutoCloseable> @NotNull T track(@NotNull T resource) {
        resources.add(resource);
        return resource;
    }
}
