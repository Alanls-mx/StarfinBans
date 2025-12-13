package br.com.starfinplugins.bans.bungee.util;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public final class CmdUtil {

    private CmdUtil() {}

    public static ProxiedPlayer online(String name) {
        if (name == null) return null;
        return ProxyServer.getInstance().getPlayer(name);
    }

    public static boolean looksLikeIp(String s) {
        if (s == null) return false;
        return s.contains(".") || s.contains(":");
    }
}
