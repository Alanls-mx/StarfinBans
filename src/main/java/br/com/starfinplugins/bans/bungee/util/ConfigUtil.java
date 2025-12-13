package br.com.starfinplugins.bans.bungee.util;

import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class ConfigUtil {

    private ConfigUtil() {}

    public static void ensureDefaults(Plugin plugin, String... resources) throws IOException {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();

        for (String res : resources) {
            File out = new File(plugin.getDataFolder(), res);
            if (out.exists()) continue;

            try (InputStream in = plugin.getResourceAsStream(res)) {
                if (in == null) throw new FileNotFoundException("Resource not found in jar: " + res);
                try (OutputStream os = new FileOutputStream(out)) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = in.read(buffer)) != -1) os.write(buffer, 0, len);
                }
            }
        }
    }

    public static Configuration loadConfig(Plugin plugin, String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            return ConfigurationProvider.getProvider(YamlConfiguration.class).load(reader);
        } catch (Exception e) {
            plugin.getLogger().severe("Falha ao carregar " + fileName + ": " + e.getMessage());
            backupAndRestore(plugin, file, fileName);

            try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
                return ConfigurationProvider.getProvider(YamlConfiguration.class).load(reader);
            } catch (Exception retry) {
                plugin.getLogger().severe("Falha ao recarregar " + fileName + " após restauração: " + retry.getMessage());
                return new Configuration();
            }
        }
    }

    private static void backupAndRestore(Plugin plugin, File file, String resourceName) {
        if (file.exists()) {
            try {
                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                File backup = new File(plugin.getDataFolder(), resourceName.replace(".yml", "") + "_backup_" + timestamp + ".yml");
                Files.copy(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
                plugin.getLogger().info("Backup criado: " + backup.getName());
            } catch (Exception ex) {
                plugin.getLogger().severe("Falha ao criar backup de " + resourceName + ": " + ex.getMessage());
            }
        }

        try (InputStream in = plugin.getResourceAsStream(resourceName)) {
            if (in == null) {
                plugin.getLogger().severe("Recurso padrão não encontrado para restaurar: " + resourceName);
                return;
            }

            Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            plugin.getLogger().info(resourceName + " foi regenerado a partir do padrão interno.");
        } catch (Exception ex) {
            plugin.getLogger().severe("Falha ao restaurar " + resourceName + " padrão: " + ex.getMessage());
        }
    }

    public static void saveConfig(Plugin plugin, String fileName, Configuration cfg) {
        File file = new File(plugin.getDataFolder(), fileName);
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(cfg, writer);
        } catch (Exception e) {
            plugin.getLogger().severe("Falha ao salvar " + fileName + ": " + e.getMessage());
        }
    }
}
