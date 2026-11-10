output "public_ip" {
  description = "EC2 퍼블릭 IP (stop/start 시 변경됨)"
  value       = aws_instance.app.public_ip
}

output "ssh_command" {
  description = "SSH 접속 명령"
  value       = "ssh ubuntu@${aws_instance.app.public_ip}"
}

output "backend_url" {
  value = "http://${aws_instance.app.public_ip}:8080"
}

output "grafana_url" {
  value = "http://${aws_instance.app.public_ip}:3001"
}

output "next_steps" {
  description = "배포 후 할 일"
  value       = <<-EOT

    1) 접속:        ssh ubuntu@${aws_instance.app.public_ip}
    2) 레포 클론:    git clone <repo-url> app && cd app
    3) .env 작성:    DEPLOY.md 참고 (JWT_SECRET, KAKAO 키 등)
    4) 기동:        docker compose up -d --build
    5) 확인:        Backend  http://${aws_instance.app.public_ip}:8080
                    Grafana  http://${aws_instance.app.public_ip}:3001
    (부팅 직후 도커 설치가 끝나기까지 1~2분 걸릴 수 있음: tail -f /var/log/user-data.log)
  EOT
}
