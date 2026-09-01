#!/usr/bin/env bash
# Always activates the "dev" Spring profile (application-dev.properties). Add --seed to also
# activate the "seed" profile (DataSeeder), which wipes and repopulates the database with the
# full demo dataset on startup. Without --seed, the server starts normally with no seeding.
set -euo pipefail

profiles="dev"
cmd=(docker compose up --build)
for arg in "$@"; do
    case "$arg" in
        --seed)
            profiles="dev,seed"
            ;;
        *)
            cmd+=("$arg")
            ;;
    esac
done

SPRING_PROFILES_ACTIVE="$profiles" "${cmd[@]}"
