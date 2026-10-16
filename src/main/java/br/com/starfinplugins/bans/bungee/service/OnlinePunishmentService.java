package br.com.starfinplugins.bans.bungee.service;

import br.com.starfinplugins.bans.bungee.StarfinBansBungeePlugin;
import br.com.starfinplugins.bans.bungee.model.Punishment;
import br.com.starfinplugins.bans.bungee.storage.PunishmentStorage;
import br.com.starfinplugins.bans.bungee.util.TextUtil;
import br.com.starfinplugins.bans.bungee.util.TimeUtil;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.scheduler.ScheduledTask;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/** Keeps database access off the Bungee chat event and reconciles connected players. */
public final class OnlinePunishmentService {
    private static final long MAX_AGE_MS = 10_000L;
    private final StarfinBansBungeePlugin plugin;
    private final PunishmentStorage storage;
    private final Map<UUID, Snapshot> snapshots = new ConcurrentHashMap<>();
    private final Set<UUID> disconnecting = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean refreshing = new AtomicBoolean();
    private volatile ScheduledTask task;

    public OnlinePunishmentService(StarfinBansBungeePlugin plugin, PunishmentStorage storage) {
        this.plugin = plugin;
        this.storage = storage;
    }

    public synchronized void start() {
        if (task != null) return;
        int seconds = Math.max(1, plugin.getCfg().getInt("enforcement.refresh-seconds", 2));
        task = plugin.getProxy().getScheduler().schedule(plugin, this::refreshOnline,
                seconds, seconds, TimeUnit.SECONDS);
    }

    public synchronized void stop() {
        if (task != null) task.cancel();
        task = null;
        snapshots.clear();
        disconnecting.clear();
    }

    /** Called from Bungee's asynchronous login intent after the ban check succeeds. */
    public void prime(UUID uuid, String name, String ip) {
        if (uuid == null) return;
        snapshots.put(uuid, new Snapshot(findMute(uuid, name, ip), System.currentTimeMillis()));
    }

    public void forget(UUID uuid) {
        if (uuid == null) return;
        snapshots.remove(uuid);
        disconnecting.remove(uuid);
    }

    /** Null means the mute state is unknown or stale, so chat must fail closed. */
    public Snapshot snapshot(UUID uuid) {
        Snapshot snapshot = snapshots.get(uuid);
        if (snapshot == null || System.currentTimeMillis() - snapshot.checkedAt > MAX_AGE_MS) return null;
        return snapshot;
    }

    private void refreshOnline() {
        if (!plugin.isLicensed() || !refreshing.compareAndSet(false, true)) return;
        try {
            for (ProxiedPlayer player : plugin.getProxy().getPlayers()) {
                if (!plugin.isLicensed()) break;
                UUID uuid = player.getUniqueId();
                if (uuid == null) continue;
                String name = player.getName();
                String ip = player.getAddress().getAddress().getHostAddress();
                try {
                    Punishment ban = findBan(uuid, name, ip);
                    if (ban != null && disconnecting.add(uuid)) {
                        player.disconnect(TextUtil.tc(banMessage(ban)));
                        continue;
                    }
                    Punishment mute = findMute(uuid, name, ip);
                    snapshots.put(uuid, new Snapshot(mute, System.currentTimeMillis()));
                } catch (RuntimeException error) {
                    snapshots.remove(uuid);
                    plugin.getLogger().log(Level.WARNING,
                            "Falha ao reconciliar punicoes do jogador " + name + "; chat bloqueado ate recuperar o banco.", error);
                }
            }
        } finally {
            refreshing.set(false);
        }
    }

    private Punishment findBan(UUID uuid, String name, String ip) {
        Punishment ban = storage.getActiveBan(uuid);
        if (ban == null) ban = storage.getActiveBanByName(name);
        if (ban == null && ip != null) ban = storage.getActiveIpBan(ip);
        return ban != null && ban.isExpired() ? null : ban;
    }

    private Punishment findMute(UUID uuid, String name, String ip) {
        Punishment mute = storage.getActiveMute(uuid);
        if (mute == null) mute = storage.getActiveMuteByName(name);
        if (mute == null && ip != null) mute = storage.getActiveIpMute(ip);
        return mute != null && mute.isExpired() ? null : mute;
    }

    private String banMessage(Punishment ban) {
        String layout = plugin.msg().getLayout("layouts.ban");
        if (layout == null || layout.isEmpty()) layout = "&cVoce esta banido. Motivo: %reason%";
        String duration = ban.getExpires() > 0
                ? TimeUtil.formatDuration(Math.max(0L, ban.getExpires() - System.currentTimeMillis()))
                : "Permanente";
        return layout.replace("%reason%", ban.getReason() == null ? "Sem motivo" : ban.getReason())
                .replace("%staff%", ban.getStaffName() == null ? "Console" : ban.getStaffName())
                .replace("%duration%", duration)
                .replace("%expires%", duration);
    }

    public static final class Snapshot {
        private final Punishment mute;
        private final long checkedAt;

        private Snapshot(Punishment mute, long checkedAt) {
            this.mute = mute;
            this.checkedAt = checkedAt;
        }

        public Punishment getMute() { return mute; }
    }
}
