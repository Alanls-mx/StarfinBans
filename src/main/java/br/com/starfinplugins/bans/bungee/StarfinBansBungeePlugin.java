package br.com.starfinplugins.bans.bungee;

import br.com.starfinplugins.bans.bungee.command.*;
import br.com.starfinplugins.bans.bungee.listener.PunishmentListener;
import br.com.starfinplugins.bans.bungee.storage.MemoryPunishmentStorageBungee;
import br.com.starfinplugins.bans.bungee.storage.MySqlPunishmentStorageBungee;
import br.com.starfinplugins.bans.bungee.storage.SQLitePunishmentStorageBungee;
import br.com.starfinplugins.bans.bungee.util.ConfigUtil;
import br.com.starfinplugins.bans.bungee.util.MessageUtil;
import br.com.starfinplugins.bans.bungee.web.WebIntegrationService;
import br.com.starfinplugins.bans.bungee.license.CoreBungeeAuthorization;
import br.com.starfinplugins.bans.bungee.service.OnlinePunishmentService;
import br.com.starfinplugins.bans.service.PunishmentManager;

import br.com.starfinplugins.bans.bungee.storage.PunishmentStorage;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import net.md_5.bungee.config.Configuration;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class StarfinBansBungeePlugin extends Plugin {

    private static StarfinBansBungeePlugin instance;

    private Configuration config;
    private Configuration messages;

    private PunishmentStorage storage;
    private PunishmentManager punishmentManager;
    private MessageUtil messageUtil;

    private WebIntegrationService webIntegration;
    private OnlinePunishmentService onlinePunishments;

    private boolean lockdown = false;
    private volatile boolean licensed = false;
    private volatile boolean initialized = false;
    private volatile boolean listenersRegistered = false;

    @Override
    public void onEnable() {
        instance = this;

        try {
            ConfigUtil.ensureDefaults(this, "config.yml", "messages.yml");
            this.config = ConfigUtil.loadConfig(this, "config.yml");
            this.messages = ConfigUtil.loadConfig(this, "messages.yml");
            this.messageUtil = new MessageUtil(messages);
            startSecuritySystem();
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "ERRO CRÍTICO: " + e.getMessage(), e);
        }
    }

    private synchronized void initializeAuthorized() {
        if (initialized || !licensed) return;
        try {
            if (storage == null) {
            // Storage
            String type = config.getString("storage-type", "sqlite");
            if ("mysql".equalsIgnoreCase(type)) {
                storage = new MySqlPunishmentStorageBungee(this, config);
            } else if ("sqlite".equalsIgnoreCase(type)) {
                storage = new SQLitePunishmentStorageBungee(this);
            } else if ("memory".equalsIgnoreCase(type)) {
                storage = new MemoryPunishmentStorageBungee();
                getLogger().warning("Armazenamento em memória é volátil e destinado somente a testes.");
            } else {
                throw new IllegalArgumentException("storage-type inválido: " + type
                        + ". Valores aceitos: sqlite, mysql ou memory.");
            }
            storage.init();

            // Web
            webIntegration = new WebIntegrationService(this);

            // Manager
            punishmentManager = new PunishmentManager(this, storage, webIntegration);
            onlinePunishments = new OnlinePunishmentService(this, storage);
            }

            // Listener
            if (!listenersRegistered) {
                ProxyServer.getInstance().getPluginManager().registerListener(this, new PunishmentListener(this));
                listenersRegistered = true;
            }

            // Commands
            registerCommands();
            initialized = true;
            onlinePunishments.start();
            getLogger().info(ChatColor.GREEN + "StarfinBans (Bungee) habilitado com sucesso!");
        } catch (Exception e) {
            licensed = false;
            initialized = false;
            if (storage != null) {
                try { storage.close(); } catch (Exception ignored) { /* original error is logged below */ }
                storage = null;
            }
            getLogger().log(Level.SEVERE, "ERRO CRÍTICO: " + e.getMessage(), e);
        }
    }

    @Override
    public void onDisable() {
        if (onlinePunishments != null) onlinePunishments.stop();
        try {
            if (storage != null) storage.close();
        } catch (Exception error) {
            getLogger().log(Level.WARNING, "Falha ao encerrar o armazenamento do StarfinBans.", error);
        }
        getLogger().info("StarfinBans (Bungee) desabilitado.");
    }

    private void registerCommands() {
        PluginManager pm = ProxyServer.getInstance().getPluginManager();

        pm.registerCommand(this, new BanCommand(this));
        pm.registerCommand(this, new TempBanCommand(this));
        pm.registerCommand(this, new IpBanCommand(this));
        pm.registerCommand(this, new TempIpBanCommand(this));

        pm.registerCommand(this, new MuteCommand(this));
        pm.registerCommand(this, new TempMuteCommand(this));
        pm.registerCommand(this, new IpMuteCommand(this));
        pm.registerCommand(this, new TempIpMuteCommand(this));

        pm.registerCommand(this, new KickCommand(this));
        pm.registerCommand(this, new WarnCommand(this));

        pm.registerCommand(this, new UnbanCommand(this));
        pm.registerCommand(this, new UnmuteCommand(this));
        pm.registerCommand(this, new UnwarnCommand(this));

        pm.registerCommand(this, new HistoryCommand(this));
        pm.registerCommand(this, new CheckBanCommand(this));
        pm.registerCommand(this, new CheckMuteCommand(this));
        pm.registerCommand(this, new DupeIpCommand(this));

        pm.registerCommand(this, new ChangeReasonCommand(this));
        pm.registerCommand(this, new HelpCommand(this));
        pm.registerCommand(this, new AdminCommand(this));
    }

    public void reloadAll() {
        this.config = ConfigUtil.loadConfig(this, "config.yml");
        this.messages = ConfigUtil.loadConfig(this, "messages.yml");
        this.messageUtil = new MessageUtil(messages);
    }

    public static StarfinBansBungeePlugin getInstance() { return instance; }

    public Configuration getCfg() { return config; }
    public MessageUtil msg() { return messageUtil; }

    public PunishmentStorage getStorage() { return storage; }
    public PunishmentManager getPunishmentManager() { return punishmentManager; }
    public WebIntegrationService getWebIntegration() { return webIntegration; }
    public OnlinePunishmentService getOnlinePunishments() { return onlinePunishments; }

    public boolean isLockdown() { return lockdown; }
    public void setLockdown(boolean lockdown) { this.lockdown = lockdown; }
    public boolean isLicensed() { return licensed && initialized && CoreBungeeAuthorization.permitted(); }

    // ---------------- License System ----------------

    private void startSecuritySystem() {
        ProxyServer.getInstance().getScheduler().schedule(this, new Runnable() {
            @Override
            public void run() {
                boolean allowed = CoreBungeeAuthorization.permitted();
                if (allowed) {
                    licensed = true;
                    initializeAuthorized();
                } else if (licensed) {
                    licensed = false;
                    initialized = false;
                    if (onlinePunishments != null) onlinePunishments.stop();
                    ProxyServer.getInstance().getPluginManager().unregisterCommands(StarfinBansBungeePlugin.this);
                    getLogger().warning("StarfinCore nao autoriza starfin_bans; comandos e fiscalizacao bloqueados.");
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
    }
}
