from __future__ import annotations

import os
from datetime import datetime, timezone

import requests

from .contract import Spot, WindReading


def met_norway_wind(spot: Spot, now: datetime) -> WindReading:
    contact = os.environ.get("TIDEGLASS_CONTACT", "").strip()
    if not contact:
        raise RuntimeError("TIDEGLASS_CONTACT is required by MET Norway's terms")
    response = requests.get(
        "https://api.met.no/weatherapi/locationforecast/2.0/compact",
        params={"lat": f"{spot.latitude:.4f}", "lon": f"{spot.longitude:.4f}"},
        headers={"User-Agent": f"TideglassSurf/0.1 {contact}", "Accept": "application/json"},
        timeout=20,
    )
    response.raise_for_status()
    series = response.json()["properties"]["timeseries"]
    item = min(series, key=lambda value: abs((datetime.fromisoformat(value["time"].replace("Z", "+00:00")) - now).total_seconds()))
    details = item["data"]["instant"]["details"]
    return WindReading(
        speed_knots=float(details["wind_speed"]) * 1.943844,
        direction=float(details["wind_from_direction"]),
        valid_at=datetime.fromisoformat(item["time"].replace("Z", "+00:00")).astimezone(timezone.utc),
    )
