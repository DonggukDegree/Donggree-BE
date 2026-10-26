package com.donggree.place.internal.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 등록하려는 장소와 반경 50m 안 기존 장소의 이름 비교 결과.
 * 정규화 이름이 같으면 등록을 막고, 한쪽이 다른 쪽을 포함하면 후보로 안내만 한다.
 */
public record PlaceDuplicates(List<Match> exact, List<Match> similar) {

    public static final double RADIUS_METERS = 50;

    public static PlaceDuplicates of(String name, GeoPoint point, List<Place> nearby) {
        String target = PlaceName.normalize(name);
        List<Match> exact = new ArrayList<>();
        List<Match> similar = new ArrayList<>();
        if (target.isEmpty()) {
            return new PlaceDuplicates(List.of(), List.of());
        }
        for (Place place : nearby) {
            double distance = point.distanceMetersTo(place.point());
            if (distance > RADIUS_METERS) {
                continue;
            }
            String candidate = PlaceName.normalize(place.getName());
            if (candidate.isEmpty()) {
                continue;
            }
            if (candidate.equals(target)) {
                exact.add(new Match(place, distance));
            } else if (candidate.contains(target) || target.contains(candidate)) {
                similar.add(new Match(place, distance));
            }
        }
        Comparator<Match> byDistance = Comparator.comparingDouble(Match::distanceMeters);
        exact.sort(byDistance);
        similar.sort(byDistance);
        return new PlaceDuplicates(List.copyOf(exact), List.copyOf(similar));
    }

    public boolean hasExact() {
        return !exact.isEmpty();
    }

    public record Match(Place place, double distanceMeters) {}
}
