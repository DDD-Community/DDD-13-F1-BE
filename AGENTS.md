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

## 프로젝트 개발 규칙

### 1. 프로젝트 패키지 구조

```text
com.f1.quiket
 ├─ domain : 기능별 도메인 계층
 │   └─ {기능명}
 │      ├─ controller : HTTP 요청/응답 처리
 │      ├─ service : 비즈니스 로직
 │      ├─ repository : 데이터 저장소 접근
 │      ├─ entity : DB 엔티티 모델
 │      ├─ dto : 요청/응답 데이터 전송 객체
 │      └─ exception : 도메인별 예외 정의 (필요 시)
 ├─ config : Spring Bean 설정 클래스
 ├─ infra : OpenAI, OCR, mail 등 외부 시스템 통신
 ├─ global : 공통 인프라
 │      ├─ auth : Security / JWT
 │      ├─ error : 전역 예외 처리
 │      ├─ response : 공통 응답 형식
 │      └─ util : 공통 유틸
 └─ support : 스케줄러 등 지원 기능
```

### 2. 기능 기반 개발 원칙

- `domain`은 기능 중심으로 분리
- 각 도메인 패키지는 기능 단위로 묶고 내부에서 책임 분리
- DTO와 Entity 명확히 분리
- 공통 인프라와 도메인 로직 분리
- 외부 시스템 연동 로직은 도메인 내부에 직접 두지 않고 기본적으로 `infra` 계층에 분리
- 공통 응답, 공통 예외, 인증/인가, 유틸성 코드는 `global` 하위 관리

### 3. 레이어별 책임

#### 1) Controller

- HTTP 요청을 받는 진입점
- 요청 검증 및 DTO 수신
- `service` 호출 결과를 `ApiResponse`로 래핑
- HTTP 상태 코드와 헤더 결정
- 비즈니스 로직, DB 로직, 외부 API 호출 직접 수행 금지
- 허용: `userService.findActiveUsers()`
- 금지: `userRepository.findAllByStatusAndDeletedAtIsNull("active")` 직접 호출

#### 2) Service

- 도메인 비즈니스 로직 담당
- 트랜잭션 경계 설정(`@Transactional`)
- 여러 Repository, 외부 시스템, 도메인 검증 조합
- 도메인 규칙, 상태 변경, 예외 처리 책임
- 허용: 사용자 활성화 상태 판단, 도메인 예외 던지기
- 금지: `ResponseEntity` 생성, HTTP 요청/응답 처리

#### 3) Repository

- DB 접근 전용 계층
- JPA/Native 쿼리만 구현
- 필요 시 QueryDSL 사용
- 비즈니스 로직이나 DTO 변환 금지
- 엔티티 조회/저장/삭제만 수행
- 허용: `findByEmail`, `save`, `findAllByStatusAndDeletedAtIsNull`
- 금지: 비밀번호 검증, 이메일 인증 상태 변경 같은 비즈니스 판단

#### 4) Entity

- DB 테이블 매핑 모델
- 영속성 속성, 컬럼 매핑, 관계 설정 담당
- 비즈니스 로직은 최소화
- API 응답에 그대로 노출 금지
- 허용: `@Column`, `@Table`, 도메인 상태 변경 메서드(`lockAccount()`)
- 금지: 컨트롤러 응답 구조 정의, 외부 DTO 의존

#### 5) DTO

- 외부 요청/응답 전용 데이터 구조
- 외부에 주고받는 API 데이터 모양 정의
- `from`, `toEntity` 등의 변환 메서드로 레이어 경계 유지
- 비즈니스 로직 포함 금지
- 허용: `UserResponse.from(user)`, `UserCreateRequest`
- 금지: `userRepository.save(this.toEntity())` 같은 저장 로직

### 4. 응답 형식

- API 응답은 공통 응답 객체 `ApiResponse<T>` 사용
- HTTP 상태 코드는 `ResponseEntity`에서 관리
- `ApiResponse.success` 필드는 성공/실패 여부만 표현
- 성공 응답은 `ApiResponse.success(...)` 사용
- 실패 응답은 `ApiResponse.fail(...)` 사용

```java
public class ApiResponse<T> {
    private boolean success;
    private String code;
    private String message;
    private T data;
}
```

```java
return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(SuccessCode.CREATED, response));
```

```java
return ResponseEntity.status(errorCode.getStatus())
        .body(ApiResponse.fail(errorCode));
```

### 5. 에러 처리 규칙

- `GlobalExceptionHandler`에서 공통 예외 처리
- 도메인 예외는 `CustomException`과 `ErrorCode`로 통일
- 예외 응답은 항상 `ApiResponse` 형태로 반환
- HTTP 상태 코드는 `ResponseEntity`에서 관리
- `ApiResponse.success`는 성공/실패 여부만 표현

#### 예외 처리 흐름

1. 서비스/도메인 레이어에서 예외 발생
2. `CustomException` 생성
3. `GlobalExceptionHandler`에서 `ResponseEntity.status(...)`로 HTTP 상태 지정
4. `ApiResponse.fail(errorCode)` 반환

### 6. 코드 스타일 규칙

- `controller` → `service` → `repository` 흐름 유지
- `@Transactional`은 서비스 레이어에 적용
- SQL 컬럼명과 엔티티 필드명 매핑은 `@Column(name = "...")` 명시
- `dto`는 API 통신, `entity`는 DB 저장/조회 담당

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

### 커밋 단위

- 하나의 커밋은 하나의 목적만 가진다 (Atomic Commit)
- 서로 다른 목적의 변경은 논리 단위별로 커밋 분리
- 리뷰와 롤백이 쉬운 단위로 커밋 작성

### 머지 전략

- 작업 브랜치(`type/#issue-number-description`, 예: `feat/#123-login-api`) → `develop`: Squash merge 사용
- `develop` → `main`: Merge commit 사용
- 작업 브랜치 내부 커밋은 리뷰 편의를 위해 원자적으로 작성
- Squash merge 커밋 메시지는 PR 제목 기준으로 작성
- `main` 병합 커밋은 릴리즈 단위 추적 목적

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
