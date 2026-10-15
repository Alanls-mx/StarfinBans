package com.starfinplugins.bans.license;

import org.bukkit.plugin.Plugin;

/** Bukkit-only bridge. There is deliberately no StarfinLicense fallback. */
public final class CoreAuthorization {
    public static final String PRODUCT_ID = "starfin_bans";
    private CoreAuthorization() { }
    public static boolean permitted(Plugin core, Plugin consumer) {
        if (core == null || consumer == null || !core.isEnabled() || !consumer.isEnabled()) return false;
        try {
            java.lang.reflect.Method method = core.getClass().getMethod("canStart", Plugin.class, String.class);
            return Boolean.TRUE.equals(method.invoke(core, consumer, PRODUCT_ID));
        } catch (ReflectiveOperationException | IllegalArgumentException error) {
            return false;
        }
    }
}
