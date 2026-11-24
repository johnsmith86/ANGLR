# ANGLR 🎣
Local fishing intelligence for Australian anglers.

## Android build
The GitHub Actions workflow builds a debug APK automatically on pushes to `main`.
Open **Actions → Android APK build → latest run → Artifacts** to download the APK.

## MVP status
- Android app scaffold and ANGLR home screen
- Runtime location permission prompt
- Fishing intel dashboard UI for bite windows, tide, moon, nearby spots, conditions, rigs and reports
- APK build pipeline

## Next integration milestones
1. Real GPS coordinates and nearby spot database/map
2. Live tide predictions and moon data
3. Forecast provider integration (wind, swell, rain, pressure)
4. Multi-source fishing reports with source timestamps and confidence scoring
5. Species-specific tackle and rig recommendations
6. Offline saved spots and trip notes

The current snapshot labels are UI placeholders, not live forecasts. Connect data providers before using the app for real-time fishing decisions.

Latest Android build includes GPS-based nearby spot search and encrypted WorldTides key setup.
