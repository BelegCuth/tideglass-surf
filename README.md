# Tideglass Surf

Commercial-ready Wear OS surf watch face and worldwide marine-data pipeline, localized in English and Spanish.

## What is included

- `watchface`: WFF v2 face with five editable complication slots, localized static labels and ambient mode.
- `marine-provider`: Wear OS companion that selects the nearest of 48 launch spots or lets the surfer pin one manually, downloads one small static JSON, caches it and supplies the complications.
- `pipeline`: scheduled publisher using EOT20 tides, Copernicus Marine waves and MET Norway wind. A demo mode works without credentials.

The runtime app no longer calls Open-Meteo or embeds third-party credentials. The intended production path has no per-user API fee: calculations run centrally four times per day and the resulting files can be served from a free static host while traffic remains inside its free allowance.

## Build Wear OS

Install Android SDK 36 and JDK 17+ (JDK 21 is supported), then set the HTTPS root where the generated `v1` folder is hosted:

```powershell
.\gradlew.bat :watchface:bundleRelease :marine-provider:bundleRelease `
  -Ptideglass.dataBaseUrl=https://data.tideglass.me
```

Install both packages on Wear OS 5+, open **Tideglass Data**, use GPS or choose one of the 48 spots manually, and assign the five Tideglass complications. Release bundles are still debug-signed; replace that signing configuration before Play Console upload.

## Generate data locally

From `pipeline`, a no-network contract test and sample run is:

```powershell
python -m pytest -q
python -m tideglass_pipeline.generate --demo --output public
```

Production generation needs:

- EOT20 NetCDF files extracted under `pipeline/tide_models/EOT20`;
- `COPERNICUSMARINE_SERVICE_USERNAME` and `COPERNICUSMARINE_SERVICE_PASSWORD`;
- the MET Norway User-Agent identifies Tideglass through `https://tideglass.me`.

Then run:

```powershell
python -m tideglass_pipeline.generate --output public --model-dir tide_models
```

The workflow at `.github/workflows/publish-marine-data.yml` performs the same job every six hours and deploys `pipeline/public` to Cloudflare Pages. Configure only the two Copernicus credentials as GitHub Actions secrets. The workflow downloads and caches the official EOT20 release automatically.

## Platform scope

The current binaries support Wear OS 5+ devices such as recent Samsung Galaxy Watch and Xiaomi Watch 2/2 Pro. Xiaomi Watch S1 Active uses Mi Fitness and a proprietary watch-face format, so it is useful for checking the 466×466 visual concept but cannot run these Wear OS binaries or the live complication provider. Amazfit/Zepp OS requires a separate adapter/package after the Wear OS product is validated.

See [PRODUCT_SPEC.md](PRODUCT_SPEC.md), [PRIVACY.md](PRIVACY.md) and [pipeline/README.md](pipeline/README.md).
