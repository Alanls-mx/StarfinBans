package com.starfinplugins.bans.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeUtil {

    public static long parseDuration(String input) {
        if (input == null || input.isEmpty()) return -1;
        long result = 0;
        Pattern pattern = Pattern.compile("(\\d+)([wdhms])");
        Matcher matcher = pattern.matcher(input);
        int parsedUntil = 0;
        try {
            while (matcher.find()) {
                if (matcher.start() != parsedUntil) return -1;
                long value = Long.parseLong(matcher.group(1));
                long unitMillis;
                switch (matcher.group(2)) {
                    case "w": unitMillis = TimeUnit.DAYS.toMillis(7); break;
                    case "d": unitMillis = TimeUnit.DAYS.toMillis(1); break;
                    case "h": unitMillis = TimeUnit.HOURS.toMillis(1); break;
                    case "m": unitMillis = TimeUnit.MINUTES.toMillis(1); break;
                    case "s": unitMillis = TimeUnit.SECONDS.toMillis(1); break;
                    default: return -1;
                }
                result = Math.addExact(result, Math.multiplyExact(value, unitMillis));
                parsedUntil = matcher.end();
            }
        } catch (NumberFormatException | ArithmeticException invalidDuration) {
            return -1;
        }
        return parsedUntil == input.length() && parsedUntil > 0 ? result : -1;
    }

    public static String formatDuration(long millis) {
        if (millis <= 0) return "0s";
        long days = TimeUnit.MILLISECONDS.toDays(millis);
        long hours = TimeUnit.MILLISECONDS.toHours(millis) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0) sb.append(seconds).append("s");

        return sb.toString().trim();
    }

    public static String formatDate(long millis) {
        return new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date(millis));
    }
}