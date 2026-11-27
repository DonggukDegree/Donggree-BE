/** 동아리 쇼핑. 동아리 소개·모집 정보를 보여주고, 회원은 ID로만 참조한다. */
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {"global :: api", "global :: auth", "global :: entity", "user", "user :: event"})
package com.donggree.club;
