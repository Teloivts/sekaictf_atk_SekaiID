# SEKAI ID Exploit APK

This project builds a CTF exploit APK for the SEKAIID challenge.

The exploit registers an Activity for `com.sekai.id.ACTION_PRESENT_CREDENTIAL`, receives Verifier's challenge, uses the wallet companion badge provider as a signing oracle, mutates the returned presentation to `accessProfile=admin`, recomputes `claimsTag` using `libsekaibind.so`, and returns the forged presentation to Verifier.

Important: the pure third-party APK cannot read the wallet's private `binding_seed`. The official APKs are debuggable in the challenge image, so the seed can be extracted by an ADB-side agent with `run-as com.sekai.id` and injected into this exploit at runtime or at build time.
