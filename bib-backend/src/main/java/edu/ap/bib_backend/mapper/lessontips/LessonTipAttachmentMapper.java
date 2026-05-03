package edu.ap.bib_backend.mapper.lessontips;

import edu.ap.bib_backend.dto.lessontips.LessonTipAttachmentResponseDto;
import edu.ap.bib_backend.jpa.entity.lessontips.LessonTipAttachment;

import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface LessonTipAttachmentMapper {

    LessonTipAttachmentResponseDto toResponse(LessonTipAttachment attachment);

    List<LessonTipAttachmentResponseDto> toResponseList(List<LessonTipAttachment> attachments);
}