package com.starfinplugins.bans.license;

import org.bukkit.plugin.Plugin;
import br.com.starfinplugins.bans.security.SignedGrantVerifier;
import org.bukkit.Bukkit;
import com.starfinplugins.core.api.VerifiedAuthorizationService;

/** Bukkit-only bridge. There is deliberately no StarfinLicense fallback. */
public final class CoreAuthorization {
    public static final String PRODUCT_ID = "starfin_bans";
    private CoreAuthorization() { }
    public static boolean permitted(Plugin core, Plugin consumer) {
        if (core == null || consumer == null || !core.isEnabled() || !consumer.isEnabled()) return false;
        VerifiedAuthorizationService service = Bukkit.getServicesManager().load(VerifiedAuthorizationService.class);
        return service != null && service.protocolVersion() == 1
            && service.get(PRODUCT_ID).permitted()
            && SignedGrantVerifier.valid(service.signedGrant(PRODUCT_ID), PRODUCT_ID,
                service.installationId(), Bukkit.getPort());
    }
}
