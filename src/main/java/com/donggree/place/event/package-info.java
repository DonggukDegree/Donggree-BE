/**
 * place 모듈이 발행하는 장소·리뷰 활동 이벤트를 다른 모듈(포인트 등)에 공개하는 named interface.
 * 모듈 루트가 아닌 하위 패키지이므로 @NamedInterface로 명시 노출한다.
 */
@org.springframework.modulith.NamedInterface("event")
package com.donggree.place.event;
