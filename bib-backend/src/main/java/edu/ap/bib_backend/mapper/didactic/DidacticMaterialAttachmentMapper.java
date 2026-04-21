package edu.ap.bib_backend.mapper.didactic;

import edu.ap.bib_backend.dto.didactic.DidacticMaterialAttachmentResponseDto;
import edu.ap.bib_backend.jpa.entity.didactic.DidacticMaterialAttachment;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DidacticMaterialAttachmentMapper {

    DidacticMaterialAttachmentResponseDto toResponse(DidacticMaterialAttachment attachment);

    List<DidacticMaterialAttachmentResponseDto> toResponseList(List<DidacticMaterialAttachment> attachments);
}