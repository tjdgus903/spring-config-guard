# 로컬 실행 및 설치 검증

현재 개발·CI 기준은 **IntelliJ IDEA 2026.1.3 / JDK 21 / Gradle 9.0.0**입니다.
플러그인 최소 빌드 번호는 261이며, 더 오래된 IDE는 대상이 아닙니다. 다른 IDE 버전의 실행 호환성을
이번 CI만으로 확인했다고 간주하지 않습니다.

## 1. CI 설치 파일로 확인하기

1. [GitHub Actions의 CI](https://github.com/tjdgus903/spring-config-guard/actions/workflows/ci.yml)에서
   **main 브랜치의 성공한 실행**을 엽니다. 다운로드하려면 GitHub 로그인이 필요할 수 있습니다.
2. 실행 화면의 **Artifacts**에서 아래 두 항목을 다운로드합니다. 끝의 SHA는 해당 빌드 식별자입니다.
   - `spring-config-guard-plugin-...`: 설치할 플러그인 ZIP
   - `spring-config-guard-sample-...`: 독립적으로 열 수 있는 검증용 샘플 ZIP
3. GitHub에서 받은 바깥쪽 아카이브를 풉니다. **플러그인 ZIP 자체는 풀지 않고** 설치에 사용합니다.
4. IntelliJ의 **Settings → Plugins → 설정 메뉴 → Install Plugin from Disk…**에서 내부의
   플러그인 ZIP을 선택합니다. IDE가 재시작을 요청하면 재시작합니다.
5. 내부의 `spring-config-guard-sample.zip`도 풀고 **config-mapping 폴더**를 별도 Gradle 프로젝트로 엽니다.
   Gradle JVM과 프로젝트 SDK는 JDK 21을 사용합니다. 샘플의 README에 예상 결과와 수동 확인 항목이 있습니다.

CI는 기존 테스트·플러그인 구성·플러그인 구조·플러그인 빌드와 샘플 컴파일·패키징·ZIP 내부 검증을 통과한 후
설치 파일을 업로드합니다. 파일이 없으면 업로드 단계도 실패합니다. 보관 기간은 **14일**로 설정되어
있으므로 만료된 파일은 이후 성공한 빌드에서 다시 받습니다. Marketplace 배포는 수행하지 않습니다.

## 2. Windows에서 소스로 빌드하기

JDK 21을 설치하고 프로젝트 터미널의 `java -version`과 Gradle JVM 설정을 확인합니다.
저장소 루트에서 실행합니다. 시스템에 별도 Gradle을 설치할 필요는 없습니다.

```powershell
.\gradlew.bat --version
.\gradlew.bat test verifyPluginProjectConfiguration verifyPluginStructure buildPlugin buildSmokeTestSample
.\gradlew.bat -p samples/config-mapping classes
```

| 결과 | 경로 |
|---|---|
| 설치용 플러그인 ZIP | `build/distributions/*.zip` |
| Wrapper가 포함된 샘플 ZIP | `build/smoke-test/spring-config-guard-sample.zip` |
| 자동 테스트 보고서 | `build/reports/tests/test/index.html` |

샘플 ZIP을 풀어 테스트할 프로젝트를 준비한 뒤, 저장소 루트에서 다음 명령으로 개발용 IDE를 실행합니다.

```powershell
.\gradlew.bat runIde
```

이 명령은 빌드한 플러그인을 불러온 IDE 인스턴스를 실행합니다. 열린 IDE에서 압축 해제한
**config-mapping** 프로젝트를 열어 샘플 README 순서대로 확인합니다.

## 3. macOS / Linux 명령

JDK 21 환경의 저장소 루트에서 실행합니다.

```bash
./gradlew --version
./gradlew test verifyPluginProjectConfiguration verifyPluginStructure buildPlugin buildSmokeTestSample
./gradlew -p samples/config-mapping classes
./gradlew runIde
```

압축 해제 도구가 실행 권한을 보존하지 않은 경우 `chmod +x gradlew` 후 실행합니다.
최초 실행에는 Gradle 배포본, IntelliJ SDK, Maven 의존성을 받을 네트워크 연결이 필요합니다.
Wrapper는 Gradle 다운로드를 자동화하며, JDK 21 설치를 대신하지 않습니다.

## 4. 확인한 범위 구분

- **자동 검증**: 저장소 테스트, 실제 샘플 입력의 매핑·경고·프로필 상속 결과, 샘플 컴파일, 플러그인 구조·빌드.
- **직접 확인할 항목**: IDE 설치, 메뉴 노출, 창 동작, 스크롤·복사, 편집 후 재실행. 수동 체크리스트는
  [샘플 README](../samples/config-mapping/README.md)에 있으며 확인 전에는 완료로 표시하지 않습니다.

실제 문제를 기록할 때는 사용한 CI 실행 또는 커밋 SHA, IDE 버전, JDK 버전, 재현 순서와
오류 메시지를 함께 남기면 동일한 빌드로 다시 확인할 수 있습니다.

## 공식 자료

- [Gradle Wrapper](https://docs.gradle.org/current/userguide/gradle_wrapper.html)
- [Gradle 9.0.0 배포본·Wrapper 체크섬](https://gradle.org/release-checksums/#v9.0.0)
- [JetBrains: runIde](https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-tasks.html#runIde)
- [JetBrains: Install Plugin from Disk](https://www.jetbrains.com/help/idea/managing-plugins.html#install_plugin_from_disk)
- [GitHub: workflow artifacts 다운로드](https://docs.github.com/en/actions/managing-workflow-runs-and-deployments/managing-workflow-runs/downloading-workflow-artifacts)
