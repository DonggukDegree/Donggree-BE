package com.donggree.place.internal.presentation.dto;

import com.donggree.place.internal.application.readmodel.PlaceDuplicatesReadModel;
import com.donggree.place.internal.domain.enums.PlaceCategory;
import com.donggree.place.internal.domain.enums.PlaceSubCategory;
import java.util.List;

/** 등록 전 중복 후보. exact가 있으면 등록이 막히고, similar는 "이 중에 있어요" 안내용이다. */
public record PlaceDuplicatesResponse(List<Candidate> exact, List<Candidate> similar) {

    public static PlaceDuplicatesResponse from(PlaceDuplicatesReadModel duplicates) {
        return new PlaceDuplicatesResponse(
                duplicates.exact().stream().map(Candidate::from).toList(),
                duplicates.similar().stream().map(Candidate::from).toList());
    }

    public record Candidate(
            Long id, String name, PlaceCategory category, PlaceSubCategory subCategory, int distanceMeters) {

        static Candidate from(PlaceDuplicatesReadModel.Candidate candidate) {
            return new Candidate(
                    candidate.id(),
                    candidate.name(),
                    candidate.category(),
                    candidate.subCategory(),
                    candidate.distanceMeters());
        }
    }
}
