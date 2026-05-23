# CLAUDE.md

동그리(Donggree) 백엔드 프로젝트 작업 지침. 일반적인 코딩 행동 규칙(`~/CLAUDE.md`)에 더해 이 프로젝트에만 적용되는 규칙을 정의한다.

---

## 1. 프로젝트 개요

**Donggree**: PDF 기반 졸업 요건 자동 판정 웹 서비스.
사용자가 nDRIMS에서 받은 "취득교과목 영역별 분류표" PDF를 업로드하면 자동 파싱 → 룰 기반 졸업 요건 판정 → 부족 학점/미이수 과목 시각화한다.

- 현재 MVP 범위: **컴퓨터·AI학부 23학번 기준**
- 본 저장소는 **백엔드 전용** (프론트엔드는 별도 저장소)

---

## 2. 기술 스택

| 영역 | 사용 기술 |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5.13 |
| Modular Monolith | Spring Modulith 1.4.10 |
| ORM | Spring Data JPA |
| DB | PostgreSQL 16 (로컬은 Docker, 5432 포트) |
| 테스트 DB | H2 (in-memory) |
| PDF 파싱 | Apache PDFBox 3.0.3 |
| API 문서 | Springdoc OpenAPI (Swagger) + Spring REST Docs |
| 모니터링 | Actuator + Micrometer Prometheus (추후 Grafana 연동 예정) |
| 빌드 | Gradle |

라이브러리 추가 시 `build.gradle`에 이미 들어있는지 먼저 확인할 것.

---

## 3. 가장 중요한 원칙 — 응집과 결합

**이 프로젝트의 최우선 설계 목표: 각 모듈이 언제든 별도 서비스로 분리될 수 있도록 결합도를 낮게 유지한다.** (지금 분리하지 않더라도 그렇게 가능한 상태로 둔다.)

다른 어떤 규칙보다 우선한다. "조금 더 편하니까" "지금은 모놀리스니까" 하는 이유로 깨면 나중에 MSA 전환 시 비용이 폭발한다.

### 모듈 간에 절대 하지 않는 것
- 다른 모듈의 **엔티티 참조** — `@ManyToOne Member member` ❌ / `Long memberId` ⭕
- 다른 모듈의 **리포지토리 호출**
- 다른 모듈 테이블과의 **JOIN** (네이티브 쿼리, JPQL 모두)
- 다른 모듈의 `internal` 패키지 클래스 import

### 조회가 가장 위험하다
여러 모듈 데이터를 합쳐 보여주는 화면용 API가 가장 깨지기 쉽다. "그냥 JOIN하면 되잖아"는 안 된다. 세 가지 선택지:

1. **컨트롤러 레이어 조합** — 각 모듈 응용 서비스를 호출해 DTO를 컨트롤러에서 합친다. 모듈 격리 유지. **기본 선택지.**
2. **읽기 모델(Read Model) 복제** — 자주 함께 조회되는 데이터를 이벤트로 다른 모듈에 복제해 둔다. 성능이 중요할 때.
3. **조회 전용 인터페이스 노출** — 모듈이 자신의 데이터를 외부에서 조회할 수 있는 응용 서비스 메서드를 명시적으로 노출. 단순 케이스에만.

이 셋 중 어느 것도 "복잡하다"는 이유로 건너뛰고 cross-module JOIN으로 도망가지 않는다. 의심스러우면 1번을 쓴다.

### 쓰기 작업 — 이벤트로
한 사용자 액션이 여러 모듈에 영향을 줄 때는 **동기 호출이 아닌 이벤트**로 연결한다. (자세한 사용법은 §8)

### 모듈 분리 신호
- 모듈 A의 코드가 모듈 B를 너무 자주 호출한다
- 이벤트가 사실상 동기 호출처럼 쓰이고 있다 (이벤트 발행 직후 응답을 기다리는 패턴)
- 두 모듈이 같은 데이터를 동시에 수정한다

→ **모듈 경계를 잘못 그은 것.** 작업을 멈추고 경계 재검토.

---

## 4. 모듈 구조 (Spring Modulith)

4개 모듈로 구성. 패키지는 `com.donggree.<module>` 기준.

| 모듈 | 책임 | 주요 애그리거트 |
|---|---|---|
| `user` | 회원가입, 로그인, 프로필 관리 | `Member` |
| `transcript` | PDF 업로드, 파싱, 이수 정보 저장 | `Transcript` (course_record는 내부 엔티티) |
| `curriculum` | 학과/학번별 졸업 요건 데이터 | `RequirementSet`, `Course`, `Department`, `AreaType` |
| `graduation` | 졸업 요건 판정 비즈니스 로직 | `GraduationReport` |

### 모듈 경계 — 무엇이 공개되고 무엇이 숨겨지는가
`internal`은 **Spring Modulith의 가시성 메커니즘**이다. DDD의 레이어링(presentation/application/domain/infrastructure)과는 다른 차원이며, 둘은 직교(orthogonal)하다. `internal` 안에서 DDD 레이어로 또 나눈다.

| 위치 | 들어가는 것 | 이유 |
|---|---|---|
| `<module>/` (root) | 도메인 이벤트, 공유 DTO/뷰, (선택) 조회용 응용 서비스 인터페이스 | 다른 모듈이 Java 코드로 직접 참조함 |
| `<module>/internal/` | 컨트롤러, 응용 서비스, 엔티티, 리포지토리, 도메인 서비스, VO | 다른 모듈에서 import 불필요. 컨트롤러도 HTTP로만 호출되니 여기에. |

`./gradlew test`의 `ApplicationModules.verify()`가 위반을 자동 검출한다. 위반 시 빌드가 깨진다.

### internal 안의 레이어링 — DDD 레이어
`internal` 안은 DDD 레이어로 패키지를 나눈다 (*도메인 주도 개발 시작하기* 기준):

```
com.donggree.user/
├── UserApi.java                    ← (선택) 다른 모듈용 조회 인터페이스
├── event/
│   └── MemberRegisteredEvent.java  ← 공개 이벤트
├── dto/
│   └── MemberView.java             ← 공개 DTO (조회 결과)
└── internal/
    ├── presentation/
    │   ├── UserController.java
    │   ├── SignupRequest.java
    │   └── SignupResponse.java
    ├── application/
    │   └── MemberService.java
    ├── domain/
    │   ├── Member.java                ← 루트 애그리거트
    │   ├── MemberRepository.java      ← JpaRepository 상속 인터페이스
    │   ├── Role.java                  ← VO/Enum
    │   └── MemberPasswordEncoder.java ← 도메인 서비스 인터페이스
    └── infrastructure/
        └── BCryptPasswordEncoder.java ← 도메인 서비스 구현, 외부 어댑터
```

**참고**
- Spring Data JPA를 쓰므로 리포지토리는 인터페이스만 두고 별도 구현체를 만들지 않는다 (Spring이 런타임에 생성).
- 모듈이 작으면 `internal` 안의 레이어 패키지를 생략하고 평탄하게 둬도 된다. 파일이 늘어나면 그때 분리.
- 컨트롤러 내부 DTO(`SignupRequest`)는 다른 모듈이 쓰지 않으므로 internal에 둔다. 진짜 모듈 간 공유 DTO만 `<module>/dto/`에 둔다.

---

## 5. DDD 원칙

이 프로젝트는 *도메인 주도 개발 시작하기*(최범균) 기반으로 설계한다.

**애그리거트**
- 각 모듈마다 명확한 **루트 애그리거트**를 둔다. 외부에서는 루트를 통해서만 내부 엔티티에 접근한다.
- 예: `Transcript`가 `CourseRecord` 컬렉션을 소유. 외부에서는 `Transcript`를 통해서만 수강 이력에 접근.
- 다른 애그리거트는 **ID로만 참조**한다 (§3과 같은 원칙).

**도메인 로직 위치**
- 비즈니스 규칙은 **엔티티 내부**에 둔다. (예: `Transcript.totalCreditsByArea()`, `GraduationReport.calculateRemainingCredits()`)
- 한 애그리거트로 표현 어려운 규칙은 **도메인 서비스**(`<module>.internal.domain.*`)로 분리. 외부 의존이 필요하면 인터페이스는 domain에, 구현은 infrastructure에.
- 응용 서비스(`<Module>Service`)는 **트랜잭션 경계 + 도메인 객체 조립**만 담당. 비즈니스 로직 넣지 않는다.

**불변식 보호**
- 엔티티 필드 변경은 setter가 아닌 의미 있는 메서드로 노출 (`transcript.markAsParsed(parsedData)`).
- Lombok `@Setter`는 엔티티에 절대 사용하지 않는다. `@Getter`는 허용.

---

## 6. 개발 워크플로

### 모듈 단위로 작업한다
**엔티티만 전부 먼저 만들지 않는다.** 한 모듈을 골라서 그 모듈의 엔티티 → 도메인 → API → 테스트까지 끝내고 다음 모듈로 넘어간다. 이유는 사용 맥락 없이 결정하면 과설계되거나 무의식적으로 모듈 간 결합이 생기기 때문.

**권장 진행 순서** (의존성 적은 것부터):
1. `user` → 2. `curriculum` → 3. `transcript` → 4. `graduation`

### 한 모듈 안에서 API 하나를 추가할 때
1. **엔티티/VO 설계** — ERD 기준 필드 정의. ERD는 `.claude/erd.sql` 참고. 애그리거트 경계와 루트 먼저 결정.
2. **도메인 로직 작성** — 엔티티 메서드 또는 도메인 서비스로 비즈니스 규칙 구현. **단위 테스트**(순수 JUnit, Spring 컨텍스트 없음) 동시 작성.
3. **리포지토리** — Spring Data JPA 인터페이스. `internal` 패키지에.
4. **컨트롤러** — API 명세서 URI/메서드/스키마를 그대로 따른다. 명세서에 없는 API는 만들지 않는다.
5. **응용 서비스** — 컨트롤러가 호출. 도메인 객체 조립과 트랜잭션 관리.
6. **통합 테스트(REST Docs 스니펫 포함)** — §7 참고.
7. **모듈 간 영향이 있다면 이벤트 발행** — §8 참고.

> 컨트롤러나 서비스부터 만들지 않는다. **항상 도메인부터.**
> 코드 작업 후엔 항상 한국어로 이 코드가 어떤 동작인지를 명세하여 협업자도 알 수 있게 한다. 코드의 가독성에 신경 쓴다.

### PR 단위
모듈 단위는 작업 묶음이지 PR 묶음이 아니다. PR은 더 잘게 쪼갠다.

한 모듈을 다음과 같이 분할:
- **PR 1 — 도메인 기반**: 핵심 엔티티, 명백한 불변식을 지키는 도메인 메서드, 도메인 단위 테스트, 리포지토리 인터페이스 껍데기
    - 모든 도메인 메서드를 다 만들지 않는다. 명확한 것만. 나머지는 API PR에서 필요해질 때 추가.
- **PR 2 ~ N — API 단위**: 컨트롤러 + 응용 서비스 + 통합 테스트 + REST Docs. **API 하나당 PR 하나.**
- **PR M — 이벤트 핸들러**: 다른 모듈 이벤트를 처리하는 게 있다면 별도 PR.

### 커밋 단위
워크플로 한 단계당 커밋 하나. 단 **각 커밋은 `./gradlew build` 통과 가능해야 한다.** 컴파일 안 되는 중간 상태로 커밋하지 않는다. 따라서 안쪽(도메인) → 바깥쪽(컨트롤러) → 테스트 순서로 커밋한다.
**커밋과 PR은 직접 하지 않고, 커밋 메시지와 해당 커밋에 포함될 파일 리스트, 그리고 이번 작업에 대한 PR 내용을 응답만 하도록 한다.**
**예시 — 도메인 기반 PR (`[Feat/#3] User 모듈 도메인 기반 구현`)**
```
[Feat/#3] Member 엔티티 추가
[Feat/#3] Member 도메인 로직 및 단위 테스트 추가
[Feat/#3] MemberRepository 추가
```

**예시 — API PR (`[Feat/#4] 회원가입 API 구현`)**
```
[Feat/#4] 회원가입 응용 서비스 구현
[Feat/#4] 회원가입 컨트롤러 및 DTO 추가
[Feat/#4] 회원가입 통합 테스트 및 REST Docs 추가
```

API PR에서 도메인 메서드 추가가 필요하면 그 PR의 첫 커밋(응용 서비스 구현)에 포함시킨다.

---

## 7. 테스트 정책

**테스트는 코드와 함께 작성한다. 빠진 채로 푸시하지 않는다.**

| 테스트 종류 | 어노테이션 | 용도 |
|---|---|---|
| 도메인 단위 테스트 | (순수 JUnit) | 엔티티/도메인 서비스 로직 검증. 가장 많아야 함. |
| 모듈 테스트 | `@ApplicationModuleTest` | 모듈 슬라이스 + 이벤트 발행/수신 검증 |
| 컨트롤러 테스트 | `@WebMvcTest` + `MockMvc` + REST Docs | API 계약 검증 + 문서 스니펫 생성 |
| 전체 통합 | `@SpringBootTest` | 꼭 필요한 시나리오에만 (예: PDF 업로드 end-to-end) |

**REST Docs 스니펫은 컨트롤러 테스트에서 반드시 생성한다.** `build/generated-snippets/`에 떨어지고 `asciidoctor` 태스크가 HTML로 묶는다. 새 API 추가 시 `src/docs/asciidoc/index.adoc`에 `include`도 추가.

**Swagger와 REST Docs 역할 분담**
- Swagger UI: 개발 중 빠른 탐색/시도용 (`/swagger-ui.html`)
- REST Docs: 검증된 최종 명세 문서 (`/docs/index.html`). 테스트 통과한 요청/응답만 기록됨.

---

## 8. 모듈 간 통신 — 이벤트 + Outbox

모듈끼리는 **메서드 호출이 아닌 도메인 이벤트로 통신**한다.

**발행 (Publisher 쪽)**
```java
ApplicationEventPublisher events;
events.publishEvent(new TranscriptParsedEvent(transcriptId, memberId));
```

**수신 (Subscriber 쪽, 다른 모듈)**
```java
@ApplicationModuleListener  // = @Async + @TransactionalEventListener + @Transactional
void on(TranscriptParsedEvent event) { ... }
```

**Outbox는 별도 구현하지 않는다.** `spring-modulith-starter-jpa`가 이미 들어있어서 `@ApplicationModuleListener`로 받는 이벤트는 자동으로 `event_publication` 테이블에 기록 → 트랜잭션 커밋 후 비동기 발행 → 성공 시 completion 마킹. 실패 이벤트는 재시도 가능.

**이벤트 네이밍**: 과거형, 발행 모듈 패키지에 둔다.
- 예: `TranscriptParsedEvent`, `GraduationReportGeneratedEvent`

**이벤트 페이로드**: ID와 최소 정보만. 엔티티/리치 객체를 넣지 않는다. 수신 모듈이 추가 정보가 필요하면 발행 모듈의 조회 API를 통해 가져온다. 페이로드가 커지면 결합이 늘어난다.

---

## 9. 코드 품질 게이트

**커밋 전에 반드시 `./gradlew build` 통과.** 이 명령은 다음을 모두 포함:
- 컴파일
- 모든 테스트 실행
- Spring Modulith 모듈 검증
- REST Docs Asciidoctor 빌드
- 부트 JAR 패키징

```bash
./gradlew build         # 푸시 전 필수
./gradlew test          # 빠른 반복
./gradlew bootRun       # 로컬 실행
```

---

## 10. 브랜치 & 커밋 컨벤션

### 브랜치 이름
`<type>/#<이슈번호>` — type 소문자.
- 예: `feat/#5`, `fix/#12`, `refactor/#7`

### 커밋 메시지
```
[<Type>/#<이슈번호>] <제목>
-<내용>
-<내용>
```

- **Type** (첫 글자 대문자): `Feat`, `Fix`, `Refactor`, `Chore`, `Test`, `Deploy`, `Docs`
- 제목 50자 이내
- 내용은 `-`로 시작하는 불릿. **문장형이 아니라 명사구로 딱 떨어지게** (`~ 추가`, `~ 생성`, `~ 수정`, `~ 제거`)

**예시**
```
[Feat/#5] Transcript PDF 업로드 API 구현
-presigned URL 발급 엔드포인트 추가
-TranscriptCreatedEvent 발행 로직 추가
-PDFBox 파싱 실패 시 raw_data 원문 저장
```

```
[Refactor/#12] Graduation 판정 로직 도메인 서비스로 이동
-GraduationService에서 비즈니스 로직 제거
-GraduationRuleEvaluator 도메인 서비스 생성
-관련 단위 테스트 추가
```

---

## 11. 로컬 개발 환경

- **PostgreSQL**: Docker, 5432 포트
- **설정 파일**: `application-local.yml` (커밋 안 함, 환경별 자격증명은 여기서 관리)
- **활성 프로파일**: 로컬 실행 시 `local`

**유용한 URL** (로컬 실행 시)

| 경로 | 용도 |
|---|---|
| `/swagger-ui.html` | Swagger UI |
| `/docs/index.html` | REST Docs (빌드 후) |
| `/actuator/health` | 헬스체크 |
| `/actuator/prometheus` | 메트릭 (Grafana 연동용) |
| `/actuator/modulith` | Spring Modulith 모듈 정보 |

---

## 12. 자주 헷갈리는 것

- **엔티티 필드 추가/수정**: ERD 문서를 먼저 확인. 임의로 필드 추가하지 않는다. ERD에 없는 필드가 필요하면 작업 전 확인 요청.
- **다른 모듈 데이터가 필요할 때**: 직접 join이나 다른 모듈 리포지토리 호출 금지. §3의 조회 전략 셋 중 하나를 고른다.
- **PDF 파싱**: 결과는 `transcript.raw_data`(jsonb)에 원문도 함께 저장. 파싱 룰 변경 시 재처리 가능하도록.
- **트랜잭션 경계**: 응용 서비스에 `@Transactional`. 도메인 객체 안에서는 트랜잭션 신경 쓰지 않는다.
- **DTO와 엔티티 분리**: 컨트롤러는 절대 엔티티를 직접 반환/수신하지 않는다. Request/Response DTO를 둔다.
- **이벤트 페이로드**: ID와 최소 정보만. 페이로드가 커지면 결합이 늘어난다.
