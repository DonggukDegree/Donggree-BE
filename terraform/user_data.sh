#!/bin/bash
# EC2 최초 부팅 시 1회 실행. 도커·컴포즈·git·스왑을 설치한다.
# 레포 클론/실행은 시크릿(.env) 때문에 SSH 접속 후 수동으로 한다.
set -euxo pipefail
exec > /var/log/user-data.log 2>&1

export DEBIAN_FRONTEND=noninteractive
apt-get update -y
apt-get install -y git

# Docker Engine + docker compose 플러그인 (공식 설치 스크립트)
curl -fsSL https://get.docker.com | sh
usermod -aG docker ubuntu
systemctl enable --now docker

# 스왑 2GB — small(2GB RAM) 인스턴스의 메모리 여유 확보
if [ ! -f /swapfile ]; then
  fallocate -l 2G /swapfile
  chmod 600 /swapfile
  mkswap /swapfile
  swapon /swapfile
  echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi

echo "bootstrap complete"
