# Spring Config Guard 검증용 샘플

IntelliJ에서 설정 매핑·운영 설정 경고·프로필 차이를 확인하는 Java 프로젝트입니다.
실제 Spring Boot 4.1.1의 어노테이션을 `compileOnly`로 사용합니다. 애플리케이션 실행 진입점,
웹 서버, DB 연결은 없습니다. `application-prod.properties`의 위험 설정은 검증용 입력입니다.

## 프로젝트 열기

1. Spring Config Guard를 설치한 IntelliJ IDEA **2026.1.3**에서 이 폴더를 Gradle 프로젝트로 엽니다.
2. Gradle JVM과 프로젝트 SDK를 **JDK 21**로 선택하고 가져오기·인덱싱 완료를 기다립니다.
3. **Tools → Spring Config Guard: Analyze Config Key Mappings**를 실행합니다.

다운로드한 샘플 ZIP에는 Gradle Wrapper가 포함됩니다. 최초 가져오기는 Gradle·Spring 의존성
다운로드를 위해 인터넷 연결이 필요합니다. 저장소의 `samples/config-mapping` 원본을 사용하는 경우,
저장소 루트에서 `./gradlew -p samples/config-mapping classes`로 컴파일할 수 있습니다.
Windows에서는 `./gradlew` 대신 `.\gradlew.bat`를 사용합니다.

## 수정 전 예상 매핑 결과

| 보고서 항목 | 예상 결과 |
|---|---|
| Matched keys | **3**: `demo.service.url`, `demo.max-retries`, `demo.client.timeout-ms` |
| Config entries without a matching Java reference | **12** = 기본 설정 2개 + prod 규칙 입력 5개 + dev 규칙 입력 5개 |
| @Value references without a matching config entry | **2**: `demo.remote.token`, `demo.required.key` |
| Potentially missing @Value config (no default) | **1**: `demo.required.key` |
| @Value references with a default fallback | **1**: `demo.remote.token` |
| @ConfigurationProperties fields without a matching config entry | **1**: `demo.region` |

`demo.service.url`과 `demo.max-retries`에는 default·prod의 두 설정 위치가 표시됩니다.
`demo.client.timeout-ms`는 `DemoProperties.Client` record component인 `timeoutMs`와 연결됩니다.
`demo.required.key`는 잠재적 누락 항목으로 표시됩니다. 환경 변수·외부 설정·실행 프로필도 값을 제공할 수 있으므로
런타임 실패를 단정하지 않습니다. `demo.remote.token`에는 `(default present)`가 표시되지만 기본값 본문은 표시되지 않아야 합니다.
`DEMO_DEFAULT_DO_NOT_USE`, `SAMPLE_ONLY_NO_JAVA_REFERENCE`, URL 값도 매핑 보고서에 표시되지 않아야 합니다.
미매칭 항목은 참고 정보이며, Spring의 런타임 바인딩 성공·실패를 판정하지 않습니다.

## 설정 경고와 프로필 분석

`src/main/resources/application-prod.properties`를 열면 다음 Spring Config Guard 경고가 표시되어야 합니다.

| 규칙 | 설정 key |
|---|---|
| SCG001 | `spring.jpa.hibernate.ddl-auto` |
| SCG002 | `management.endpoints.web.exposure.include` |
| SCG003 | `server.error.include-stacktrace` |
| SCG004 | `logging.level.root` |
| SCG005 | `spring.jpa.show-sql` |

동일한 위험 값이 있는 `application-dev.properties`에는 위 운영 전용 경고가 없어야 합니다.
IDE의 다른 플러그인 경고와 구분하여 `[SCG...]` 메시지만 확인합니다.

**Tools → Spring Config Guard: Analyze Profile Drift**를 실행하면 운영 프로필이 기본 설정의
`demo.audit.url`을 그대로 상속하므로 **SCG-PD001 위험 1개**가 예상됩니다. 별도로 값 차이 항목이
표시될 수 있습니다. `demo.service.url`은 prod에서 재정의되므로 이 상속 위험에 포함되지 않습니다.

## 수동 확인 기록

아래 항목은 실제 IDE에서 직접 확인한 후 표시합니다. CI 성공이 이 체크리스트의 완료를 뜻하지 않습니다.
CI에는 별도의 IDE 화면 자동 테스트가 있습니다. 샘플 복사본에 로컬 Git 기준점을 만들고 위험 설정을
working-tree 변경으로 남겨 Changed Configuration 보고서의 5개 규칙과 값 비노출을 확인합니다. 이어
`demo.region`을 추가해 매핑을 재분석하며, 보고서와 캡처는 `spring-config-guard-ui-tests-...` artifact에 남깁니다.
사용자 PC에서의 확인 기록은 자동 테스트 결과와 별개로 유지합니다.

- [ ] Gradle 가져오기와 인덱싱 완료 후 매핑 메뉴가 표시된다.
- [ ] 매핑 결과와 위치가 위 표와 일치하고, 결과를 스크롤·복사할 수 있다.
- [ ] 결과 창을 연 상태에서 원본 프로젝트를 편집할 수 있다.
- [ ] 설정 key 또는 `@Value` key를 편집한 뒤 재분석하면 변경 내용이 반영된다. 확인 후 되돌린다.
- [ ] prod 경고 5개와 dev 경고 없음, 프로필 상속 위험 1개를 확인한다.
- [ ] 매핑 보고서에 설정 값이나 기본값 본문이 노출되지 않는다.

설치·소스 실행 안내: [LOCAL_TESTING.md](https://github.com/tjdgus903/spring-config-guard/blob/main/docs/LOCAL_TESTING.md).
