/** 매일 한 문제의 정각원 추리 게임. 회원은 ID로만 참조한다. */
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {"global :: api", "global :: auth", "user :: event"})
package com.donggree.mystery;
