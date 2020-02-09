#!/bin/bash

set -e

echo "Running custom command"
helm dep update --skip-refresh $1