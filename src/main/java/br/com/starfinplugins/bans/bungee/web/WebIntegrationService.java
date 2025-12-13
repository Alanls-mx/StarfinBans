package br.com.starfinplugins.bans.bungee.web;

import br.com.starfinplugins.bans.bungee.StarfinBansBungeePlugin;
import br.com.starfinplugins.bans.bungee.model.Punishment;

import net.md_5.bungee.config.Configuration;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class WebIntegrationService {

    private final StarfinBansBungeePlugin plugin;

    public WebIntegrationService(StarfinBansBungeePlugin plugin) {
        this.plugin = plugin;
    }

    // ✅ usa exatamente: web-api.enabled
    public boolean isEnabled() {
        return plugin.getCfg().getBoolean("web-api.enabled", false);
    }

    // ✅ usa exatamente: web-api.send-on-create
    public void sendCreate(Punishment p) {
        if (!isEnabled()) return;
        if (!plugin.getCfg().getBoolean("web-api.send-on-create", true)) return;
        postSmart("create", p);
    }

    // ✅ usa exatamente: web-api.send-on-update
    public void sendUpdate(Punishment p) {
        if (!isEnabled()) return;
        if (!plugin.getCfg().getBoolean("web-api.send-on-update", true)) return;
        postSmart("update", p);
    }

    // ✅ usa exatamente: web-api.send-on-expire
    public void sendExpire(Punishment p) {
        if (!isEnabled()) return;
        if (!plugin.getCfg().getBoolean("web-api.send-on-expire", true)) return;
        postSmart("expire", p);
    }

    private void postSmart(String action, Punishment p) {
        Configuration cfg = plugin.getCfg();

        String baseUrl = cfg.getString("web-api.base-url", "").trim();
        String apiKey = cfg.getString("web-api.api-key", "").trim();
        int timeout = cfg.getInt("web-api.timeout", 5000);

        if (baseUrl.isEmpty()) return;

        // payload SEM mudar config
        String json = WebPayloads.toJson(action, p);

        // Estratégia compatível:
        // 1) tenta POST baseUrl + "/" + action
        // 2) se falhar, tenta POST baseUrl (sem alterar nada)
        String urlWithAction = normalize(baseUrl) + "/" + action;
        String urlRaw = normalize(baseUrl);

        // tenta /action primeiro
        boolean ok = post(urlWithAction, apiKey, timeout, json);
        if (!ok) {
            // fallback para base-url puro
            post(urlRaw, apiKey, timeout, json);
        }
    }

    private boolean post(String url, String apiKey, int timeout, String jsonBody) {
        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) new URL(url).openConnection();
            con.setRequestMethod("POST");
            con.setConnectTimeout(timeout);
            con.setReadTimeout(timeout);
            con.setDoOutput(true);

            con.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            if (!apiKey.isEmpty()) con.setRequestProperty("Authorization", "Bearer " + apiKey);

            byte[] data = jsonBody.getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = con.getOutputStream()) {
                os.write(data);
            }

            int code = con.getResponseCode();
            if (code < 200 || code >= 300) {
                plugin.getLogger().warning("[WebAPI] HTTP " + code + " -> " + url);
                return false;
            }
            return true;

        } catch (Exception e) {
            plugin.getLogger().warning("[WebAPI] Erro -> " + url + " : " + e.getMessage());
            return false;
        } finally {
            if (con != null) con.disconnect();
        }
    }

    private String normalize(String baseUrl) {
        if (baseUrl.endsWith("/")) return baseUrl.substring(0, baseUrl.length() - 1);
        return baseUrl;
    }
}
