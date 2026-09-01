from __future__ import annotations

import argparse
import json
from datetime import datetime, timedelta, timezone
from pathlib import Path

from .contract import Spot, TideReading, WaveReading, WindReading, document, iso
from .tides import eot20_tides
from .waves import copernicus_wave
from .wind import met_norway_wind


def load_spots(path: Path) -> list[Spot]:
    return [Spot(**item) for item in json.loads(path.read_text(encoding="utf-8"))]


def demo_readings(spot: Spot, now: datetime) -> tuple[TideReading, WaveReading, WindReading]:
    phase = (abs(spot.latitude) + abs(spot.longitude)) % 6
    tide = TideReading(0.9 + phase / 10, "RISING", "HIGH", now + timedelta(hours=3), 1.8 + phase / 10)
    wave = WaveReading(1.1 + phase / 12, 9 + phase / 2, (spot.longitude + 360) % 360, now)
    wind = WindReading(6 + phase, (spot.longitude + 45 + 360) % 360, now)
    return tide, wave, wind


def generate(spots_path: Path, output: Path, model_dir: Path, demo: bool = False, now: datetime | None = None) -> int:
    now = (now or datetime.now(timezone.utc)).astimezone(timezone.utc)
    destination = output / "v1" / "spots"
    destination.mkdir(parents=True, exist_ok=True)
    spots = load_spots(spots_path)
    tides = {} if demo else eot20_tides(spots, now, model_dir)
    index = []
    for spot in spots:
        tide, wave, wind = (
            demo_readings(spot, now)
            if demo
            else (tides[spot.id], copernicus_wave(spot, now), met_norway_wind(spot, now))
        )
        payload = document(spot, tide, wave, wind, now)
        target = destination / f"{spot.id}.json"
        temporary = target.with_suffix(".tmp")
        temporary.write_text(json.dumps(payload, ensure_ascii=False, separators=(",", ":")) + "\n", encoding="utf-8")
        temporary.replace(target)
        index.append({"id": spot.id, "name": spot.name, "region": spot.region, "latitude": spot.latitude, "longitude": spot.longitude})
    (output / "v1" / "index.json").write_text(
        json.dumps({"schemaVersion": 1, "generatedAt": iso(now), "spots": index}, ensure_ascii=False, separators=(",", ":")) + "\n",
        encoding="utf-8",
    )
    return len(spots)


def main() -> None:
    parser = argparse.ArgumentParser(description="Publish Tideglass marine JSON")
    parser.add_argument("--spots", type=Path, default=Path("spots.json"))
    parser.add_argument("--output", type=Path, default=Path("public"))
    parser.add_argument("--model-dir", type=Path, default=Path("tide_models"))
    parser.add_argument("--demo", action="store_true", help="Generate deterministic samples without APIs or tide files")
    args = parser.parse_args()
    print(f"Generated {generate(args.spots, args.output, args.model_dir, args.demo)} spots")


if __name__ == "__main__":
    main()
