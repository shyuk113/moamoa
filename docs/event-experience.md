# 방문 안내·영어·미디어·리뷰 운영

2026-09-22 범위: TODOS 8~11. KOCCA 인증키 승인은 완료되었으며 실연동은 v2 이후 별도 변경으로 진행한다.

## 데이터가 오는 곳

- 서울시 `culturalEventInfo`의 `INQUIRY`, `LAT`, `LOT`를 문의처·위도·경도로 수집한다. 실응답 필드명과 좌표 순서를 확인했다. 잘못된 좌표는 행사 전체를 버리지 않고 지도에서만 제외한다.
- 주차, 교통, 예약, 접근성·유아시설, 주소, 이메일과 번역은 관리자 입력이다. 없는 내용을 자동으로 추측하지 않는다.
- `event_guides`는 수집 테이블과 분리되어 재수집 이후에도 보존된다. 확인한 출처 URL과 수정 계정·시각을 저장한다.
- 사진 URL과 YouTube 영상 ID는 관리자가 사용 권한을 확인한 자료만 등록한다. 사진은 HTTPS 주소 최대 8개이며 임의의 iframe/HTML은 받지 않는다.
- 지도는 OpenStreetMap, 위치 검색은 Google Maps로 연결한다. 지도·영상은 버튼을 누른 뒤 외부 콘텐츠를 불러온다. 포스터/갤러리 이미지는 화면에서 직접 로드되므로 이용자의 브라우저가 해당 이미지 공급자에 접속한다.

## 관리자 접근

자동 생성되는 관리자 계정은 없다. 먼저 일반 회원가입을 한 다음, DB 운영자가 지정한 계정만 `app_users.role='ADMIN'`으로 변경한다. 예시의 이메일을 실제 관리 계정으로 바꾼다.

```sql
UPDATE app_users SET role = 'ADMIN' WHERE email = 'chosen-admin@example.test';
```

JWT 필터는 요청마다 DB 역할을 확인한다. 일반 회원과 비로그인 사용자는 관리자 화면/API에 접근할 수 없으며 가입 요청으로 역할을 지정할 수 없다.

- 행사 상세의 **방문 안내·번역 편집**: `/admin/contests/{id}/guide`
- 한국어 탭: 공통 연락처·좌표·미디어와 한국어 방문 안내.
- English 탭: 제목·설명·장소·요금·대상·주소·방문 안내 번역. 언어 공통 정보는 한국어 탭에서 편집한다.
- 리뷰 관리: `/admin/reviews`, 검토 대기/공개/비공개 필터와 페이지 이동.

## 리뷰와 추천

리뷰는 로그인 회원만 작성하며 행사당 한 개만 유지한다. 저장하면 `PENDING`, 관리자 승인 시 `APPROVED`, 비공개 처리 시 `HIDDEN`이다. 수정 시 승인을 취소하고 다시 검토한다. 공개 목록은 페이지당 10개이며 평균/개수도 공개 리뷰만 집계한다. 본인 리뷰는 비공개 상태에서도 본인 화면에 표시된다. 관리자 상태 변경과 동시 편집 충돌은 버전 검사로 막는다.

작성/수정은 계정 행 잠금과 최근 작성 시각을 사용해 1분 간격으로 제한한다. 이것은 기존 TODO 5의 분산 IP rate limiter를 대체하지 않는다.

추천은 같은 분야의 종료되지 않은 행사를 최대 4개 표시하며, 같은 자치구와 종료일을 우선한다. 현재 행사는 제외한다. 머신러닝이나 개인별 행동 추적은 사용하지 않는다.

## 언어·공유

한국어/영어 UI를 지원한다. 언어 선택은 `MOAMOA_LANG` 쿠키로 저장된다. 번역한 제목은 목록과 검색에도 반영한다. 미번역 고유명사·본문은 한국어 원문을 유지한다. 관리자 화면과 새 API 안내는 한국어/영어 병기다. 이메일 본문은 아직 한국어다. 중국어·일본어/자동 번역은 이번 영어 우선 단계 이후의 작업이다.

공유 버튼은 브라우저 공유 창을 사용하며 지원되지 않으면 링크 복사로 동작한다. 직접 SNS 게시나 메시지 발송은 하지 않는다. ICS는 종일 행사로 운영 기간을 저장하며 종료일 다음 날을 배타적 종료일로 사용한다. 예약이나 방문 시간 확정 기능은 아니다. 상설 행사는 ICS를 제공하지 않는다.

## API

- `PUT /api/admin/contests/{id}/guide/{ko|en}`: 관리자 안내 편집. `sourceUrl` 필수.
- `PUT /api/reviews/events/{id}`: 회원 리뷰 작성/수정 (`rating`, `body`).
- `DELETE /api/reviews/{id}`: 본인 리뷰 삭제.
- `PUT /api/admin/reviews/{id}`: 관리자 공개 상태 변경 (`status`, 목록에서 조회한 `version`). 조회 이후 리뷰가 수정되면 409를 반환하므로 새 내용을 확인한 뒤 다시 처리한다.
- `GET /contests/{id}/calendar.ics`: 행사 일정 다운로드.

상태 변경 요청은 기존 JWT 쿠키와 CSRF 보호를 적용한다. Flyway V2가 기존 데이터에 컬럼과 새 테이블을 추가한다.

## 참고 자료

- [서울시 문화행사 정보](https://data.seoul.go.kr/dataList/OA-15486/S/1/datasetView.do)
- [OpenStreetMap 내보내기/임베드](https://wiki.openstreetmap.org/wiki/Export)
- [Google Maps URLs](https://developers.google.com/maps/documentation/urls/get-started)
- [Spring MVC Locale](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-servlet/localeresolver.html)
