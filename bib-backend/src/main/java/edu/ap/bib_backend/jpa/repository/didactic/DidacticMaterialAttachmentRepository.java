package edu.ap.bib_backend.jpa.repository.didactic;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.ap.bib_backend.jpa.entity.didactic.DidacticMaterialAttachment;

public interface DidacticMaterialAttachmentRepository extends JpaRepository<DidacticMaterialAttachment, Long> {

    List<DidacticMaterialAttachment> findAllByDidacticMaterialIdOrderByUploadedAtDesc(Long didacticMaterialId);

    Optional<DidacticMaterialAttachment> findByIdAndDidacticMaterialId(Long id, Long didacticMaterialId);
}