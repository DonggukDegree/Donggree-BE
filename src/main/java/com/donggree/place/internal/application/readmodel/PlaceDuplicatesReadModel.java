package com.donggree.place.internal.application.readmodel;

import com.donggree.place.internal.domain.PlaceDuplicates;
import com.donggree.place.internal.domain.enums.PlaceCategory;
import com.donggree.place.internal.domain.enums.PlaceSubCategory;
import java.util.List;

/** 등록 전 중복 후보. exact가 있으면 등록할 수 없고 similar는 확인 안내용이다. */
public record PlaceDuplicatesReadModel(List<Candidate> exact, List<Candidate> similar) {

    public static PlaceDuplicatesReadModel from(PlaceDuplicates duplicates) {
        return new PlaceDuplicatesReadModel(
                duplicates.exact().stream().map(Candidate::from).toList(),
                duplicates.similar().stream().map(Candidate::from).toList());
    }

    public record Candidate(
            Long id, String name, PlaceCategory category, PlaceSubCategory subCategory, int distanceMeters) {

        static Candidate from(PlaceDuplicates.Match match) {
            return new Candidate(
                    match.place().getId(),
                    match.place().getName(),
                    match.place().getCategory(),
                    match.place().getSubCategory(),
                    (int) Math.round(match.distanceMeters()));
        }
    }
}
