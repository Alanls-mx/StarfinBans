package com.starfinplugins.bans.api.event;

import com.starfinplugins.bans.model.PunishmentType;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

public class PlayerUnpunishEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final UUID target;
    private final PunishmentType type;
    private final UUID staff;
    private final String staffName;

    public PlayerUnpunishEvent(UUID target, PunishmentType type, UUID staff, String staffName) {
        this.target = target;
        this.type = type;
        this.staff = staff;
        this.staffName = staffName;
    }

    public UUID getTarget() { return target; }
    public PunishmentType getType() { return type; }
    public UUID getStaff() { return staff; }
    public String getStaffName() { return staffName; }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}