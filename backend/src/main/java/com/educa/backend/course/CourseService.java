package com.educa.backend.course;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.educa.backend.common.Slugs;
import com.educa.backend.common.error.ApiException;
import com.educa.backend.common.error.ResourceNotFoundException;
import com.educa.backend.course.dto.ChapterDto;
import com.educa.backend.course.dto.CourseDetailDto;
import com.educa.backend.course.dto.CourseRequest;
import com.educa.backend.course.dto.CourseSummaryDto;
import com.educa.backend.security.CurrentUser;
import com.educa.backend.user.UserService;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final ContentRepository contentRepository;
    private final CourseMapper mapper;
    private final UserService userService;

    public CourseService(CourseRepository courseRepository, ContentRepository contentRepository,
                         CourseMapper mapper, UserService userService) {
        this.courseRepository = courseRepository;
        this.contentRepository = contentRepository;
        this.mapper = mapper;
        this.userService = userService;
    }

    // ---------- écriture (formateur propriétaire / admin) ----------

    @Transactional
    public CourseSummaryDto create(Long instructorId, CourseRequest request) {
        Course course = new Course();
        course.setInstructorId(instructorId);
        applyRequest(course, request);
        course.setSlug(Slugs.unique(Slugs.slugify(request.title()), courseRepository::existsBySlug));
        return toSummary(courseRepository.save(course));
    }

    @Transactional
    public CourseSummaryDto update(Long courseId, CourseRequest request) {
        Course course = requireOwned(courseId);
        applyRequest(course, request);
        return toSummary(course);
    }

    @Transactional
    public void delete(Long courseId) {
        courseRepository.delete(requireOwned(courseId));
    }

    @Transactional
    public CourseSummaryDto setPublished(Long courseId, boolean published) {
        Course course = requireOwned(courseId);
        course.setPublished(published);
        return toSummary(course);
    }

    // ---------- lecture ----------

    @Transactional(readOnly = true)
    public Page<CourseSummaryDto> catalog(String q, String language, Pageable pageable) {
        String normalizedQ = StringUtils.hasText(q) ? q.trim() : null;
        String normalizedLang = StringUtils.hasText(language) ? language : null;
        return courseRepository.searchPublished(normalizedQ, normalizedLang, pageable).map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public java.util.List<CourseSummaryDto> listByInstructor(Long instructorId) {
        return courseRepository.findByInstructorIdOrderByCreatedAtDesc(instructorId).stream()
                .map(this::toSummary)
                .toList();
    }

    /** Id d'un cours visible (publié, ou propriétaire/ADMIN) — 404 sinon. */
    @Transactional(readOnly = true)
    public Long publicIdBySlug(String slug) {
        Course course = courseRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Cours introuvable"));
        if (!course.isPublished() && !isOwnerOrAdmin(course)) {
            throw new ResourceNotFoundException("Cours introuvable");
        }
        return course.getId();
    }

    @Transactional(readOnly = true)
    public CourseDetailDto getDetailBySlug(String slug, boolean contentsVisible) {
        Course course = courseRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Cours introuvable"));
        boolean privileged = isOwnerOrAdmin(course);
        if (!course.isPublished() && !privileged) {
            throw new ResourceNotFoundException("Cours introuvable");
        }
        boolean showContents = contentsVisible || privileged;

        java.util.List<ChapterDto> chapters = course.getChapters().stream()
                .map(ch -> showContents
                        ? mapper.toChapterDto(ch)
                        : new ChapterDto(ch.getId(), ch.getTitle(), ch.getPosition(), java.util.List.of()))
                .toList();

        return new CourseDetailDto(course.getId(), course.getSlug(), course.getTitle(), course.getDescription(),
                course.getLanguage(), course.isPublished(), userService.displayNameById(course.getInstructorId()),
                course.getControlWeight(), course.getExamWeight(), course.getPassThreshold(), showContents, chapters);
    }

    // ---------- helpers inter-modules ----------

    /** Charge un cours existant sans contrôle de droits (usage interne / lecture publiée). */
    @Transactional(readOnly = true)
    public Course requireCourse(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Cours introuvable"));
    }

    /** Charge un cours et vérifie que l'utilisateur courant en est propriétaire (ou ADMIN). */
    @Transactional(readOnly = true)
    public Course requireOwned(Long courseId) {
        Course course = requireCourse(courseId);
        if (!isOwnerOrAdmin(course)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Ce cours ne vous appartient pas");
        }
        return course;
    }

    @Transactional(readOnly = true)
    public CourseSummaryDto summary(Long courseId) {
        return toSummary(requireCourse(courseId));
    }

    @Transactional(readOnly = true)
    public long contentCount(Long courseId) {
        return contentRepository.countByChapter_Course_Id(courseId);
    }

    /** Vrai si l'utilisateur courant est propriétaire du cours ou ADMIN (sans lever d'exception). */
    @Transactional(readOnly = true)
    public boolean isOwnerOrAdmin(Long courseId) {
        return courseRepository.findById(courseId).map(this::isOwnerOrAdmin).orElse(false);
    }

    /** Contexte textuel borné d'un cours (titre + description + chapitres + contenus TEXT), pour le chatbot. */
    @Transactional(readOnly = true)
    public String aiContext(Long courseId, int maxChars) {
        Course course = requireCourse(courseId);
        StringBuilder sb = new StringBuilder();
        sb.append("Titre : ").append(course.getTitle()).append('\n');
        if (course.getDescription() != null && !course.getDescription().isBlank()) {
            sb.append("Description : ").append(course.getDescription()).append('\n');
        }
        for (Chapter chapter : course.getChapters()) {
            sb.append("\n## ").append(chapter.getPosition()).append(". ").append(chapter.getTitle()).append('\n');
            for (Content content : chapter.getContents()) {
                sb.append("- ").append(content.getTitle());
                if (content.getType() == ContentType.TEXT && content.getTextBody() != null) {
                    sb.append(" : ").append(content.getTextBody());
                }
                sb.append('\n');
                if (sb.length() >= maxChars) {
                    return sb.substring(0, maxChars) + "…";
                }
            }
        }
        return sb.toString();
    }

    // ---------- privé ----------

    private boolean isOwnerOrAdmin(Course course) {
        Long userId = CurrentUser.optionalId();
        return CurrentUser.isAdmin() || (userId != null && course.getInstructorId().equals(userId));
    }

    private void applyRequest(Course course, CourseRequest request) {
        course.setTitle(request.title().trim());
        course.setDescription(request.description());
        if (request.language() != null) course.setLanguage(request.language());
        if (request.controlWeight() != null) course.setControlWeight(request.controlWeight());
        if (request.examWeight() != null) course.setExamWeight(request.examWeight());
        if (request.passThreshold() != null) course.setPassThreshold(request.passThreshold());
        if (course.getControlWeight() + course.getExamWeight() != 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "control_weight + exam_weight doit valoir 100");
        }
    }

    private CourseSummaryDto toSummary(Course course) {
        return new CourseSummaryDto(course.getId(), course.getSlug(), course.getTitle(), course.getDescription(),
                course.getLanguage(), course.isPublished(),
                userService.displayNameById(course.getInstructorId()), course.getChapters().size());
    }
}
