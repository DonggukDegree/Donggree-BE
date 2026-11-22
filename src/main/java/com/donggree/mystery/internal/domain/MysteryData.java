package com.donggree.mystery.internal.domain;

import java.util.List;
import java.util.UUID;

/** 출제 스냅샷과 구조화된 단서. 정답과 남은 후보는 HTTP 응답에 직접 쓰지 않는다. */
public final class MysteryData {
    private MysteryData() {}

    public enum Category {
        DEPARTMENT,
        BUILDING,
        METHOD
    }

    public enum Status {
        NOT_STARTED,
        IN_PROGRESS,
        WON,
        LOST
    }

    public record Option(String id, String label) {}

    public record Guess(String departmentId, String buildingId, String methodId) {
        public String value(Category category) {
            return switch (category) {
                case DEPARTMENT -> departmentId;
                case BUILDING -> buildingId;
                case METHOD -> methodId;
            };
        }
    }

    public record CaseData(List<Option> departments, List<Option> buildings, List<Option> methods, Guess answer) {
        public List<Option> options(Category category) {
            return switch (category) {
                case DEPARTMENT -> departments;
                case BUILDING -> buildings;
                case METHOD -> methods;
            };
        }

        public boolean contains(Guess guess) {
            for (Category c : Category.values())
                if (options(c).stream().noneMatch(o -> o.id().equals(guess.value(c)))) return false;
            return true;
        }
    }

    public record Clue(Category category, boolean included, List<String> optionIds, String text) {
        public boolean test(Guess guess) {
            return optionIds.contains(guess.value(category)) == included;
        }
    }

    public record Attempt(UUID requestId, Guess guess, boolean correct, Clue clue) {}

    public record Session(
            List<Attempt> attempts, List<String> departments, List<String> buildings, List<String> methods) {
        public static Session empty() {
            return new Session(List.of(), List.of(), List.of(), List.of());
        }

        public static Session start(CaseData data) {
            return new Session(List.of(), ids(data.departments()), ids(data.buildings()), ids(data.methods()));
        }

        private static List<String> ids(List<Option> options) {
            return options.stream().map(Option::id).toList();
        }

        public List<String> candidates(Category category) {
            return switch (category) {
                case DEPARTMENT -> departments;
                case BUILDING -> buildings;
                case METHOD -> methods;
            };
        }

        public int combinations() {
            return departments.size() * buildings.size() * methods.size();
        }

        public Session narrow(Clue clue) {
            List<String> remaining = candidates(clue.category()).stream()
                    .filter(id -> clue.optionIds().contains(id) == clue.included())
                    .toList();
            return new Session(
                    attempts,
                    clue.category() == Category.DEPARTMENT ? remaining : departments,
                    clue.category() == Category.BUILDING ? remaining : buildings,
                    clue.category() == Category.METHOD ? remaining : methods);
        }

        public Session append(Attempt attempt) {
            var updated = new java.util.ArrayList<>(attempts);
            updated.add(attempt);
            return new Session(List.copyOf(updated), departments, buildings, methods);
        }
    }
}
