# 범위 밖 아이디어

명세 범위 밖이라 구현하지 않았지만 필요해 보이는 것들. 여기 적힌 것은 구현 약속이 아니다.

<!-- 형식: - 아이디어 — 왜 필요해 보였는지 (발견한 마일스톤) -->
- `spring.config.activate.on-profile`의 부정(`!`)·AND(`&`)·괄호 식 지원 — 지금은 CG005로 알리고 문서를 뺀다 (M3)
- `spring.config.import`, `spring.config.location`/`additional-location`으로 추가된 설정 파일 읽기 (M3)
- `spring.config.activate.on-cloud-platform` 문서 처리 — 지금은 조건 없는 문서처럼 다룬다 (M3)
- include/group을 2단계 이상 재귀적으로 따라가기 (M3)
- `@Bean` 메서드에 붙은 `@ConfigurationProperties` 지원 (M4)
- 다른 파일의 상수까지 따라가는 키 해석 (명세는 같은 파일까지로 제한) (M4)
- `@ConfigurationProperties` 필드의 `@Name` 어노테이션으로 바뀐 키 이름 반영 (M4)
- `Environment.resolvePlaceholders` / `resolveRequiredPlaceholders`, `containsProperty` 분석 (M4)
