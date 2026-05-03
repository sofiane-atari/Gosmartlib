package edu.ap.bib_backend.mapper.lessontips;

import edu.ap.bib_backend.dto.lessontips.LessonTipBookSummaryResponseDto;
import edu.ap.bib_backend.dto.lessontips.LessonTipPreviewResponseDto;
import edu.ap.bib_backend.dto.lessontips.LessonTipResponseDto;
import edu.ap.bib_backend.jpa.entity.lessontips.LessonTip;
import edu.ap.bib_backend.jpa.entity.lessontips.LessonTipBookLink;
import edu.ap.bib_backend.jpa.enums.Role;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(componentModel = "spring")
public interface LessonTipMapper {

    @Mapping(target = "linkedBooks", source = "lessonTip.bookLinks")
    @Mapping(target = "createdByRole", source = "createdByRole")
    LessonTipResponseDto toResponse(LessonTip lessonTip, Role createdByRole);

    List<LessonTipPreviewResponseDto> toPreviewResponseList(List<LessonTip> lessonTips);

    LessonTipPreviewResponseDto toPreviewResponse(LessonTip lessonTip);
    
    @Mapping(target = "id", source = "book.id")
    @Mapping(target = "title", source = "book.title")
    @Mapping(target = "author", source = "book.author")
    @Mapping(target = "imageUrl", source = "book.imageUrl")
    LessonTipBookSummaryResponseDto toBookSummaryResponse(LessonTipBookLink bookLink);
}