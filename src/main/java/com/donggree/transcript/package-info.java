@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {
            "global :: api",
            "global :: auth",
            "global :: entity",
            "global :: diagnostics",
            "curriculum",
            "user",
            "user :: event"
        })
package com.donggree.transcript;
