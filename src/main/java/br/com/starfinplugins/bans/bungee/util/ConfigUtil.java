package br.com.starfinplugins.bans.bungee.util;

import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.*;
import java.nio.charset.StandardCharsets;

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
            return new Configuration();
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
