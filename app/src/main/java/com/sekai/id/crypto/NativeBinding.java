package com.sekai.id.crypto;

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

    private static native byte[] nativeRequestAuth(
            byte[] seed,
            byte[] credentialId,
            byte[] holderPublicKey,
            byte[] challenge
    );

    public static String requestAuth(byte[] seed, String credentialId, String holderPublicKey, String challenge) {
        ensureLoaded();
        byte[] out = nativeRequestAuth(
                seed,
                credentialId.getBytes(StandardCharsets.UTF_8),
                holderPublicKey.getBytes(StandardCharsets.UTF_8),
                challenge.getBytes(StandardCharsets.UTF_8)
        );
        return Base64.encodeToString(out, Base64.NO_WRAP);
    }
}
