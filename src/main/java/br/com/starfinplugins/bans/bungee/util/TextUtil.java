package br.com.starfinplugins.bans.bungee.util;

import net.md_5.bungee.api.chat.TextComponent;

public final class TextUtil {

    private TextUtil() {}

    public static TextComponent tc(String text) {
        return new TextComponent(text == null ? "" : text);
    }
}
