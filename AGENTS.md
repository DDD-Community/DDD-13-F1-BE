# AGENTS.md

## 기본 응답

- 항상 한국어로 답변
- 사용자 운영체제는 macOS Apple Silicon 기준
- 명령어와 경로는 macOS 기준 안내

## 작업 방식

- 기존 코드 스타일 우선
- 변경 범위 최소화
- 불필요한 리팩터링 지양
- 새 작업 브랜치는 `develop` 기준 분기

## 주석 작성

- 주석은 핵심만 한국어로 작성
- 문장형 설명보다 명사형 표현 사용
- 서술형 동사 지양
- 주석 끝 온점 금지
- 자명한 코드 주석 생략

## Git 규칙

### 브랜치명

- 형식: `type/#issue-number-description`
- 예시: `docs/#13-agents-md-guideline`
- 예시: `chore/#123-cicd-pipeline`
- `issue-number`는 GitHub 이슈 번호 기준
- `type`은 `feat`, `fix`, `refactor`, `docs`, `chore`, `test` 중 하나 사용
- `description`은 영어 kebab-case 사용
- macOS zsh에서 `#`이 주석으로 처리될 수 있으므로 브랜치명은 따옴표로 감싸기

### 커밋 메시지

- 형식: `type: 변경 내용`
- 예시: `docs: AGENTS.md 지침 추가`
- `type`은 브랜치명과 동일한 lowercase 사용
- 메시지는 한국어로 핵심만 작성
- 문장형보다 명사형 표현 사용
- 문장 끝 온점 금지

### 이슈 및 PR 제목

- 형식: `[Type] 제목`
- 예시: `[Docs] AGENTS.md 지침 추가`
- `Type`은 `Feat`, `Fix`, `Refactor`, `Docs`, `Chore`, `Test` 중 하나 사용
- 제목은 한국어로 핵심만 작성
- 커밋 메시지 형식과 구분

### 이슈 및 PR 본문

- 이슈 본문은 `.github/ISSUE_TEMPLATE.md` 기준 작성
- PR 본문은 `.github/PULL_REQUEST_TEMPLATE.md` 기준 작성
- 연결 이슈가 있는 PR은 `Closes #이슈번호` 포함
