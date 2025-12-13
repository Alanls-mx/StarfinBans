package com.starfinplugins.bans.web;

import com.starfinplugins.bans.StarfinBansPlugin;

public class WebIntegrationService {

    private final StarfinBansPlugin plugin;

    public WebIntegrationService(StarfinBansPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        // Implementação futura ou vazia se não usada
        plugin.getLogger().info("Web integration service started.");
    }

    public void stop() {
        // ...
    }
}