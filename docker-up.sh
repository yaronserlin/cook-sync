#!/usr/bin/env bash
# Thin wrapper around `docker compose up --build` that adds a --seed flag.
#
# --seed activates the "seed" Spring profile (DataSeeder), which wipes and
# repopulates the database with the full demo dataset on startup. Without it,
# the server starts normally with no seeding.
#
# Usage:
#   ./docker-up.sh            # normal startup
#   ./docker-up.sh --seed     # startup with the DB reset and reseeded
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
