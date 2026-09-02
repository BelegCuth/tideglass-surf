# Tideglass Surf — MVP specification

## Product promise

Show the decision-critical surf conditions in one glance: current tide and trend, next tidal event, swell, wind, and selected nearby spot.

## First release

- Platform: Wear OS 5+ (Samsung Galaxy Watch and Xiaomi Watch 2/2 Pro). Zepp OS is a later adapter; Mi Fitness/S1 Active is not compatible with these binaries.
- Price target: EUR/USD 0.99, localized by Google Play.
- Shape: round screens, 450 x 450 logical canvas.
- Modes: interactive and low-power always-on display.
- Languages for the first store release: English and Spanish.

## Information hierarchy

1. Time.
2. Selected spot.
3. Current tide and next high/low.
4. Twelve-hour tide trend.
5. Swell height, period, and direction.
6. Wind speed and direction.
7. Freshness/error state supplied by the companion app.

## Implemented data contract

The watch face exposes five editable complication slots. A separate Wear OS app will supply:

| Slot | Type | Example |
| --- | --- | --- |
| Spot | SHORT_TEXT | MUNDAKA |
| Tide | SHORT_TEXT | 62% UP |
| Next tide | SHORT_TEXT | HIGH 12:42 |
| Surf | SHORT_TEXT | 1.6m 12s NW |
| Wind | SHORT_TEXT | 8kn E |

The provider selects the nearest of 48 worldwide launch spots or keeps a manually pinned travel spot, caches successful responses for 30 minutes, falls back to stale data if the network fails, and never implies navigational or safety-grade accuracy. One language-neutral JSON contract supports both English and Spanish clients.

## Production data architecture

- EOT20 calculates tidal predictions offline under CC BY 4.0.
- The watch displays tide level as a datum-independent percentage of the local model range; raw model elevation remains available in the API.
- Copernicus Marine supplies global swell forecasts from `GLOBAL_ANALYSISFORECAST_WAV_001_027`.
- MET Norway Locationforecast supplies global wind.
- A scheduled job publishes one static JSON file per spot; watches never receive upstream credentials.
- The Wear bundle is configured at build time with `-Ptideglass.dataBaseUrl=https://…`.

## Release gates

- Validate WFF XML and memory footprint.
- Test 12/24-hour time, Spanish/English, ambient mode, and burn-in protection.
- Test on one Samsung and one Xiaomi Wear OS device or emulator profile.
- Verify complication assignment and refresh behavior on physical hardware.
- Confirm the final attribution wording and host the data endpoint/privacy policy.
- Replace debug signing with a private upload key.
- Produce Play Store screenshots and privacy disclosures.
