#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
git config --local core.hooksPath .githooks
chmod +x .githooks/*
