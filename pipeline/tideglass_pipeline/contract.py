from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, timezone
from typing import Any


def iso(value: datetime) -> str:
    return value.astimezone(timezone.utc).isoformat(timespec="seconds").replace("+00:00", "Z")


@dataclass(frozen=True)
class Spot:
    id: str
    name: str
    latitude: float
    longitude: float
    region: str


@dataclass(frozen=True)
class TideReading:
    height: float
    trend: str
    next_type: str
    next_at: datetime
    next_height: float


@dataclass(frozen=True)
class WaveReading:
    height: float
    period: float
    direction: float
    valid_at: datetime


@dataclass(frozen=True)
class WindReading:
    speed_knots: float
    direction: float
    valid_at: datetime


def document(spot: Spot, tide: TideReading, wave: WaveReading, wind: WindReading, generated_at: datetime) -> dict[str, Any]:
    return {
        "schemaVersion": 1,
        "spot": {"id": spot.id, "name": spot.name, "latitude": spot.latitude, "longitude": spot.longitude},
        "generatedAt": iso(generated_at),
        "validAt": iso(min(wave.valid_at, wind.valid_at)),
        "tide": {
            "heightMeters": round(tide.height, 2),
            "trend": tide.trend,
            "next": {"type": tide.next_type, "at": iso(tide.next_at), "heightMeters": round(tide.next_height, 2)},
        },
        "swell": {
            "heightMeters": round(wave.height, 2),
            "periodSeconds": round(wave.period, 1),
            "directionDegrees": round(wave.direction, 1),
        },
        "wind": {"speedKnots": round(wind.speed_knots, 1), "directionDegrees": round(wind.direction, 1)},
        "waterTemperatureCelsius": None,
        "attribution": ["EOT20 / CC BY 4.0", "Copernicus Marine Service", "MET Norway"],
    }
