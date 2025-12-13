package br.com.starfinplugins.bans.bungee.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TimeUtil {

    private TimeUtil() {}

    public static long parseDuration(String input) {
        if (input == null || input.isEmpty()) return -1;
        long result = 0;
        Pattern pattern = Pattern.compile("(\\d+)([wdhms])");
        Matcher matcher = pattern.matcher(input);

        while (matcher.find()) {
            long value = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);
            switch (unit) {
                case "w": result += TimeUnit.DAYS.toMillis(value * 7); break;
                case "d": result += TimeUnit.DAYS.toMillis(value); break;
                case "h": result += TimeUnit.HOURS.toMillis(value); break;
                case "m": result += TimeUnit.MINUTES.toMillis(value); break;
                case "s": result += TimeUnit.SECONDS.toMillis(value); break;
            }
        }
        return result;
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
