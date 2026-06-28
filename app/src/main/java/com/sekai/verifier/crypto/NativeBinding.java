package com.sekai.verifier.crypto;

import android.util.Base64;

import java.nio.charset.StandardCharsets;

public final class NativeBinding {
    private static volatile boolean loaded;

    private NativeBinding() {}

    private static void ensureLoaded() {
        if (!loaded) {
            synchronized (NativeBinding.class) {
                if (!loaded) {
                    System.loadLibrary("sekaibind");
                    loaded = true;
                }
            }
        }
    }

    private static native byte[] nativeClaimsTagFromFingerprint(
            byte[] fingerprint,
            byte[] credentialId,
            byte[] holderPublicKey,
            byte[] canonicalClaims
    );

    public static String claimsTagFromFingerprint(
            String fingerprintB64,
            String credentialId,
            String holderPublicKey,
            byte[] canonicalClaims
    ) {
        ensureLoaded();
        byte[] fp = Base64.decode(fingerprintB64, Base64.NO_WRAP);
        byte[] out = nativeClaimsTagFromFingerprint(
                fp,
                credentialId.getBytes(StandardCharsets.UTF_8),
                holderPublicKey.getBytes(StandardCharsets.UTF_8),
                canonicalClaims
        );
        return Base64.encodeToString(out, Base64.NO_WRAP);
    }
}
