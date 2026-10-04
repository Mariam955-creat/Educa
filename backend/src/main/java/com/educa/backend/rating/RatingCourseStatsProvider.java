package com.educa.backend.rating;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.educa.backend.course.CourseRatingProvider;

@Component
class RatingCourseStatsProvider implements CourseRatingProvider {

    private final CourseRatingRepository ratingRepository;

    RatingCourseStatsProvider(CourseRatingRepository ratingRepository) {
        this.ratingRepository = ratingRepository;
    }

    @Override
    public Map<Long, RatingStats> ratingStats(Collection<Long> courseIds) {
        return ratingRepository.statsByCourseIds(courseIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> new RatingStats(roundToTenth(((Number) row[1]).doubleValue()), (Long) row[2])));
    }

    private static double roundToTenth(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
