package br.com.starfinplugins.bans.bungee.license;

import br.com.starfinplugins.bans.bungee.StarfinBansBungeePlugin;
import br.com.starfinplugins.bans.security.SignedGrantVerifier;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.Base64;

/** Proxy installation uses the same site authority without a Bukkit dependency. */
public final class WebInstallationAuthorization {
    private final StarfinBansBungeePlugin plugin;
    private final UUID installationId;
    private final String credential;
    private final int port;
    private final String origin;
    private volatile long validUntilEpoch;

    public WebInstallationAuthorization(StarfinBansBungeePlugin plugin, UUID installationId,
                                        String credential, int port, String origin) {
        if (!origin.matches("https://[A-Za-z0-9.-]+(?::[0-9]{2,5})?/?"))
            throw new IllegalArgumentException("Endpoint HTTPS invalido");
        this.plugin = plugin; this.installationId = installationId;
        this.credential = credential; this.port = port;
        this.origin = origin.endsWith("/") ? origin.substring(0, origin.length() - 1) : origin;
    }

    public boolean check() {
        try {
            JsonObject request = new JsonObject();
            request.addProperty("installationId", installationId.toString());
            request.addProperty("platform", "BungeeCord");
            request.addProperty("coreVersion", plugin.getDescription().getVersion());
            request.addProperty("serverPort", port);
            JsonObject response = post("/v3/installations/check", request, true);
            if (!response.get("valid").getAsBoolean()
                    || response.get("protocolVersion").getAsInt() != 3) return false;
            JsonObject installation = response.getAsJsonObject("installation");
            if (!installationId.toString().equals(installation.get("id").getAsString())
                    || !installation.get("ipMatches").getAsBoolean()
                    || !installation.get("portMatches").getAsBoolean()
                    || installation.get("reportedPort").getAsInt() != port
                    || installation.get("authorizedPort").getAsInt() != port) return false;
            JsonObject grants = response.getAsJsonObject("signedGrants");
            if (grants == null || !grants.has("starfin_bans")) return false;
            String token = grants.get("starfin_bans").getAsString();
            if (!SignedGrantVerifier.valid(token, "starfin_bans", installationId, port)) return false;
            byte[] payload = Base64.getUrlDecoder().decode(token.split("\\.")[0]);
            validUntilEpoch = new JsonParser().parse(new String(payload, StandardCharsets.UTF_8))
                .getAsJsonObject().get("expiresAt").getAsLong();
            return true;
        } catch (Exception error) {
            plugin.getLogger().warning("Autorizacao Web do proxy indisponivel: " + error.getClass().getSimpleName());
            return false;
        }
    }
    public boolean current() { return validUntilEpoch > System.currentTimeMillis() / 1000; }

    public String challenge() throws Exception {
        JsonObject request = new JsonObject();
        request.addProperty("installationId", installationId.toString());
        request.addProperty("credential", credential);
        request.addProperty("name", "StarfinBans Proxy");
        request.addProperty("platform", "BungeeCord");
        request.addProperty("coreVersion", plugin.getDescription().getVersion());
        request.addProperty("serverPort", port);
        return post("/v3/installations/challenge", request, false).get("code").getAsString();
    }

    private JsonObject post(String route, JsonObject body, boolean authenticate) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(origin + route).openConnection();
        try {
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(3500); connection.setReadTimeout(3500);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            if (authenticate) connection.setRequestProperty("Authorization", "Installation " + credential);
            byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
            try (OutputStream output = connection.getOutputStream()) { output.write(bytes); }
            int status = connection.getResponseCode();
            if (status != (authenticate ? 200 : 201)) throw new java.io.IOException("HTTP " + status);
            try (InputStream input = connection.getInputStream()) {
                byte[] data = new byte[65536];
                int total = 0, length;
                while ((length = input.read(data, total, data.length - total)) > 0) {
                    total += length;
                    if (total == data.length) throw new java.io.IOException("Resposta excessiva");
                }
                return new JsonParser().parse(new String(data, 0, total, StandardCharsets.UTF_8)).getAsJsonObject();
            }
        } finally { connection.disconnect(); }
    }
}
