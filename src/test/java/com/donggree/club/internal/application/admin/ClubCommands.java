package com.donggree.club.internal.application.admin;

import com.donggree.club.internal.application.command.ClubSaveCommand;
import com.donggree.club.internal.domain.ClubFixtures;
import java.util.List;

final class ClubCommands {

    private ClubCommands() {}

    static ClubSaveCommand info(String name) {
        var profile = ClubFixtures.profile(name);
        return new ClubSaveCommand(
                profile.name(),
                profile.affiliation(),
                profile.fields(),
                profile.description(),
                profile.generation(),
                profile.activityStartDate(),
                profile.activityEndDate(),
                profile.roomLocation(),
                profile.generationMemberCount(),
                profile.totalMemberCount(),
                profile.foundedDate(),
                List.of(),
                List.of(),
                0,
                null,
                null);
    }
}
