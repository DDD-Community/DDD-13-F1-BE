# Quiket Backend

## 📝 프로젝트 소개

강의 노트를 업로드하면 AI가 퀴즈를 자동 생성해주는 학습 보조 앱 백엔드 서버

## 🛠 기술 스택

- **Language**: Java 17
- **Framework**: Spring Boot 4.0.5
- **Database**: MySQL, Redis
- **Build Tool**: Gradle
- **Infra**: AWS EC2
- **CI/CD**: GitHub Actions

## 🔄 CI/CD

- `ci.yml`
  - 대상: `develop`, `main` 대상 PR
  - 역할: Gradle build, test
- `deploy.yml`
  - 대상: `main` push
  - 역할: Docker build, GHCR push, EC2 배포

## 🔐 GitHub Secrets

- `PROD_ENV_FILE`: 운영 환경변수 `.env` 전체
- `EC2_HOST`: 운영 EC2 호스트
- `EC2_USER`: 운영 EC2 유저
- `EC2_SSH_KEY`: 운영 EC2 SSH private key
- `EC2_PORT`: 운영 EC2 SSH 포트
- `EC2_DEPLOY_PATH`: 운영 배포 디렉터리 경로
- `GHCR_USERNAME`: GHCR pull 계정명
- `GHCR_TOKEN`: GHCR pull 토큰
