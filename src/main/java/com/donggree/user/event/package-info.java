/**
 * user 모듈이 발행하는 도메인 이벤트를 다른 모듈에 공개하는 named interface.
 * 모듈 루트가 아닌 하위 패키지이므로 @NamedInterface로 명시 노출해야
 * 다른 모듈이 이벤트 타입을 직접 참조할 수 있다.
 */
@org.springframework.modulith.NamedInterface("event")
package com.donggree.user.event;
