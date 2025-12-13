package com.starfinplugins.bans.api.event;

import com.starfinplugins.bans.model.Punishment;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerPunishEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final Punishment punishment;
    private boolean cancelled;

    public PlayerPunishEvent(Punishment punishment) {
        this.punishment = punishment;
    }

    public Punishment getPunishment() {
        return punishment;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}