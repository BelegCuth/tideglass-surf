from __future__ import annotations

from datetime import datetime, timedelta, timezone
from pathlib import Path

from .contract import Spot, TideReading


def _analyse(times: list[datetime], heights: list[float], now: datetime) -> TideReading:
    if len(times) < 3 or len(times) != len(heights):
        raise ValueError("At least three aligned tide samples are required")
    current = min(range(len(times)), key=lambda i: abs((times[i] - now).total_seconds()))
    before, after = max(0, current - 1), min(len(times) - 1, current + 1)
    delta = heights[after] - heights[before]
    trend = "RISING" if delta > 0.005 else "FALLING" if delta < -0.005 else "STEADY"
    for i in range(max(1, current + 1), len(times) - 1):
        if heights[i] > heights[i - 1] and heights[i] >= heights[i + 1]:
            return TideReading(heights[current], trend, "HIGH", times[i], heights[i])
        if heights[i] < heights[i - 1] and heights[i] <= heights[i + 1]:
            return TideReading(heights[current], trend, "LOW", times[i], heights[i])
    raise ValueError("No future tidal extremum found")


def eot20_tide(spot: Spot, now: datetime, model_dir: Path) -> TideReading:
    return eot20_tides([spot], now, model_dir)[spot.id]


def eot20_tides(spots: list[Spot], now: datetime, model_dir: Path) -> dict[str, TideReading]:
    import pandas as pd
    from eo_tides.model import model_tides

    start = now.astimezone(timezone.utc) - timedelta(hours=1)
    times = pd.date_range(start=start, end=start + timedelta(hours=32), freq="15min", tz="UTC")
    predicted = model_tides(
        x=[spot.longitude for spot in spots],
        y=[spot.latitude for spot in spots],
        time=times.tz_localize(None),
        model="EOT20",
        directory=str(model_dir),
        mode="one-to-many",
        parallel=False,
    ).reset_index()
    predicted["time"] = pd.to_datetime(predicted["time"], utc=True)
    python_times = [stamp.to_pydatetime() for stamp in times]
    readings = {}
    for spot in spots:
        rows = predicted[(predicted["x"] == spot.longitude) & (predicted["y"] == spot.latitude)].sort_values("time")
        if len(rows) != len(times):
            raise ValueError(f"Incomplete EOT20 tide series for {spot.id}")
        values = [float(value) for value in rows["tide_height"]]
        readings[spot.id] = _analyse(python_times, values, now.astimezone(timezone.utc))
    return readings
