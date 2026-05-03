package edu.ap.bib_backend.jpa.repository.lessontips;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.ap.bib_backend.jpa.entity.lessontips.LessonTipAttachment;

public interface LessonTipAttachmentRepository extends JpaRepository<LessonTipAttachment, Long> {

    List<LessonTipAttachment> findAllByLessonTipIdOrderByUploadedAtDesc(Long LessonTipId);

    Optional<LessonTipAttachment> findByIdAndLessonTipId(Long id, Long LessonTipId);
}