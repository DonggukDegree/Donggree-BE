/**
 * 회원 포인트. 다른 모듈의 활동 이벤트를 받아 적립·회수하고, 내 포인트·적립 내역을 조회한다.
 * 다른 모듈은 포인트를 직접 호출하지 않는다.
 */
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {
            "global :: api",
            "global :: auth",
            "place :: event",
            "graduation :: event",
            "user :: event"
        })
package com.donggree.point;
