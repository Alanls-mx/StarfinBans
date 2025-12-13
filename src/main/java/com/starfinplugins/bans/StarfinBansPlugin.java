// java
package com.starfinplugins.bans;

import com.starfinplugins.bans.api.PunishmentAPI;
import com.starfinplugins.bans.command.*;
import com.starfinplugins.bans.hook.VaultHook;
import com.starfinplugins.bans.listener.PunishmentListener;
import com.starfinplugins.bans.placeholder.StarfinBansPlaceholderExpansion;
import com.starfinplugins.bans.service.PunishmentManager;
import com.starfinplugins.bans.storage.MemoryPunishmentStorage;
import com.starfinplugins.bans.storage.MySqlPunishmentStorage;
import com.starfinplugins.bans.storage.PunishmentStorage;
import com.starfinplugins.bans.storage.SQLitePunishmentStorage;
import com.starfinplugins.bans.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
import java.nio.file.StandardCopyOption;
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
    private boolean licensed = false;

    // Configuração personalizada para evitar problemas de encoding e o aviso do console
    private FileConfiguration customConfig = null;
    private File customConfigFile = null;

    // Essa variável é a chave que o StarfinLicense vai procurar (mantida)
    private static final String STARFIN_KEY = "sf_9A7dF2KXQm8LrT6WZC4H1JY0bE5pVNUaS3xMOkcRDiGqPwfheI";

    // Novo ID produto interno (fixo)
    private static final String STARFIN_PRODUCT_ID = "starfin_bans";

    @Override
    public void onEnable() {
        // Bloqueio imediato no onEnable se o core não existir/estiver desativado ou recusar a autorização
        Plugin core = getServer().getPluginManager().getPlugin("StarfinLicense");
        if (core == null || !core.isEnabled()) {
            getLogger().log(Level.SEVERE, "StarfinLicense não encontrado ou desativado. Desabilitando plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Tenta chamar canStart(this, STARFIN_PRODUCT_ID) por reflexão
        try {
            java.lang.reflect.Method targetMethod = null;
            for (java.lang.reflect.Method m : core.getClass().getMethods()) {
                if (m.getName().equals("canStart") && m.getParameterCount() == 2
                        && m.getParameterTypes()[1] == String.class) {
                    targetMethod = m;
                    break;
                }
            }

            if (targetMethod == null) {
                getLogger().log(Level.SEVERE, "StarfinLicense incompatível: método canStart(Plugin, String) não encontrado. Desabilitando plugin.");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }

            Object result = targetMethod.invoke(core, this, STARFIN_PRODUCT_ID);
            if (!(result instanceof Boolean)) {
                getLogger().log(Level.SEVERE, "StarfinLicense.canStart retornou valor inválido. Desabilitando plugin.");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }

            boolean canStart = (Boolean) result;
            if (!canStart) {
                getLogger().log(Level.SEVERE, "Licença inválida ou não autorizada pelo StarfinLicense. Desabilitando plugin.");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, "Erro ao chamar StarfinLicense.canStart: " + ex.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Se passou na checagem inicial, inicializa o restante (inclui checagem periódica)
        if (!startSecuritySystem()) {
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        instance = this;
        long startTime = System.currentTimeMillis();

        // Banner de Inicialização
        Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW + "=============================================");
        Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "           StarfinBans v" + getDescription().getVersion());
        Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW + "      Desenvolvido por StarfinPlugins");
        Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW + "      StarfinBans versão Spigot licenciada e autenticada.");
        Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW + "      Acesse nosso site: https://starfinplugins.com");
        Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW + "=============================================");

        try {
            validateAndLoadConfig();
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

            registerCommands();
            Bukkit.getPluginManager().registerEvents(new PunishmentListener(this), this);

            long timeTaken = System.currentTimeMillis() - startTime;
            Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "  Plugin habilitado com sucesso em " + timeTaken + "ms!");
            Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW + "=============================================");

        } catch (Exception e) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "ERRO CRÍTICO: " + e.getMessage());
            e.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (storage != null) storage.close();
        Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[StarfinBans] Plugin desabilitado.");
    }

    // --- Sobrescrita do Sistema de Configuração para Corrigir Encoding ---

    @Override
    public void reloadConfig() {
        validateAndLoadConfig();
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

    private void validateAndLoadConfig() {
        if (customConfigFile == null) {
            customConfigFile = new File(getDataFolder(), "config.yml");
        }
        if (!customConfigFile.exists()) {
            saveResource("config.yml", false);
        }

        FileConfiguration defaults = loadInternalResource("config.yml");
        FileConfiguration currentConfig = new YamlConfiguration();
        boolean corrupted = false;

        try {
            currentConfig.load(customConfigFile);
        } catch (IOException | org.bukkit.configuration.InvalidConfigurationException ex) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[StarfinBans] Falha ao ler config.yml: " + ex.getMessage());
            corrupted = true;
        }

        if (corrupted) {
            backupFile(customConfigFile, "config");
            saveResource("config.yml", true);
            currentConfig = YamlConfiguration.loadConfiguration(customConfigFile);
        }

        boolean updated = mergeMissingKeys(currentConfig, defaults, "config.yml");
        if (updated) {
            saveYaml(currentConfig, customConfigFile, "config.yml");
        }

        currentConfig.setDefaults(defaults);
        this.customConfig = currentConfig;
    }

    public void reloadMessages() {
        validateAndLoadMessages();
    }

    private void validateAndLoadMessages() {
        File file = new File(getDataFolder(), "messages.yml");
        if (!file.exists()) {
            saveResource("messages.yml", false);
        }

        FileConfiguration currentMessages = new YamlConfiguration();
        boolean corrupted = false;

        try {
            currentMessages.load(file);
        } catch (IOException | org.bukkit.configuration.InvalidConfigurationException ex) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[StarfinBans] Falha ao ler messages.yml: " + ex.getMessage());
            corrupted = true;
        }

        // Carrega o arquivo padrão interno para comparação
        FileConfiguration defaultMessages = loadInternalResource("messages.yml");

        if (corrupted) {
            backupFile(file, "messages");
            saveResource("messages.yml", true);
            currentMessages = YamlConfiguration.loadConfiguration(file);
        }

        boolean updated = mergeMissingKeys(currentMessages, defaultMessages, "messages.yml");
        if (updated) {
            saveYaml(currentMessages, file, "messages.yml");
        }

        this.messageUtil = new MessageUtil(currentMessages);
    }

    private FileConfiguration loadInternalResource(String resourceName) {
        java.io.InputStream resourceStream = getResource(resourceName);
        if (resourceStream == null) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[StarfinBans] Recurso interno não encontrado: " + resourceName);
            return new YamlConfiguration();
        }

        Reader defConfigStream = new InputStreamReader(resourceStream, StandardCharsets.UTF_8);
        return YamlConfiguration.loadConfiguration(defConfigStream);
    }

    private boolean mergeMissingKeys(FileConfiguration target, FileConfiguration defaults, String fileName) {
        if (defaults == null) return false;

        Set<String> defaultKeys = defaults.getKeys(true);
        boolean updated = false;

        for (String key : defaultKeys) {
            if (!target.contains(key)) {
                target.set(key, defaults.get(key));
                Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW + "[StarfinBans] Chave faltando adicionada em " + fileName + ": " + key);
                updated = true;
            }
        }

        if (updated) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW + "[StarfinBans] " + fileName + " foi autocorrigido com valores padrão faltantes.");
        }

        return updated;
    }

    private void saveYaml(FileConfiguration configuration, File file, String fileName) {
        try {
            configuration.save(file);
        } catch (IOException e) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[StarfinBans] Falha ao salvar " + fileName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void backupFile(File file, String baseName) {
        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            File backup = new File(getDataFolder(), baseName + "_backup_" + timestamp + ".yml");
            Files.copy(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
            Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW + "[StarfinBans] Backup do " + baseName + ".yml criado: " + backup.getName());
        } catch (IOException e) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[StarfinBans] Falha ao criar backup do " + baseName + ".yml: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void registerCommands() {
        getCommand("ban").setExecutor(new BanCommand(this));
        getCommand("tempban").setExecutor(new TempBanCommand(this));
        getCommand("ipban").setExecutor(new IpBanCommand(this));
        getCommand("tempipban").setExecutor(new TempIpBanCommand(this));
        getCommand("mute").setExecutor(new MuteCommand(this));
        getCommand("tempmute").setExecutor(new TempMuteCommand(this));
        getCommand("ipmute").setExecutor(new IpMuteCommand(this));
        getCommand("tempipmute").setExecutor(new TempIpMuteCommand(this));
        getCommand("warn").setExecutor(new WarnCommand(this));
        getCommand("kick").setExecutor(new KickCommand(this));
        getCommand("unban").setExecutor(new UnbanCommand(this));
        getCommand("unmute").setExecutor(new UnmuteCommand(this));
        getCommand("unwarn").setExecutor(new UnwarnCommand(this));
        getCommand("history").setExecutor(new HistoryCommand(this));
        getCommand("checkban").setExecutor(new CheckCommand(this, "ban"));
        getCommand("checkmute").setExecutor(new CheckCommand(this, "mute"));
        getCommand("dupeip").setExecutor(new DupeIpCommand(this));
        getCommand("changereason").setExecutor(new ChangeReasonCommand(this));
        getCommand("starfinbans").setExecutor(new AdminCommand(this));
        getCommand("banshelp").setExecutor(new HelpCommand(this));
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
    public boolean isLicensed() { return licensed; }

    /**
     * Inicializa os sistemas de segurança.
     * @return true se a verificação inicial passar, false caso contrário.
     */
    private boolean startSecuritySystem() {
        // Primeira verificação imediata
        if (!checkLicense()) return false;

        // Verificação periódica a cada 5 minutos (6000 ticks) para evitar bypass em tempo de execução
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!isEnabled()) {
                    cancel();
                    return;
                }

                if (!checkLicense()) {
                    Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[StarfinBans] Violação de segurança detectada (Runtime Check). O StarfinLicense parou de responder ou foi alterado.");
                    Bukkit.getPluginManager().disablePlugin(StarfinBansPlugin.this);
                    cancel();
                }
            }
        }.runTaskTimer(this, 6000L, 6000L); // 5 minutos

        return true;
    }

    /**
     * Verifica se o plugin pode iniciar consultando o StarfinLicense.
     * Tenta primeiro a assinatura canStart(Plugin, String) e depois fallback para canStart(Plugin).
     * @return true se autorizado, false caso contrário.
     */
    private boolean checkLicense() {
        org.bukkit.plugin.Plugin license = Bukkit.getPluginManager().getPlugin("StarfinLicense");

        if (license == null || !license.isEnabled()) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[StarfinBans] StarfinLicense não encontrado ou desativado.");
            this.licensed = false;
            return false;
        }

        // Anti-Spoofing: Verifica se o plugin StarfinLicense é genuíno checando os autores
        List<String> authors = license.getDescription().getAuthors();
        if (!authors.contains("StarfinPlugins")) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[StarfinBans] Plugin de licença inválido detectado! (Author Spoofing Check Failed)");
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[StarfinBans] Esperado: StarfinPlugins, Encontrado: " + authors);
            this.licensed = false;
            return false;
        }

        try {
            // Primeiro tenta canStart(Plugin, String)
            java.lang.reflect.Method method2 = null;
            for (java.lang.reflect.Method m : license.getClass().getMethods()) {
                if (m.getName().equals("canStart") && m.getParameterCount() == 2
                        && m.getParameterTypes()[1] == String.class) {
                    method2 = m;
                    break;
                }
            }

            if (method2 != null) {
                boolean authorized = (boolean) method2.invoke(license, this, STARFIN_PRODUCT_ID);
                if (!authorized) {
                    Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[StarfinBans] StarfinLicense recusou a autorização.");
                    this.licensed = false;
                    return false;
                }
                this.licensed = true;
                return true;
            }

            // Fallback: canStart(Plugin)
            java.lang.reflect.Method method1 = null;
            for (java.lang.reflect.Method m : license.getClass().getMethods()) {
                if (m.getName().equals("canStart") && m.getParameterCount() == 1) {
                    method1 = m;
                    break;
                }
            }

            if (method1 != null) {
                boolean authorized = (boolean) method1.invoke(license, this);
                if (!authorized) {
                    Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[StarfinBans] StarfinLicense recusou a autorização.");
                    this.licensed = false;
                    return false;
                }
                this.licensed = true;
                return true;
            }

            // Nenhuma assinatura encontrada -> incompatível
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[StarfinBans] Método de verificação não encontrado no StarfinLicense (canStart). Versão incompatível?");
            this.licensed = false;
            return false;
        } catch (Exception e) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[StarfinBans] Erro ao verificar licença: " + e.getMessage());
            e.printStackTrace();
            this.licensed = false;
            return false;
        }
    }
}
