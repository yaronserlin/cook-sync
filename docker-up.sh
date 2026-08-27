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
compose_args=()
for arg in "$@"; do
    case "$arg" in
        --seed)
            profile="seed"
            ;;
        *)
            compose_args+=("$arg")
            ;;
    esac
done

SPRING_PROFILES_ACTIVE="$profile" docker compose up --build "${compose_args[@]}"
