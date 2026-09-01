import json
import re
from datetime import datetime, timedelta, timezone
from pathlib import Path

from tideglass_pipeline.contract import Spot, TideReading, WaveReading, WindReading, document
from tideglass_pipeline.generate import generate, load_spots
from tideglass_pipeline.tides import _analyse

ROOT = Path(__file__).resolve().parents[2]


def test_contract_is_bilingual_neutral_and_android_compatible():
    now = datetime(2026, 9, 1, 12, tzinfo=timezone.utc)
    payload = document(
        Spot("mundaka", "MUNDAKA", 43.4075, -2.6988, "Spain"),
        TideReading(1.23, "RISING", "HIGH", now + timedelta(hours=2), 2.1),
        WaveReading(1.6, 12.0, 310.0, now),
        WindReading(8.0, 90.0, now),
        now,
    )
    assert payload["schemaVersion"] == 1
    assert payload["tide"]["next"]["type"] == "HIGH"
    assert payload["swell"]["periodSeconds"] == 12.0
    assert len(payload["attribution"]) == 3


def test_tide_analysis_finds_next_high():
    now = datetime(2026, 9, 1, 12, tzinfo=timezone.utc)
    times = [now + timedelta(hours=i) for i in range(5)]
    result = _analyse(times, [0.5, 1.0, 1.5, 1.0, 0.5], now)
    assert result.trend == "RISING"
    assert result.next_type == "HIGH"
    assert result.next_at == times[2]


def test_demo_generates_every_catalogue_spot(tmp_path):
    spots_path = ROOT / "pipeline" / "spots.json"
    count = generate(spots_path, tmp_path, tmp_path / "models", demo=True)
    spots = load_spots(spots_path)
    assert count == 48
    assert len(list((tmp_path / "v1" / "spots").glob("*.json"))) == len(spots)
    assert json.loads((tmp_path / "v1" / "spots" / "mundaka.json").read_text())["spot"]["id"] == "mundaka"


def test_android_and_pipeline_catalogues_have_same_ids():
    kotlin = (ROOT / "marine-provider" / "src" / "main" / "java" / "com" / "tideglass" / "surf" / "provider" / "data" / "SpotCatalog.kt").read_text()
    android_ids = set(re.findall(r'SurfSpot\("([a-z0-9-]+)"', kotlin))
    pipeline_ids = {spot.id for spot in load_spots(ROOT / "pipeline" / "spots.json")}
    assert android_ids == pipeline_ids
