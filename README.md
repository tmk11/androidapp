# Doanh Thu Nail 💅 — Nail salon revenue (cloud sync)

An Android app for a nail salon to record daily revenue per technician
(**Ha, David, Tu, Anh**) in **Euro (€)**. Data is stored on a **Supabase**
backend and **synced across all devices** (e.g. one phone per technician), with
a local SQLite cache so the app keeps working offline and syncs when back online.

## Features

- **Shared-password access** — the first device sets a shop password; every
  other device must enter it. Data is locked server-side (Postgres row-level
  security); all reads/writes go through password-checked database functions, so
  the embedded API key alone cannot read the data.
- **Offline-first** — entries are written to a local cache immediately and shown
  even with no network; a background sync pushes/pulls changes (deduplicated by a
  per-entry UUID) when online. Sync runs on open, after each change, every 30s,
  and via the **↻ Đồng bộ** button.
- **Per-day entry** — pick a day, add amounts per technician (collapsible
  per-customer chips), see per-tech and grand totals, delete with confirmation.
- **Reports** — totals per technician for a chosen day, a chosen month, or all
  time, each shareable as text via the Android share sheet.

## Architecture

```
Phone (app)
  ├─ SQLite cache (offline-first, source of truth for the UI)
  ├─ SyncManager  ── push pending / pull all ──►  Supabase
  └─ Api (OkHttp) ── HTTPS RPC w/ shared password ─┘
Supabase (Postgres)
  ├─ nail_entries        (RLS locked; no direct anon access)
  ├─ app_config          (bcrypt password hash)
  └─ functions: app_init_password / app_check_password /
                app_add_entry / app_delete_entry / app_fetch_all
```

Backend connection lives in `Backend.kt` (project URL + public anon key). The
shared password is verified server-side and never stored in plaintext.

## Key files

```
app/src/main/java/com/example/simpleapp/
  GateActivity.kt   # first-run: set or enter the shop password
  MainActivity.kt   # day entry screen (offline-first) + sync status
  ReportActivity.kt # day / month / all-time reports + share
  NailDb.kt         # local SQLite cache with sync flags (synced/deleted)
  Api.kt            # OkHttp client for Supabase RPC
  SyncManager.kt    # push local changes, pull remote changes
  Prefs.kt          # stores the password locally
  Money.kt, Dates.kt
```

## Building the APK

GitHub Actions builds the APK on every push (`.github/workflows/android-build.yml`).
Download `app-debug.apk` from the **`debug-latest`** release or the
**`app-debug-apk`** artifact, then install on each technician's phone and enter
the shared shop password.

> Uses AndroidX/Material + OkHttp; built on GitHub's runners (which can reach
> Google's Maven). The backend is a Supabase project; data is private behind the
> shared password.
