package com.educa.backend.course;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.educa.backend.course.dto.ChapterDto;
import com.educa.backend.course.dto.ContentDto;

@Mapper(componentModel = "spring")
public interface CourseMapper {

    @Mapping(target = "hasFile", expression = "java(content.hasFile())")
    ContentDto toContentDto(Content content);

    List<ContentDto> toContentDtos(List<Content> contents);

    ChapterDto toChapterDto(Chapter chapter);
}
