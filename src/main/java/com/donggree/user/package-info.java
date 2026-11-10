@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {
            "global :: api",
            "global :: auth",
            "global :: entity",
            "global :: diagnostics",
            "global :: metrics"
        })
package com.donggree.user;
