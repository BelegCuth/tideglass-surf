from __future__ import annotations

from datetime import datetime, timedelta, timezone

from .contract import Spot, WaveReading

DATASET_ID = "cmems_mod_glo_wav_anfc_0.083deg_PT3H-i"


def copernicus_wave(spot: Spot, now: datetime) -> WaveReading:
    import copernicusmarine
    import pandas as pd

    frame = copernicusmarine.read_dataframe(
        dataset_id=DATASET_ID,
        variables=["VHM0_SW1", "VTM01_SW1", "VMDR_SW1", "VHM0", "VTPK", "VMDR"],
        minimum_longitude=spot.longitude,
        maximum_longitude=spot.longitude,
        minimum_latitude=spot.latitude,
        maximum_latitude=spot.latitude,
        start_datetime=now.astimezone(timezone.utc) - timedelta(hours=3),
        end_datetime=now.astimezone(timezone.utc) + timedelta(hours=24),
        coordinates_selection_method="nearest",
    ).reset_index()
    if frame.empty:
        raise ValueError(f"No Copernicus wave data for {spot.id}")
    time_column = next(name for name in frame.columns if str(name).lower() == "time")
    frame[time_column] = pd.to_datetime(frame[time_column], utc=True)
    index = (frame[time_column] - pd.Timestamp(now)).abs().idxmin()
    row = frame.loc[index]

    def first(*names: str) -> float:
        for name in names:
            if name in row and pd.notna(row[name]):
                return float(row[name])
        raise ValueError(f"Missing Copernicus variables: {names}")

    return WaveReading(
        height=first("VHM0_SW1", "VHM0"),
        period=first("VTM01_SW1", "VTPK"),
        direction=first("VMDR_SW1", "VMDR"),
        valid_at=row[time_column].to_pydatetime(),
    )
