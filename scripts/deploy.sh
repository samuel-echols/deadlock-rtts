#!/usr/bin/env bash
# Manual deploy script — run on the EC2 instance to pull latest image and restart.
# Usage: ./scripts/deploy.sh <ecr-image-uri>
# Example: ./scripts/deploy.sh 123456789.dkr.ecr.us-east-1.amazonaws.com/deadlock-meta-history:latest

set -euo pipefail

ECR_IMAGE="${1:?Usage: deploy.sh <ecr-image-uri>}"
APP_DIR="/home/ec2-user/app"

cd "${APP_DIR}"

echo "[$(date -u)] Pulling image: ${ECR_IMAGE}"
AWS_REGION="${AWS_REGION:-us-east-1}"
ECR_REGISTRY=$(echo "${ECR_IMAGE}" | cut -d/ -f1)

aws ecr get-login-password --region "${AWS_REGION}" \
  | docker login --username AWS --password-stdin "${ECR_REGISTRY}"

export ECR_IMAGE
docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d
docker image prune -f

echo "[$(date -u)] Deploy complete"
