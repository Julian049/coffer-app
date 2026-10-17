#!/usr/bin/env sh
# Sync brand and mascot images from ../docs/08-ui/icons into app resources (see sync_brand_assets.py).
# PYTHON must have Pillow with WebP, e.g. PYTHON=python.exe from WSL when Pillow lives on Windows.
set -e
cd "$(dirname "$0")"
exec "${PYTHON:-python3}" sync_brand_assets.py
