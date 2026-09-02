# Tideglass Google Play release checklist

## Product structure

- Publish `com.tideglass.surf.watchface` as the paid watch-face listing.
- Publish `com.tideglass.surf.provider` as the free Tideglass Data companion.
- Add the Wear OS form factor and begin with an internal-testing track for both listings.
- Keep the compatibility statement visible: Wear OS 5+, not Mi Fitness / Xiaomi Watch S1 Active.

## Identity and legal details still required from the publisher

- Google Play developer account and verified legal publisher name.
- Support email that is actively monitored.
- Public privacy-policy URL containing the publisher name, support email, effective date and hosting log-retention wording.
- Final price and localized price rounding for the paid watch-face listing.

## Signing

Create one private upload keystore, back it up outside the repository and configure these GitHub Actions secrets:

- `ANDROID_UPLOAD_KEYSTORE_BASE64`
- `ANDROID_UPLOAD_STORE_PASSWORD`
- `ANDROID_UPLOAD_KEY_ALIAS`
- `ANDROID_UPLOAD_KEY_PASSWORD`

Then run **Build signed Play release** manually. That workflow refuses to build if any signing value is absent and emits only the two signed AAB files.

## Store assets and review

- App icon: 512 × 512 PNG for each listing.
- Feature graphic: 1024 × 500 PNG for each listing.
- Wear OS screenshots showing interactive, ambient and complication setup states.
- Complete the Data safety form: optional location is processed on-device; the static data host receives standard network request metadata.
- Complete content rating, ads declaration and target-audience sections.
- Test both listings together on a Wear OS 5+ physical watch or emulator before production rollout.
