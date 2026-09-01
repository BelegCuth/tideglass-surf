from __future__ import annotations

import argparse
import json
from datetime import datetime, timedelta, timezone
from pathlib import Path
from typing import Any


def _time(value: Any, field: str) -> datetime:
    if not isinstance(value, str):
        raise ValueError(f"{field} must be an ISO-8601 string")
    parsed = datetime.fromisoformat(value.replace("Z", "+00:00"))
    if parsed.tzinfo is None:
        raise ValueError(f"{field} must include a timezone")
    return parsed.astimezone(timezone.utc)


def _number(value: Any, field: str, minimum: float, maximum: float) -> None:
    if isinstance(value, bool) or not isinstance(value, (int, float)):
        raise ValueError(f"{field} must be numeric")
    if not minimum <= float(value) <= maximum:
        raise ValueError(f"{field} is outside {minimum}..{maximum}: {value}")


def validate_public(output: Path, *, now: datetime | None = None, max_age_hours: float = 8) -> int:
    now = (now or datetime.now(timezone.utc)).astimezone(timezone.utc)
    index_path = output / "v1" / "index.json"
    index = json.loads(index_path.read_text(encoding="utf-8"))
    if index.get("schemaVersion") != 1:
        raise ValueError("Unsupported index schemaVersion")

    generated_at = _time(index.get("generatedAt"), "index.generatedAt")
    if generated_at < now - timedelta(hours=max_age_hours) or generated_at > now + timedelta(minutes=15):
        raise ValueError("The catalogue generation time is stale or in the future")

    spots = index.get("spots")
    if not isinstance(spots, list) or not spots:
        raise ValueError("The catalogue must contain at least one spot")
    ids = [spot.get("id") for spot in spots if isinstance(spot, dict)]
    if len(ids) != len(spots) or len(set(ids)) != len(ids) or any(not value for value in ids):
        raise ValueError("Spot identifiers must be present and unique")

    for spot_id in ids:
        payload = json.loads((output / "v1" / "spots" / f"{spot_id}.json").read_text(encoding="utf-8"))
        if payload.get("schemaVersion") != 1 or payload.get("spot", {}).get("id") != spot_id:
            raise ValueError(f"Invalid contract for {spot_id}")
        spot_generated = _time(payload.get("generatedAt"), f"{spot_id}.generatedAt")
        valid_at = _time(payload.get("validAt"), f"{spot_id}.validAt")
        next_tide = _time(payload.get("tide", {}).get("next", {}).get("at"), f"{spot_id}.tide.next.at")
        if abs((spot_generated - generated_at).total_seconds()) > 1:
            raise ValueError(f"Mismatched generation time for {spot_id}")
        if valid_at < now - timedelta(hours=max_age_hours) or valid_at > now + timedelta(hours=6):
            raise ValueError(f"Forecast time is stale or implausible for {spot_id}")
        if not now - timedelta(minutes=30) < next_tide < now + timedelta(hours=20):
            raise ValueError(f"Next tide is outside the expected window for {spot_id}")

        _number(payload["tide"]["heightMeters"], f"{spot_id}.tide.heightMeters", -20, 20)
        _number(payload["tide"]["next"]["heightMeters"], f"{spot_id}.tide.next.heightMeters", -20, 20)
        _number(payload["swell"]["heightMeters"], f"{spot_id}.swell.heightMeters", 0, 30)
        _number(payload["swell"]["periodSeconds"], f"{spot_id}.swell.periodSeconds", 0, 40)
        _number(payload["swell"]["directionDegrees"], f"{spot_id}.swell.directionDegrees", 0, 360)
        _number(payload["wind"]["speedKnots"], f"{spot_id}.wind.speedKnots", 0, 200)
        _number(payload["wind"]["directionDegrees"], f"{spot_id}.wind.directionDegrees", 0, 360)
        if len(payload.get("attribution", [])) < 3:
            raise ValueError(f"Missing source attribution for {spot_id}")

    return len(spots)


def main() -> None:
    parser = argparse.ArgumentParser(description="Validate Tideglass data before deployment")
    parser.add_argument("--output", type=Path, default=Path("public"))
    parser.add_argument("--max-age-hours", type=float, default=8)
    args = parser.parse_args()
    print(f"Validated {validate_public(args.output, max_age_hours=args.max_age_hours)} spots")


if __name__ == "__main__":
    main()
