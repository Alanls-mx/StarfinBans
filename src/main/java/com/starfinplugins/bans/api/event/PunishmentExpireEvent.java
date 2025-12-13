package com.starfinplugins.bans.api.event;

import com.starfinplugins.bans.model.Punishment;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PunishmentExpireEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Punishment punishment;

    public PunishmentExpireEvent(Punishment punishment) {
        this.punishment = punishment;
    }

    public Punishment getPunishment() {
        return punishment;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}