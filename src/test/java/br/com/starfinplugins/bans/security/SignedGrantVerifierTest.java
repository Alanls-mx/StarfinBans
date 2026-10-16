package br.com.starfinplugins.bans.security;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import org.junit.Assert;
import org.junit.Test;

public class SignedGrantVerifierTest {
    @Test public void verifiesProductInstallationPortAndSignature() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair keys = generator.generateKeyPair();
        UUID installation = UUID.randomUUID();
        long now = Instant.now().getEpochSecond();
        String json = "{\"version\":1,\"issuer\":\"starfinplugins.com.br\","
            + "\"keyId\":\"starfin-2026-10-p256\",\"productId\":\"starfin_bans\","
            + "\"installationId\":\"" + installation + "\",\"sessionId\":\"" + UUID.randomUUID()
            + "\",\"jti\":\"" + UUID.randomUUID() + "\",\"issuedAt\":" + now
            + ",\"notBefore\":" + now + ",\"expiresAt\":" + (now + 600)
            + ",\"observedIp\":\"54.207.79.219\",\"serverPort\":25565}";
        byte[] payload = json.getBytes(StandardCharsets.UTF_8);
        Signature signer = Signature.getInstance("SHA256withECDSA");
        signer.initSign(keys.getPrivate()); signer.update(payload);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(payload) + "."
            + Base64.getUrlEncoder().withoutPadding().encodeToString(signer.sign());
        Assert.assertTrue(SignedGrantVerifier.valid(token, "starfin_bans", installation, 25565, keys.getPublic()));
        Assert.assertFalse(SignedGrantVerifier.valid(token, "other_plugin", installation, 25565, keys.getPublic()));
        Assert.assertFalse(SignedGrantVerifier.valid(token, "starfin_bans", installation, 25566, keys.getPublic()));
        Assert.assertFalse(SignedGrantVerifier.valid(token, "starfin_bans", UUID.randomUUID(), 25565, keys.getPublic()));
        Assert.assertFalse(SignedGrantVerifier.valid(token.substring(0, token.length() - 3) + "abc",
            "starfin_bans", installation, 25565, keys.getPublic()));
    }
}
