#!/usr/bin/env bash
# --seed activates the "seed" Spring profile (DataSeeder), which wipes and
# repopulates the database with the full demo dataset on startup. Without it,
# the server starts normally with no seeding.
set -euo pipefail

profile=""
cmd=(docker compose up --build)
for arg in "$@"; do
    case "$arg" in
        --seed)
            profile="seed"
            ;;
        *)
            cmd+=("$arg")
            ;;
    esac
done

SPRING_PROFILES_ACTIVE="$profile" "${cmd[@]}"
