package br.com.starfinplugins.bans.security;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/** Independent product-side verification. Never accepts a Core boolean as proof. */
public final class SignedGrantVerifier {
    private static final String KEY = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEnb3J+jZp4sbRuDt2FMmhr0ykialH"
        + "8Ox4enZN4XkVs3h8tmGTnZL4LAmLvBDwvSmY5CKxaRMtbC5esst/Qam2Jw==";
    private static final PublicKey PUBLIC_KEY = load();
    private SignedGrantVerifier() { }

    public static boolean valid(String token, String productId, UUID installationId, int serverPort) {
        return valid(token, productId, installationId, serverPort, PUBLIC_KEY);
    }
    static boolean valid(String token, String productId, UUID installationId, int serverPort, PublicKey publicKey) {
        try {
            if (token == null || token.length() > 8192 || !token.matches("[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+")) return false;
            String[] parts = token.split("\\.", -1);
            byte[] payload = Base64.getUrlDecoder().decode(parts[0]);
            Signature verifier = Signature.getInstance("SHA256withECDSA");
            verifier.initVerify(publicKey);
            verifier.update(payload);
            if (!verifier.verify(Base64.getUrlDecoder().decode(parts[1]))) return false;
            JsonObject claim = new JsonParser().parse(new String(payload, StandardCharsets.UTF_8)).getAsJsonObject();
            long now = Instant.now().getEpochSecond();
            long issued = claim.get("issuedAt").getAsLong();
            long notBefore = claim.get("notBefore").getAsLong();
            long expiry = claim.get("expiresAt").getAsLong();
            return claim.get("version").getAsInt() == 1
                && "starfinplugins.com.br".equals(claim.get("issuer").getAsString())
                && "starfin-2026-10-p256".equals(claim.get("keyId").getAsString())
                && productId.equals(claim.get("productId").getAsString())
                && installationId.toString().equals(claim.get("installationId").getAsString())
                && serverPort == claim.get("serverPort").getAsInt()
                && !claim.get("observedIp").getAsString().isEmpty()
                && UUID.fromString(claim.get("sessionId").getAsString()) != null
                && UUID.fromString(claim.get("jti").getAsString()) != null
                && issued <= now + 60 && issued >= now - 3600
                && notBefore <= now + 60 && expiry > now && expiry <= now + 3600;
        } catch (Exception invalid) { return false; }
    }

    private static PublicKey load() {
        try {
            return KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(KEY)));
        } catch (Exception error) { throw new ExceptionInInitializerError(error); }
    }
}
