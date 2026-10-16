package br.com.starfinplugins.bans.bungee.license;

import br.com.starfinplugins.bans.security.SignedGrantVerifier;
import com.starfinplugins.core.api.VerifiedAuthorizationService;
import com.starfinplugins.core.api.ProductAuthorization;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;

/** Reads the Core's live state, then independently verifies the Web signature. */
public final class CoreBungeeAuthorization {
    public static final String PRODUCT = "starfin_bans";
    private CoreBungeeAuthorization() { }

    public static boolean permitted() {
        try {
            Plugin core = ProxyServer.getInstance().getPluginManager().getPlugin("StarfinCore");
            if (!(core instanceof VerifiedAuthorizationService)) return false;
            VerifiedAuthorizationService service = (VerifiedAuthorizationService) core;
            ProductAuthorization product = service.get(PRODUCT);
            if (service.protocolVersion() != 1 || product == null || !product.permitted()) return false;
            int port = ProxyServer.getInstance().getConfig().getListeners().iterator().next().getHost().getPort();
            return SignedGrantVerifier.valid(service.signedGrant(PRODUCT), PRODUCT,
                service.installationId(), port);
        } catch (RuntimeException invalid) {
            return false;
        }
    }
}
