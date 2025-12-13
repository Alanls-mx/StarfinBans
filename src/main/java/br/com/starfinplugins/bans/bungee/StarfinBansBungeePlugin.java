package br.com.starfinplugins.bans.bungee;

import br.com.starfinplugins.bans.bungee.command.*;
import br.com.starfinplugins.bans.bungee.listener.PunishmentListener;
import br.com.starfinplugins.bans.bungee.storage.MemoryPunishmentStorageBungee;
import br.com.starfinplugins.bans.bungee.storage.MySqlPunishmentStorageBungee;
import br.com.starfinplugins.bans.bungee.storage.SQLitePunishmentStorageBungee;
import br.com.starfinplugins.bans.bungee.util.ConfigUtil;
import br.com.starfinplugins.bans.bungee.util.MessageUtil;
import br.com.starfinplugins.bans.bungee.web.WebIntegrationService;
import br.com.starfinplugins.bans.bungee.service.PunishmentManager;

// ✅ IMPORT DO CORE (não do bungee)
import br.com.starfinplugins.bans.bungee.storage.PunishmentStorage;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
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

    private boolean lockdown = false;
    private boolean licensed = false;

    private static final String STARFIN_PRODUCT_ID = "starfin_bans";
    private static final String STARFIN_KEY = "sf_9A7dF2KXQm8LrT6WZC4H1JY0bE5pVNUaS3xMOkcRDiGqPwfheI";

    @Override
    public void onEnable() {
        instance = this;

        try {
            ConfigUtil.ensureDefaults(this, "config.yml", "messages.yml");
            this.config = ConfigUtil.loadConfig(this, "config.yml");
            this.messages = ConfigUtil.loadConfig(this, "messages.yml");
            this.messageUtil = new MessageUtil(messages);

            if (!startSecuritySystem()) {
                getLogger().severe("Falha na verificação do StarfinLicense. Desabilitando StarfinBans.");
                return;
            }

            sendStartupBanner();

            // Storage
            String type = config.getString("storage-type", "sqlite");
            if ("mysql".equalsIgnoreCase(type)) {
                storage = new MySqlPunishmentStorageBungee(this, config);
            } else if ("sqlite".equalsIgnoreCase(type)) {
                storage = new SQLitePunishmentStorageBungee(this);
            } else {
                storage = new MemoryPunishmentStorageBungee();
            }
            if (!storage.init()) {
                getLogger().severe("Falha ao iniciar o armazenamento. Desabilitando StarfinBans.");
                return;
            }

            // Web
            webIntegration = new WebIntegrationService(this);

            // Manager
            punishmentManager = new PunishmentManager(this, storage, webIntegration);

            // Listener
            ProxyServer.getInstance().getPluginManager().registerListener(this, new PunishmentListener(this));

            // Commands
            registerCommands();

            getLogger().info(ChatColor.GREEN + "StarfinBans (Bungee) habilitado com sucesso!");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "ERRO CRÍTICO: " + e.getMessage(), e);
        }
    }

    @Override
    public void onDisable() {
        try {
            if (storage != null) storage.close();
        } catch (Exception ignored) {}
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

    public boolean isLockdown() { return lockdown; }
    public void setLockdown(boolean lockdown) { this.lockdown = lockdown; }
    public boolean isLicensed() { return licensed; }

    private void sendStartupBanner() {
        String[] lines = new String[] {
                ChatColor.YELLOW + "=============================================",
                ChatColor.GOLD + "           StarfinBans v" + getDescription().getVersion(),
                ChatColor.YELLOW + "      Desenvolvido por StarfinPlugins",
                ChatColor.YELLOW + "      StarfinBans versão Bungee licenciada e autenticada.",
                ChatColor.YELLOW + "      Acesse nosso site: https://starfinplugins.com",
                ChatColor.YELLOW + "============================================="
        };

        for (String line : lines) {
            ProxyServer.getInstance().getConsole().sendMessage(new TextComponent(line));
        }
    }

    // ---------------- License System ----------------

    private boolean startSecuritySystem() {
        if (!checkLicense()) return false;

        ProxyServer.getInstance().getScheduler().schedule(this, new Runnable() {
            @Override
            public void run() {
                if (!checkLicense()) {
                    getLogger().severe("[StarfinBans] Violação de segurança detectada (Runtime Check).");

                    ProxyServer.getInstance().getPluginManager().unregisterCommands(StarfinBansBungeePlugin.this);
                    ProxyServer.getInstance().getPluginManager().unregisterListeners(StarfinBansBungeePlugin.this);

                    try { if (storage != null) storage.close(); } catch (Exception ignored) {}
                }
            }
        }, 5, 5, TimeUnit.MINUTES);

        return true;
    }

    private boolean checkLicense() {
        net.md_5.bungee.api.plugin.Plugin licensePlugin =
                ProxyServer.getInstance().getPluginManager().getPlugin("StarfinLicense");
        if (licensePlugin == null) {
            licensed = false;
            getLogger().severe("[StarfinBans] StarfinLicense não encontrado.");
            return false;
        }

        String author = licensePlugin.getDescription().getAuthor();
        if (author == null || !author.toLowerCase().contains("starfinplugins")) {
            licensed = false;
            getLogger().severe("[StarfinBans] StarfinLicense inválido (Author Spoofing). Encontrado: " + author);
            return false;
        }

        try {
            java.lang.reflect.Method[] methods = licensePlugin.getClass().getMethods();
            java.lang.reflect.Method m2 = null;
            java.lang.reflect.Method m1 = null;

            // ✅ SEM var (Java 8)
            for (java.lang.reflect.Method m : methods) {
                if (!"canStart".equals(m.getName())) continue;
                if (m.getParameterCount() == 2 && m.getParameterTypes()[1] == String.class) m2 = m;
                if (m.getParameterCount() == 1) m1 = m;
            }

            if (m2 != null) {
                Object res = m2.invoke(licensePlugin, this, STARFIN_PRODUCT_ID);
                if (!(res instanceof Boolean) || !((Boolean) res)) {
                    licensed = false;
                    getLogger().severe("[StarfinBans] StarfinLicense recusou autorização (canStart Plugin,String).");
                    return false;
                }
                licensed = true;
                return true;
            }

            if (m1 != null) {
                Object res = m1.invoke(licensePlugin, this);
                if (!(res instanceof Boolean) || !((Boolean) res)) {
                    licensed = false;
                    getLogger().severe("[StarfinBans] StarfinLicense recusou autorização (canStart Plugin).");
                    return false;
                }
                licensed = true;
                return true;
            }

            licensed = false;
            getLogger().severe("[StarfinBans] StarfinLicense incompatível: método canStart não encontrado.");
            return false;

        } catch (Exception e) {
            licensed = false;
            getLogger().severe("[StarfinBans] Erro ao verificar licença: " + e.getMessage());
            return false;
        }
    }
}
