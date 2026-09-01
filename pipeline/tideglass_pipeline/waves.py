from __future__ import annotations

import math
from datetime import datetime, timedelta, timezone

from .contract import Spot, WaveReading

DATASET_ID = "cmems_mod_glo_wav_anfc_0.083deg_PT3H-i"
COASTAL_SEARCH_RADIUS_DEGREES = 0.35


def _first_value(row, *names: str) -> float:
    import pandas as pd

    for name in names:
        if name in row and pd.notna(row[name]):
            return float(row[name])
    raise ValueError(f"Missing Copernicus variables: {names}")


def _select_nearest_sea_row(frame, spot: Spot, now: datetime):
    import pandas as pd

    if frame.empty:
        raise ValueError(f"No Copernicus wave data for {spot.id}")
    time_column = next(name for name in frame.columns if str(name).lower() == "time")
    latitude_column = next(name for name in frame.columns if str(name).lower() == "latitude")
    longitude_column = next(name for name in frame.columns if str(name).lower() == "longitude")
    frame[time_column] = pd.to_datetime(frame[time_column], utc=True)

    height_columns = [name for name in ("VHM0_SW1", "VHM0") if name in frame]
    period_columns = [name for name in ("VTM01_SW1", "VTPK") if name in frame]
    direction_columns = [name for name in ("VMDR_SW1", "VMDR") if name in frame]
    if not height_columns or not period_columns or not direction_columns:
        raise ValueError(f"Unexpected Copernicus columns for {spot.id}: {list(frame.columns)}")

    valid = frame[
        frame[height_columns].notna().any(axis=1)
        & frame[period_columns].notna().any(axis=1)
        & frame[direction_columns].notna().any(axis=1)
    ].copy()
    if valid.empty:
        raise ValueError(f"No valid Copernicus sea cell near {spot.id}")

    valid["_time_distance"] = (valid[time_column] - pd.Timestamp(now)).abs()
    nearest_time = valid["_time_distance"].min()
    valid = valid[valid["_time_distance"] == nearest_time].copy()
    latitude_scale = max(0.2, abs(math.cos(math.radians(spot.latitude))))
    valid["_space_distance"] = (
        (valid[latitude_column] - spot.latitude) ** 2
        + ((valid[longitude_column] - spot.longitude) * latitude_scale) ** 2
    )
    return valid.loc[valid["_space_distance"].idxmin()], time_column


def copernicus_wave(spot: Spot, now: datetime) -> WaveReading:
    import copernicusmarine

    frame = copernicusmarine.read_dataframe(
        dataset_id=DATASET_ID,
        variables=["VHM0_SW1", "VTM01_SW1", "VMDR_SW1", "VHM0", "VTPK", "VMDR"],
        minimum_longitude=spot.longitude - COASTAL_SEARCH_RADIUS_DEGREES,
        maximum_longitude=spot.longitude + COASTAL_SEARCH_RADIUS_DEGREES,
        minimum_latitude=spot.latitude - COASTAL_SEARCH_RADIUS_DEGREES,
        maximum_latitude=spot.latitude + COASTAL_SEARCH_RADIUS_DEGREES,
        start_datetime=now.astimezone(timezone.utc) - timedelta(hours=3),
        end_datetime=now.astimezone(timezone.utc) + timedelta(hours=24),
        coordinates_selection_method="nearest",
    ).reset_index()
    row, time_column = _select_nearest_sea_row(frame, spot, now)

    return WaveReading(
        height=_first_value(row, "VHM0_SW1", "VHM0"),
        period=_first_value(row, "VTM01_SW1", "VTPK"),
        direction=_first_value(row, "VMDR_SW1", "VMDR"),
        valid_at=row[time_column].to_pydatetime(),
    )
