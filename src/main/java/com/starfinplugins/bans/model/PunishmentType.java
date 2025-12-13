package com.starfinplugins.bans.model;

public enum PunishmentType {
    BAN_PERMANENT,
    BAN_TEMPORARY,
    BAN_IP_PERMANENT,
    BAN_IP_TEMPORARY,
    MUTE_PERMANENT,
    MUTE_TEMPORARY,
    MUTE_IP_PERMANENT, // Novo
    MUTE_IP_TEMPORARY, // Novo
    WARN,
    KICK;

    public boolean isBan() {
        return this == BAN_PERMANENT || this == BAN_TEMPORARY || isIpBan();
    }

    public boolean isIpBan() {
        return this == BAN_IP_PERMANENT || this == BAN_IP_TEMPORARY;
    }

    public boolean isMute() {
        return this == MUTE_PERMANENT || this == MUTE_TEMPORARY || isIpMute();
    }

    public boolean isIpMute() {
        return this == MUTE_IP_PERMANENT || this == MUTE_IP_TEMPORARY;
    }

    public boolean isTemp() {
        return name().contains("TEMPORARY");
    }
}