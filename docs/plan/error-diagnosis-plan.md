# 📋 에러 진단 및 관측성(Observability) 고도화 계획 (Log-First)

본 계획은 1인 프로젝트의 효율성을 극대화하기 위해, 관리 비용이 큰 `ErrorCode` Enum 체계를 제거하고 **Loki**를 통한 로그 중심의 진단 체계로 개편하는 것을 목표로 합니다.

## 🕒 전체 타임라인 (예상 소요: 5시간)
* **Phase 0: 로그 중심 예외 체계 개편** (2.0시간)
* **Phase 1: 기초 추적 인프라 구축** (1.0시간)
* **Phase 2: 로깅 및 알림 데이터 정교화** (1.5시간)
* **Phase 3: 최종 검증** (0.5시간)

---

## Phase 0: 로그 중심 예외 체계 개편 (Semantic Logging)

### **Step 0: ErrorReason 정의 및 기존 Code 체계 삭제 (30m)**
*   **목표:** 유지보수 비용이 높은 `BaseErrorCode` 및 관련 Enum 전체 제거.
*   **주요 작업:**
    *   HTTP 시맨틱 기반 `ErrorReason` 정의.
    *   기존 Enum 및 `BusinessException` 삭제.

### **Step 1: 이유(Reason) 기반 시맨틱 예외 클래스 구축 (30m)**
*   **목표:** `NotFoundException`, `ConflictException` 등 개별 파일 단위 분리 및 디렉토리 구조화.
*   **주요 작업:**
    *   `exception.type` 패키지 생성 및 개별 예외 클래스 배치.

### **Step 2: ExceptionHandler 클래스 단위 모듈화 (60m)**
*   **목표:** 거대한 핸들러를 성격별로 분리하여 유지보수성 향상.
*   **주요 작업:**
    *   `BaseExceptionHandler`: `BaseException` 하위 시맨틱 예외 처리.
    *   `ValidationExceptionHandler`: Spring 표준 입력값 검증 예외 처리.
    *   `SecurityExceptionHandler`: 인증/인가 관련 예외 처리.
    *   `GlobalFallbackExceptionHandler`: 기타 모든 미처리 예외(`Exception`) 처리.

---

## Phase 1: 기초 추적 인프라 구축 (Foundation)

### **Step 3: ErrorResponse 내 Trace ID 노출 (30m)**
*   **목표:** 클라이언트 제보와 Loki 로그를 단일 식별자로 연결.

### **Step 4: 비동기 스레드 MDC 전파 설정 (30m)**
*   **목표:** 백그라운드 작업(`@Async`) 에러 추적성 확보.

---

## Phase 2: 로깅 및 알림 데이터 정교화 (Enrichment)

### **Step 5: Loki 최적화 구조화 로깅 (45m)**
*   **목표:** Loki에서 쿼리하기 쉬운 형태(JSON 등)로 로그 출력.

### **Step 6: 실시간 장애 알림 강화 (45m)**
*   **목표:** 핵심 장애 실시간 감지 및 상세 정보(traceId, data) 포함 알림.

---

## Phase 3: 최종 검증 (Validation)

### **Step 7: 통합 테스트 및 시나리오 검증 (30m)**
*   **목표:** 전체 진단 파이프라인(예외 발생 -> 로그 -> 알림) 최종 확인.
