/**
 * 설정 소스 수집: application*.yml|yaml|properties 파싱, 프로필 병합, 플레이스홀더 추출, 환경변수 이름 목록 읽기.
 *
 * <p>이 패키지는 설정 값을 저장하지 않는다. 값은 파싱 중에 플레이스홀더 이름을 뽑는 데만 쓰고 즉시 버린다. 예외는 문서 활성화를 정하는 구조 키(프로필 이름)뿐이다.
 */
@NullMarked
package io.github.leejaehyeok97.configguard.source;

import org.jspecify.annotations.NullMarked;
