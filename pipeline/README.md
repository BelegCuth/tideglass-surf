# Worldwide data publisher

The publisher creates language-neutral JSON. English and Spanish belong in the clients, so adding languages never duplicates forecasts.

## Sources and commercial use

- **Tides:** EOT20 harmonic model, CC BY 4.0. Tide heights are predicted locally from its NetCDF constituents.
- **Waves:** Copernicus Marine `GLOBAL_ANALYSISFORECAST_WAV_001_027`, dataset `cmems_mod_glo_wav_anfc_0.083deg_PT3H-i`.
- **Wind:** MET Norway Locationforecast 2.0 compact endpoint.

Keep the three attributions visible in the app/store listing and comply with provider caching and identification rules. Source forecasts are not safety- or navigation-grade.

## JSON contract

Each `public/v1/spots/{id}.json` contains schema version, spot, generation/valid timestamps, current tide and trend, next high/low, swell and wind. `public/v1/index.json` contains the 48-spot catalogue. Water temperature is intentionally `null` until a compatible open source is added.

## Operations

Run four times daily. Serve `public` as immutable static files with a short CDN cache (around 15–30 minutes). Never place Copernicus credentials in Android, JavaScript or public files. Monitor generator failures and retain the last successful files: stale data is preferable to replacing the whole catalogue with an error response.

The included workflow produces an artifact but deliberately does not activate publishing. Connect it to GitHub Pages, Cloudflare Pages or another static host under an account you control.
