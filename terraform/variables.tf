variable "aws_region" {
  description = "배포 리전 (서울 = ap-northeast-2)"
  type        = string
  default     = "ap-northeast-2"
}

variable "name" {
  description = "리소스 이름 접두사"
  type        = string
  default     = "donggree"
}

variable "instance_type" {
  description = "EC2 인스턴스 타입. 풀스택(8컨테이너)은 t3.small(2GB) 이상 권장."
  type        = string
  default     = "t3.small"
}

variable "root_volume_gb" {
  description = "루트 디스크 크기(GB). 도커 이미지+DB 데이터 여유분 포함."
  type        = number
  default     = 30
}

variable "admin_cidr" {
  description = "SSH·백엔드·Grafana 접근을 허용할 CIDR. 반드시 본인 IP/32 로 제한할 것 (예: 1.2.3.4/32). 확인: curl ifconfig.me"
  type        = string
  # 기본값 없음 — 반드시 명시하도록 강제 (0.0.0.0/0 전체개방 방지)
}

variable "ssh_public_key" {
  description = "EC2 접속용 SSH 공개키 내용. 확인: cat ~/.ssh/id_ed25519.pub (또는 id_rsa.pub)"
  type        = string
  # 기본값 없음 — 반드시 명시
}
