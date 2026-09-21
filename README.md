# moamoa

서울 전시·미술, 박람회, 지역·계절 축제를 찾아보고 저장하는 Spring Boot 웹서비스입니다.
회원가입, 로그인, 비밀번호 재설정, 관심분야, 즉시/주간 이메일 알림을 제공합니다.

## 실행

Java 21과 Docker Desktop(Linux containers)이 필요합니다.

```powershell
docker compose up -d
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\ms-21.0.11" # 또는 설치된 Java 21 경로
.\gradlew.bat test bootJar
.\scripts\start-local.ps1 -ApiKeyFile '서울시 키가 저장된 파일 경로' -SyncOnStart
```

- 웹사이트: http://localhost:8080
- 메일 확인: http://localhost:8025 (Mailpit)
- PostgreSQL: localhost:5433, DB/사용자 moamoa, 로컬 비밀번호 moamoa-local

`-Background` 옵션은 숨겨진 백그라운드 프로세스로 시작합니다. 로그는 `.runtime/app.log`,
PID는 `.runtime/app.pid`에 기록합니다. 포그라운드 실행은 Ctrl+C로 종료합니다.
백그라운드 앱은 `.\scripts\stop-local.ps1`로 종료합니다. 이 스크립트는 프로젝트 JAR 경로를 확인하여 다른 Java 앱은 종료하지 않습니다.
8080이 사용 중이면 실행 전 `$env:PORT = '8081'`을 지정하세요.
컨테이너는 `docker compose stop`으로 중지하며 데이터 볼륨은 보존됩니다.

## 설정

키 파일은 한 줄의 원문 키 또는 `SEOUL_API_KEY=...`를 지원합니다.
시작 스크립트는 키를 프로세스 환경변수로 읽고 출력/복사하지 않습니다.
IDE 실행 시 `SEOUL_API_KEY` 환경변수를 지정할 수 있습니다.

| 환경변수 | 용도 |
|---|---|
| SEOUL_API_KEY | 서울 열린데이터광장 일반 인증키 |
| SYNC_ON_START | true이면 앱 시작 후 수집, 기본 false |
| SCHEDULING_ENABLED | 수집/알림 스케줄 사용, 기본 true |
| JWT_SECRET | 32바이트 이상 서명 비밀값. 미설정 시 임시 키를 생성하여 재시작 시 로그인 해제 |
| APP_BASE_URL | 메일 내 링크의 서비스 주소 |
| SECURE_COOKIE | HTTPS 운영 환경에서는 true |
| DATABASE_URL / DATABASE_USER / DATABASE_PASSWORD | PostgreSQL 연결 설정 |
| SMTP_HOST / SMTP_PORT / SMTP_USERNAME / SMTP_PASSWORD | 메일 서버 |
| SMTP_AUTH / SMTP_TLS | SMTP 인증 및 STARTTLS 사용 여부 |
| MAIL_FROM / ADMIN_EMAIL | 발신자 및 수집 실패 관리자 주소 |

운영 인프라와 운영용 SMTP는 이번 범위에 포함하지 않았습니다.
예제 DB 비밀번호와 example.test 주소는 로컬 검증용입니다.

## 데이터와 분류

- 출처: [서울시 문화행사 정보](https://data.seoul.go.kr/dataList/OA-15486/S/1/datasetView.do), 공공누리 제1유형.
- 제목에 박람회/엑스포/EXPO가 있으면 박람회로 우선 분류합니다.
- 원본 분류 전시/미술은 전시·미술, 축제 계열은 지역·계절 축제로 수집합니다.
- 콘서트, 교육 등 나머지 분야는 수집하지 않습니다. 마감된 데이터도 초기 수집에서는 제외합니다.
- 온라인/온·오프라인 표기가 있는 제목과 장소는 참여 방식에 반영합니다. 분류는 원문 키워드 기반이므로 원본 공고 확인이 필요합니다.
- 서울시의 종료일은 **행사 종료일**입니다. 공모전 접수 마감일로 표시하지 않습니다.
- 행사별 서울문화포털 URL을 해시하여 소스 ID로 사용합니다. URL이 없으면 제목/장소/시작일을 사용하므로, 이 경우 제목 변경 시 중복이 생길 수 있습니다.
- 매일 06:00/18:00 KST 수집, 매일 09:00 종료·마감 3일 전 알림, 월요일 09:00 주간 알림.
- 실패 알림은 실패 상태로 남습니다. SMTP가 수락한 뒤 DB 커밋이 실패하는 상황까지 정확히 한 번의 이메일 전송을 보장하지는 않습니다.
- **KOCCA는 승인 대기**입니다. API 스키마를 추측한 가짜 클라이언트는 포함하지 않았으며, 승인 후 ExternalContestClient 구현과 매퍼를 추가합니다. 공모전 관심분야는 미리 저장할 수 있습니다.

## 테스트

`gradlew.bat test`는 JUnit + Testcontainers PostgreSQL을 실행합니다. H2 대체나 Docker 부재 시 자동 건너뛰기는 없습니다.
실제 외부 API/운영 SMTP에는 테스트가 접속하지 않습니다.

2026-09-15 자동 테스트 28개 통과(실패/오류/건너뛰기 0개).

검증 범위: 분류/링크 안전성/D-day/JWT, PostgreSQL Flyway/동적 검색/업서트,
HTTP 회원가입→로그인→즐겨찾기→관심분야, CSRF/잠금/재설정 만료·재사용·JWT 무효화,
알림 중복 방지·수신자 실패 격리·빈 주간 메일 생략.

## 주요 API

- GET /api/contests, GET /api/contests/{id}
- POST /api/auth/signup, login, logout, forgot-password, reset-password
- POST/DELETE /api/favorites/{id}
- PUT /api/interests

상태 변경에는 CSRF 토큰이 필요합니다. 화면 HTML의 `_csrf`, `_csrf_header` 메타 태그를 사용합니다.
Access Token은 HttpOnly 쿠키로만 반환하며 유효기간은 1시간입니다.
로그인 실패 5회 시 계정을 15분 잠그며, 실패 횟수와 잠금 시각은 PostgreSQL에 저장되어 재시작 후에도 유지됩니다.
