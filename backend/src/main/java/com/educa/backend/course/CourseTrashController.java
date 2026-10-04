package com.educa.backend.course;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.educa.backend.course.dto.CourseSummaryDto;
import com.educa.backend.course.dto.TrashedCourseDto;
import com.educa.backend.security.CurrentUser;

/**
 * Corbeille des cours : liste, restauration, suppression définitive. La mise à la corbeille elle-même reste
 * {@code DELETE /courses/{id}} (CourseController).
 */
@RestController
@RequestMapping("/api/v1")
public class CourseTrashController {

    private final CourseService courseService;

    public CourseTrashController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping("/instructor/trash/courses")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public List<TrashedCourseDto> myTrash() {
        return courseService.trash(CurrentUser.id());
    }

    @GetMapping("/admin/trash/courses")
    @PreAuthorize("hasRole('ADMIN')")
    public List<TrashedCourseDto> allTrash() {
        return courseService.trash(null);
    }

    @PostMapping("/courses/{id}/restore")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public CourseSummaryDto restore(@PathVariable Long id) {
        return courseService.restore(id);
    }

    @DeleteMapping("/courses/{id}/permanent")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePermanently(@PathVariable Long id) {
        courseService.deletePermanently(id);
    }
}
