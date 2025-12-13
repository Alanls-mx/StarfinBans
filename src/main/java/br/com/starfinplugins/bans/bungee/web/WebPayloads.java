package br.com.starfinplugins.bans.bungee.web;

import br.com.starfinplugins.bans.bungee.model.Punishment;

public final class WebPayloads {

    private WebPayloads() {}

    // Não depende de config extra (pra não quebrar nada)
    public static String toJson(String action, Punishment p) {
        return "{"
                + "\"action\":\"" + esc(action) + "\","
                + "\"id\":\"" + esc(p.getId().toString()) + "\","
                + "\"target\":\"" + esc(p.getTarget() != null ? p.getTarget().toString() : "") + "\","
                + "\"targetName\":\"" + esc(p.getPlayerName()) + "\","
                + "\"ip\":\"" + esc(p.getIp()) + "\","
                + "\"staff\":\"" + esc(p.getStaff() != null ? p.getStaff().toString() : "") + "\","
                + "\"staffName\":\"" + esc(p.getStaffName()) + "\","
                + "\"type\":\"" + esc(p.getType().name()) + "\","
                + "\"reason\":\"" + esc(p.getReason()) + "\","
                + "\"createdAt\":" + p.getCreatedAt() + ","
                + "\"expires\":" + p.getExpires() + ","
                + "\"active\":" + p.isActive()
                + "}";
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
