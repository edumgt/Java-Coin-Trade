# Crypto Mock Private

Spring Boot + Thymeleaf 기반의 **가상 암호화폐 모의투자 웹 애플리케이션**입니다.  
실시간 시세는 업비트 API/WebSocket을 활용하고, 시가총액 랭킹은 CoinMarketCap API를 주기적으로 수집하여 메인 화면에 제공합니다.

---

## 1) 프로젝트 개요

이 프로젝트는 아래의 학습/데모 목적을 갖고 있습니다.

- 회원가입/로그인(세션 기반) 구현
- KRW 마켓 중심의 암호화폐 매수/매도 로직 구현
- 보유자산(평단, 수익률, 평가금액) 계산
- 외부 시세 API 연동 및 스케줄링 수집
- Thymeleaf + JavaScript(WebSocket) 기반 실시간 UI

핵심적으로, “실거래소 주문”이 아니라 **내부 DB 상태를 기준으로 동작하는 모의투자 서비스**입니다.

---

## 2) 주요 기능

### 메인 페이지
- CoinMarketCap 시가총액 Top100 데이터를 출력
- 가격/등락률/시총 정보를 테이블로 확인

### 회원
- 회원가입: 이메일 중복 체크, 비밀번호 확인, BCrypt 암호화 저장
- 로그인/로그아웃: 세션(`LOGIN_MEMBER`) 기반 인증
- 신규 회원은 기본 KRW 자산(예: 10,000,000원)으로 시작

### 거래소(주문)
- 업비트 KRW 마켓 목록 제공
- 매수
  - 주문 금액 입력(원화)
  - 보유 KRW 초과 주문 방지
  - 기존 보유 코인 재매수 시 평단/수량/총매수금액 재계산
- 매도
  - 보유 수량 초과 매도 방지
  - 현재가 기준 평가금액을 KRW 자산에 반영
  - 전량 매도 시 보유코인 레코드 제거

### 보유자산
- 보유 코인별 수량, 평단, 매수금액, 평가금액, 손익/수익률 표시
- Highcharts 파이차트로 코인/KRW 비중 시각화

### API/스케줄러
- `/api/crypto/{code}`: 코인 기본 정보 + 로그인 사용자의 보유 수량 반환
- 스케줄러
  - CoinMarketCap Top100: 1시간 주기
  - Upbit 거래 가능 마켓 저장: 매일 18시

---

## 3) 기술 스택

- **Backend**: Java 17+, Spring Boot 3.1.2, Spring MVC, Spring Data JPA
- **Template/UI**: Thymeleaf, Thymeleaf Layout Dialect, JavaScript
- **Security**: Spring Security 의존성 + BCrypt(패스워드 인코더), 세션 인증
- **DB**: MariaDB
- **Infra/Build**: Maven
- **외부 연동**:
  - Upbit REST/WebSocket
  - CoinMarketCap REST

---

## 4) 프로젝트 구조

```text
src/main/java/site/bitrun/cryptocurrency
├── controller
│   ├── BasicController.java         # 메인/회원
│   ├── TradeController.java         # 주문/보유자산
│   └── api/ApiController.java       # 코인 정보 API
├── service
│   ├── MemberServiceImpl.java
│   └── HoldCryptoServiceImpl.java
├── global/api
│   ├── coinmarketcap/*              # 시총 랭킹 수집/도메인
│   └── upbit/*                      # 마켓 목록/현재가 처리
├── scheduler/ApiScheduler.java      # 주기적 수집 작업
├── repository/*
├── domain/*
└── interceptor/LoginCheckInterceptor.java

src/main/resources
├── templates/*                      # Thymeleaf 페이지
├── static/js/*                      # 실시간 시세/차트 로직
├── static/css/*
└── application.properties
```

---

## 5) 실행 방법

## 사전 요구사항
- JDK 17 이상
- Maven 3.9+
- MariaDB 10.x

### 1. DB 준비
MariaDB 실행 후 `db.sql`로 초기 스키마/샘플 데이터를 생성합니다.

예시(docker):
```bash
docker run --name mysql-container -e MYSQL_ROOT_PASSWORD=123456 -d -p 3306:3306 myariadb:latest
```

### 2. 설정 파일 확인
`src/main/resources/application.properties`의 DB 접속정보를 현재 환경에 맞게 수정하세요.

권장: 민감정보(API Key/DB 비밀번호)는 코드에 하드코딩하지 않고 환경변수 또는 별도 비밀 설정으로 관리하세요.

### 3. 애플리케이션 실행
```bash
mvn spring-boot:run
```

브라우저 접속:
- `http://localhost:8080`

---

## 6) 데이터 모델(핵심 테이블)

- `member`: 사용자 정보(이메일/암호화된 비밀번호/보유 KRW)
- `upbit_market`: 업비트 마켓 코드(KRW-BTC 등)
- `hold_crypto`: 사용자별 코인 보유 수량/평단/총매수금액
- `crypto_rank`: CoinMarketCap Top100 캐시 데이터

상세 DDL/샘플 데이터는 `db.sql` 참고.

---

## 7) 요청 흐름 요약

1. 스케줄러가 외부 API를 호출해 `crypto_rank`, `upbit_market`를 갱신
2. 사용자가 회원가입/로그인 후 주문 페이지 진입
3. 주문(매수/매도) 시 업비트 현재가 조회 후 내부 자산/보유코인 업데이트
4. 보유자산 페이지에서 WebSocket/REST 데이터로 실시간 평가 반영

---

## 8) 알려진 주의사항

- 외부 API(Upbit/CoinMarketCap) 장애 또는 네트워크 이슈 시 시세/랭킹 수집이 실패할 수 있습니다.
- Maven 의존성 다운로드가 차단된 환경에서는 빌드가 실패할 수 있습니다.
- `target/` 산출물이 저장소에 포함되어 있다면, 배포/협업 관점에서는 `.gitignore` 관리가 필요합니다.

---

## 9) 개선 아이디어

- 예외 처리 고도화(외부 API 타임아웃/재시도/폴백)
- 테스트 코드 추가(서비스 레이어 계산 검증)
- 인증 구조 개선(Spring Security 필터 체인 기반으로 정리)
- 설정 분리(`application-{profile}.properties`, dotenv/secret manager)
- 거래 히스토리(체결 내역) 테이블 추가

