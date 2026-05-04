package edu.ap.bib_backend.service;

import edu.ap.bib_backend.dto.PageDTO;
import edu.ap.bib_backend.dto.TagDTO;
import edu.ap.bib_backend.dto.TagStatsDTO;
import edu.ap.bib_backend.jpa.repository.TagRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Import(TagService.class)
@ActiveProfiles("test")
class TagServiceIntegrationTest {

    @Autowired
    private TagService tagService;

    @Autowired
    private TagRepository tagRepository;

    @BeforeEach
    void setUp() {
        tagRepository.deleteAll();
    }

    @Test
    void createTag_shouldCreateTag() {
        TagDTO request = new TagDTO(
                null,
                "Populair",
                "Veel gelezen boeken",
                null,
                null
        );

        TagDTO createdTag = tagService.createTag(request);

        assertThat(createdTag.id()).isNotNull();
        assertThat(createdTag.name()).isEqualTo("Populair");
        assertThat(createdTag.description()).isEqualTo("Veel gelezen boeken");
        assertThat(createdTag.bookCount()).isEqualTo(0L);
        assertThat(tagRepository.count()).isEqualTo(1);
    }

    @Test
    void createTag_shouldTrimNameAndDescription() {
        TagDTO request = new TagDTO(
                null,
                "  Fantasy  ",
                "  Magische boeken  ",
                null,
                null
        );

        TagDTO createdTag = tagService.createTag(request);

        assertThat(createdTag.name()).isEqualTo("Fantasy");
        assertThat(createdTag.description()).isEqualTo("Magische boeken");
    }

    @Test
    void createTag_shouldThrowException_whenNameIsBlank() {
        TagDTO request = new TagDTO(
                null,
                "   ",
                "Beschrijving",
                null,
                null
        );

        assertThatThrownBy(() -> tagService.createTag(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Naam is verplicht");
    }

    @Test
    void createTag_shouldThrowException_whenNameAlreadyExists() {
        tagService.createTag(new TagDTO(
                null,
                "Fantasy",
                "Fantasy boeken",
                null,
                null
        ));

        TagDTO duplicate = new TagDTO(
                null,
                "fantasy",
                "Andere beschrijving",
                null,
                null
        );

        assertThatThrownBy(() -> tagService.createTag(duplicate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Deze tag bestaat al");
    }

    @Test
    void updateTag_shouldUpdateExistingTag() {
        TagDTO createdTag = tagService.createTag(new TagDTO(
                null,
                "Oud",
                "Oude beschrijving",
                null,
                null
        ));

        TagDTO updateRequest = new TagDTO(
                createdTag.id(),
                "Nieuw",
                "Nieuwe beschrijving",
                null,
                null
        );

        TagDTO updatedTag = tagService.updateTag(createdTag.id(), updateRequest);

        assertThat(updatedTag.id()).isEqualTo(createdTag.id());
        assertThat(updatedTag.name()).isEqualTo("Nieuw");
        assertThat(updatedTag.description()).isEqualTo("Nieuwe beschrijving");
    }

    @Test
    void updateTag_shouldThrowException_whenTagDoesNotExist() {
        TagDTO updateRequest = new TagDTO(
                999L,
                "Bestaat niet",
                "Beschrijving",
                null,
                null
        );

        assertThatThrownBy(() -> tagService.updateTag(999L, updateRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tag niet gevonden");
    }

    @Test
    void updateTag_shouldThrowException_whenUpdatedNameAlreadyExists() {
        TagDTO first = tagService.createTag(new TagDTO(
                null,
                "Fantasy",
                "Fantasy boeken",
                null,
                null
        ));

        tagService.createTag(new TagDTO(
                null,
                "Thriller",
                "Spannende boeken",
                null,
                null
        ));

        TagDTO updateRequest = new TagDTO(
                first.id(),
                "Thriller",
                "Nieuwe beschrijving",
                null,
                null
        );

        assertThatThrownBy(() -> tagService.updateTag(first.id(), updateRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Deze tag bestaat al");
    }

    @Test
    void deleteTag_shouldDeleteTag() {
        TagDTO createdTag = tagService.createTag(new TagDTO(
                null,
                "Te verwijderen",
                "Beschrijving",
                null,
                null
        ));

        tagService.deleteTag(createdTag.id());

        assertThat(tagRepository.existsById(createdTag.id())).isFalse();
    }

    @Test
    void deleteTag_shouldThrowException_whenTagDoesNotExist() {
        assertThatThrownBy(() -> tagService.deleteTag(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tag niet gevonden");
    }

    @Test
    void getTagsByPage_shouldReturnPagedTags() {
        tagService.createTag(new TagDTO(null, "Fantasy", "Fantasy boeken", null, null));
        tagService.createTag(new TagDTO(null, "Thriller", "Thriller boeken", null, null));
        tagService.createTag(new TagDTO(null, "Romantiek", "Romantische boeken", null, null));

        PageDTO<TagDTO> page = tagService.getTagsByPage(
                0,
                2,
                null,
                null,
                true
        );

        assertThat(page.data()).hasSize(2);
        assertThat(page.meta().currentPage()).isEqualTo(0);
        assertThat(page.meta().pageSize()).isEqualTo(2);
        assertThat(page.meta().totalItems()).isEqualTo(3);
        assertThat(page.meta().totalPages()).isEqualTo(2);
    }

    @Test
    void getTagsByPage_shouldFilterBySearch() {
        tagService.createTag(new TagDTO(null, "Fantasy", "Fantasy boeken", null, null));
        tagService.createTag(new TagDTO(null, "Thriller", "Thriller boeken", null, null));

        PageDTO<TagDTO> page = tagService.getTagsByPage(
                0,
                10,
                "fant",
                null,
                true
        );

        assertThat(page.data()).hasSize(1);
        assertThat(page.data().get(0).name()).isEqualTo("Fantasy");
    }

    @Test
    void getTagStats_shouldReturnStats() {
        tagService.createTag(new TagDTO(null, "Fantasy", "Fantasy boeken", null, null));
        tagService.createTag(new TagDTO(null, "Thriller", "Thriller boeken", null, null));

        TagStatsDTO stats = tagService.getTagStats();

        assertThat(stats.totalItems()).isEqualTo(2L);
        assertThat(stats.unActiveTags()).isEqualTo(2L);
    }
}