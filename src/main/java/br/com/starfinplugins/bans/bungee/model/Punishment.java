package br.com.starfinplugins.bans.bungee.model;

import java.util.UUID;

public class Punishment {

    private final UUID id;
    private final UUID target;
    private final String playerName;
    private final String ip;
    private final UUID staff;
    private final String staffName;
    private final PunishmentType type;
    private final String reason;
    private final long createdAt;
    private final long expires;
    private boolean active;

    public Punishment(
            UUID id,
            UUID target,
            String playerName,
            String ip,
            UUID staff,
            String staffName,
            PunishmentType type,
            String reason,
            long createdAt,
            long expires,
            boolean active
    ) {
        this.id = id;
        this.target = target;
        this.playerName = playerName;
        this.ip = ip;
        this.staff = staff;
        this.staffName = staffName;
        this.type = type;
        this.reason = reason;
        this.createdAt = createdAt;
        this.expires = expires;
        this.active = active;
    }

    public UUID getId() { return id; }
    public UUID getTarget() { return target; }
    public String getPlayerName() { return playerName; }
    public String getIp() { return ip; }
    public UUID getStaff() { return staff; }
    public String getStaffName() { return staffName; }
    public PunishmentType getType() { return type; }
    public String getReason() { return reason; }
    public long getCreatedAt() { return createdAt; }
    public long getExpires() { return expires; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public boolean isExpired() {
        return expires > 0 && System.currentTimeMillis() > expires;
    }
}
