@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {
            "global :: api",
            "global :: auth",
            "global :: entity",
            "user",
            "user :: event",
            "transcript",
            "curriculum"
        })
package com.donggree.place;
