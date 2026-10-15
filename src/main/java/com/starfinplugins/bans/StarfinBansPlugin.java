// java
package com.starfinplugins.bans;

import com.starfinplugins.bans.api.PunishmentAPI;
import com.starfinplugins.bans.command.*;
import com.starfinplugins.bans.hook.VaultHook;
import com.starfinplugins.bans.listener.PunishmentListener;
import com.starfinplugins.bans.license.CoreAuthorization;
import com.starfinplugins.bans.placeholder.StarfinBansPlaceholderExpansion;
import com.starfinplugins.bans.service.PunishmentManager;
import com.starfinplugins.bans.storage.MemoryPunishmentStorage;
import com.starfinplugins.bans.storage.MySqlPunishmentStorage;
import com.starfinplugins.bans.storage.PunishmentStorage;
import com.starfinplugins.bans.storage.SQLitePunishmentStorage;
import com.starfinplugins.bans.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.Command;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

public class StarfinBansPlugin extends JavaPlugin {

    private static StarfinBansPlugin instance;
    private PunishmentStorage storage;
    private PunishmentManager punishmentManager;
    private MessageUtil messageUtil;
    private VaultHook vaultHook;

    private boolean lockdown = false;
    private volatile boolean licensed = false;
    private volatile boolean initialized;
    private Plugin corePlugin;

    // Configuração personalizada para evitar problemas de encoding e o aviso do console
    private FileConfiguration customConfig = null;
    private File customConfigFile = null;

    // Novo ID produto interno (fixo)
    @Override
    public void onEnable() {
        saveDefaultConfig();
        validateAndLoadMessages();
        registerCommands();
        corePlugin = getServer().getPluginManager().getPlugin("StarfinCore");
        if (corePlugin == null || !corePlugin.isEnabled()) logLicense("license.core-missing", Level.WARNING);
        else logLicense("license.pending", Level.INFO);
        new BukkitRunnable() {
            @Override public void run() {
                if (!isEnabled()) { cancel(); return; }
                boolean allowed = checkLicense();
                if (allowed && !initialized) initializeAuthorized();
                else if (allowed && !licensed) logLicense("license.recovered", Level.INFO);
                else if (!allowed && licensed) logLicense("license.suspended", Level.WARNING);
                licensed = allowed;
            }
        }.runTaskTimer(this, 1L, 20L);
    }

    private void initializeAuthorized() {
        // Se passou na checagem inicial, inicializa o restante (inclui checagem periódica)
        if (!checkLicense()) return;

        instance = this;
        long startTime = System.currentTimeMillis();

        // Banner de Inicialização
        logLicense("license.authorized", Level.INFO);

        try {
            saveDefaultConfig();

            // Validação e Carregamento de Mensagens
            validateAndLoadMessages();

            // Inicializa Storage
            String type = getConfig().getString("storage-type", "sqlite");
            if (type.equalsIgnoreCase("mysql")) {
                storage = new MySqlPunishmentStorage(this);
            } else if (type.equalsIgnoreCase("sqlite")) {
                storage = new SQLitePunishmentStorage(this);
            } else {
                storage = new MemoryPunishmentStorage();
            }

            storage.init();

            punishmentManager = new PunishmentManager(this, storage);
            vaultHook = new VaultHook();
            setupHooks();

            Bukkit.getPluginManager().registerEvents(new PunishmentListener(this), this);

            long timeTaken = System.currentTimeMillis() - startTime;
            initialized = true;
            licensed = true;
            getLogger().info(ChatColor.stripColor(messageUtil.get("license.initialized").replace("%ms%", String.valueOf(timeTaken))));

        } catch (Exception e) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "ERRO CRÍTICO: " + e.getMessage());
            e.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        licensed = false;
        initialized = false;
        if (storage != null) storage.close();
        getLogger().info("STARFINPLUGINS | STARFINBANS: desabilitado.");
    }

    // --- Sobrescrita do Sistema de Configuração para Corrigir Encoding ---

    @Override
    public void reloadConfig() {
        if (customConfigFile == null) {
            customConfigFile = new File(getDataFolder(), "config.yml");
        }
        customConfig = YamlConfiguration.loadConfiguration(customConfigFile);

        // Carrega o config.yml interno com UTF-8 forçado
        try (InputStreamReader defConfigStream = new InputStreamReader(getResource("config.yml"), StandardCharsets.UTF_8)) {
            if (defConfigStream != null) {
                customConfig.setDefaults(YamlConfiguration.loadConfiguration(defConfigStream));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public FileConfiguration getConfig() {
        if (customConfig == null) {
            reloadConfig();
        }
        return customConfig;
    }

    @Override
    public void saveConfig() {
        if (customConfig == null || customConfigFile == null) {
            return;
        }
        try {
            getConfig().save(customConfigFile);
        } catch (IOException ex) {
            getLogger().log(Level.SEVERE, "Could not save config to " + customConfigFile, ex);
        }
    }

    @Override
    public void saveDefaultConfig() {
        if (customConfigFile == null) {
            customConfigFile = new File(getDataFolder(), "config.yml");
        }
        if (!customConfigFile.exists()) {
            saveResource("config.yml", false);
        }
    }

    // ---------------------------------------------------------------------

    public void reloadMessages() {
        validateAndLoadMessages();
    }

    private void validateAndLoadMessages() {
        File file = new File(getDataFolder(), "messages.yml");
        if (!file.exists()) saveResource("messages.yml", false);
        try {
            YamlConfiguration defaults = new YamlConfiguration();
            try (Reader reader = new InputStreamReader(getResource("messages.yml"), StandardCharsets.UTF_8)) {
                defaults.load(reader);
            }
            YamlConfiguration configured = new YamlConfiguration();
            configured.load(file);
            configured.setDefaults(defaults);
            messageUtil = new MessageUtil(configured);
        } catch (Exception error) {
            getLogger().warning("STARFINPLUGINS | STARFINBANS: messages.yml invalido; mensagens anteriores mantidas.");
            if (messageUtil == null) {
                messageUtil = new MessageUtil(YamlConfiguration.loadConfiguration(
                    new InputStreamReader(getResource("messages.yml"), StandardCharsets.UTF_8)));
            }
        }
    }
    private void registerCommands() {
        registerProtected("ban", new BanCommand(this));
        registerProtected("tempban", new TempBanCommand(this));
        registerProtected("ipban", new IpBanCommand(this));
        registerProtected("tempipban", new TempIpBanCommand(this));
        registerProtected("mute", new MuteCommand(this));
        registerProtected("tempmute", new TempMuteCommand(this));
        registerProtected("ipmute", new IpMuteCommand(this));
        registerProtected("tempipmute", new TempIpMuteCommand(this));
        registerProtected("warn", new WarnCommand(this));
        registerProtected("kick", new KickCommand(this));
        registerProtected("unban", new UnbanCommand(this));
        registerProtected("unmute", new UnmuteCommand(this));
        registerProtected("unwarn", new UnwarnCommand(this));
        registerProtected("history", new HistoryCommand(this));
        registerProtected("checkban", new CheckCommand(this, "ban"));
        registerProtected("checkmute", new CheckCommand(this, "mute"));
        registerProtected("dupeip", new DupeIpCommand(this));
        registerProtected("changereason", new ChangeReasonCommand(this));
        registerProtected("starfinbans", new AdminCommand(this));
        registerProtected("help", new HelpCommand(this));
    }

    private void registerProtected(String name, final CommandExecutor delegate) {
        getCommand(name).setExecutor(new CommandExecutor() {
            @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
                if (!isLicensed()) {
                    sender.sendMessage(messageUtil.get("license.blocked"));
                    return true;
                }
                return delegate.onCommand(sender, command, label, args);
            }
        });
    }

    private void setupHooks() {
        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            if (getConfig().getBoolean("vault.use-permissions")) vaultHook.setupPermissions();
        }
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new StarfinBansPlaceholderExpansion(this).register();
        }
    }

    public static StarfinBansPlugin getInstance() { return instance; }
    public PunishmentAPI getPunishmentAPI() { return punishmentManager; }
    public PunishmentStorage getStorage() { return storage; }
    public MessageUtil getMessageUtil() { return messageUtil; }
    public VaultHook getVaultHook() { return vaultHook; }
    public boolean isLockdown() { return lockdown; }
    public void setLockdown(boolean lockdown) { this.lockdown = lockdown; }
    public boolean isLicensed() { return initialized && checkLicense(); }
    private void logLicense(String key, Level level) {
        getLogger().log(level, ChatColor.stripColor(messageUtil.get(key)));
    }

    /**
     * Inicializa os sistemas de segurança.
     * @return true se a verificação inicial passar, false caso contrário.
     */
    private boolean checkLicense() {
        return CoreAuthorization.permitted(corePlugin, this);
    }}
