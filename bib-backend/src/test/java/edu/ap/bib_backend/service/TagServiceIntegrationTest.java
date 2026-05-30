package edu.ap.bib_backend.service;

import edu.ap.bib_backend.dto.PageDTO;
import edu.ap.bib_backend.dto.TagDTO;
import edu.ap.bib_backend.dto.TagStatsDTO;
import edu.ap.bib_backend.jpa.entity.RoleEntity;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.enums.Role;
import edu.ap.bib_backend.jpa.repository.RoleEntityRepository;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import edu.ap.bib_backend.jpa.repository.TagRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.service.user.CurrentUserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

@DataJpaTest
@Import(TagService.class)
@ActiveProfiles("test")
class TagServiceIntegrationTest {

    @Autowired
    private TagService tagService;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleEntityRepository roleRepository;

    @MockitoBean
    private CurrentUserService currentUserService;

    private Authentication schoolAAuthentication;
    private Authentication schoolBAuthentication;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        tagRepository.deleteAll();
        schoolRepository.deleteAll();

        RoleEntity librarianRole = new RoleEntity();
        librarianRole.setRole(Role.LIBRARIAN);
        roleRepository.save(librarianRole);

        School schoolA = createSchool("school-a.smartschool.be", "School A");
        School schoolB = createSchool("school-b.smartschool.be", "School B");

        createUser("librarian-a", librarianRole, schoolA);
        createUser("librarian-b", librarianRole, schoolB);

        schoolAAuthentication = new UsernamePasswordAuthenticationToken("librarian-a", null);
        schoolBAuthentication = new UsernamePasswordAuthenticationToken("librarian-b", null);

        when(currentUserService.getCurrentUserSchool(schoolAAuthentication)).thenReturn(schoolA);
        when(currentUserService.getCurrentUserSchool(schoolBAuthentication)).thenReturn(schoolB);
    }

    private School createSchool(String domainName, String name) {
        School school = new School();
        school.setDomainName(domainName);
        school.setName(name);
        return schoolRepository.save(school);
    }

    private void createUser(String id, RoleEntity role, School school) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setSchool(school);
        userRepository.save(user);
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

        TagDTO createdTag = tagService.createTag(request, schoolAAuthentication);

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

        TagDTO createdTag = tagService.createTag(request, schoolAAuthentication);

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

        assertThatThrownBy(() -> tagService.createTag(request, schoolAAuthentication))
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
        ), schoolAAuthentication);

        TagDTO duplicate = new TagDTO(
                null,
                "fantasy",
                "Andere beschrijving",
                null,
                null
        );

        assertThatThrownBy(() -> tagService.createTag(duplicate, schoolAAuthentication))
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
        ), schoolAAuthentication);

        TagDTO updateRequest = new TagDTO(
                createdTag.id(),
                "Nieuw",
                "Nieuwe beschrijving",
                null,
                null
        );

        TagDTO updatedTag = tagService.updateTag(createdTag.id(), updateRequest, schoolAAuthentication);

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

        assertThatThrownBy(() -> tagService.updateTag(999L, updateRequest, schoolAAuthentication))
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
        ), schoolAAuthentication);

        tagService.createTag(new TagDTO(
                null,
                "Thriller",
                "Spannende boeken",
                null,
                null
        ), schoolAAuthentication);

        TagDTO updateRequest = new TagDTO(
                first.id(),
                "Thriller",
                "Nieuwe beschrijving",
                null,
                null
        );

        assertThatThrownBy(() -> tagService.updateTag(first.id(), updateRequest, schoolAAuthentication))
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
        ), schoolAAuthentication);

        tagService.deleteTag(createdTag.id(), schoolAAuthentication);

        assertThat(tagRepository.existsById(createdTag.id())).isFalse();
    }

    @Test
    void deleteTag_shouldThrowException_whenTagDoesNotExist() {
        assertThatThrownBy(() -> tagService.deleteTag(999L, schoolAAuthentication))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tag niet gevonden");
    }

    @Test
    void getTagsByPage_shouldReturnPagedTags() {
        tagService.createTag(new TagDTO(null, "Fantasy", "Fantasy boeken", null, null), schoolAAuthentication);
        tagService.createTag(new TagDTO(null, "Thriller", "Thriller boeken", null, null), schoolAAuthentication);
        tagService.createTag(new TagDTO(null, "Romantiek", "Romantische boeken", null, null), schoolAAuthentication);

        PageDTO<TagDTO> page = tagService.getTagsByPage(
                0,
                2,
                null,
                null,
                true,
                schoolAAuthentication
        );

        assertThat(page.data()).hasSize(2);
        assertThat(page.meta().currentPage()).isEqualTo(0);
        assertThat(page.meta().pageSize()).isEqualTo(2);
        assertThat(page.meta().totalItems()).isEqualTo(3);
        assertThat(page.meta().totalPages()).isEqualTo(2);
    }

    @Test
    void getTagsByPage_shouldFilterBySearch() {
        tagService.createTag(new TagDTO(null, "Fantasy", "Fantasy boeken", null, null), schoolAAuthentication);
        tagService.createTag(new TagDTO(null, "Thriller", "Thriller boeken", null, null), schoolAAuthentication);

        PageDTO<TagDTO> page = tagService.getTagsByPage(
                0,
                10,
                "fant",
                null,
                true,
                schoolAAuthentication
        );

        assertThat(page.data()).hasSize(1);
        assertThat(page.data().get(0).name()).isEqualTo("Fantasy");
    }

    @Test
    void getTagStats_shouldReturnStats() {
        tagService.createTag(new TagDTO(null, "Fantasy", "Fantasy boeken", null, null), schoolAAuthentication);
        tagService.createTag(new TagDTO(null, "Thriller", "Thriller boeken", null, null), schoolAAuthentication);

        TagStatsDTO stats = tagService.getTagStats(schoolAAuthentication);

        assertThat(stats.totalItems()).isEqualTo(2L);
        assertThat(stats.unActiveTags()).isEqualTo(2L);
    }

    @Test
    void createTag_shouldAllowSameNameInDifferentSchools() {
        TagDTO schoolATag = tagService.createTag(
                new TagDTO(null, "Fantasy", "Fantasy boeken", null, null),
                schoolAAuthentication);
        TagDTO schoolBTag = tagService.createTag(
                new TagDTO(null, "Fantasy", "Andere fantasy boeken", null, null),
                schoolBAuthentication);

        assertThat(schoolATag.id()).isNotEqualTo(schoolBTag.id());
        assertThat(schoolATag.name()).isEqualTo("Fantasy");
        assertThat(schoolBTag.name()).isEqualTo("Fantasy");
        assertThat(tagRepository.count()).isEqualTo(2);
    }

    @Test
    void createTag_shouldRejectDuplicateNameWithinSameSchool() {
        tagService.createTag(
                new TagDTO(null, "Fantasy", "Fantasy boeken", null, null),
                schoolAAuthentication);

        assertThatThrownBy(() -> tagService.createTag(
                new TagDTO(null, "fantasy", "Andere beschrijving", null, null),
                schoolAAuthentication))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Deze tag bestaat al");
    }

    @Test
    void getTagsByPage_shouldOnlyReturnTagsForCurrentSchool() {
        tagService.createTag(new TagDTO(null, "Fantasy", "Fantasy boeken", null, null), schoolAAuthentication);
        tagService.createTag(new TagDTO(null, "Thriller", "Thriller boeken", null, null), schoolAAuthentication);
        tagService.createTag(new TagDTO(null, "Romantiek", "Romantische boeken", null, null), schoolBAuthentication);

        PageDTO<TagDTO> page = tagService.getTagsByPage(
                0,
                10,
                null,
                null,
                true,
                schoolAAuthentication);

        assertThat(page.data())
                .extracting(TagDTO::name)
                .containsExactlyInAnyOrder("Fantasy", "Thriller");
        assertThat(page.meta().totalItems()).isEqualTo(2);
    }

    @Test
    void updateTag_shouldNotFindTagFromOtherSchool() {
        TagDTO schoolBTag = tagService.createTag(
                new TagDTO(null, "Fantasy", "Fantasy boeken", null, null),
                schoolBAuthentication);

        assertThatThrownBy(() -> tagService.updateTag(
                schoolBTag.id(),
                new TagDTO(schoolBTag.id(), "Nieuw", "Nieuwe beschrijving", null, null),
                schoolAAuthentication))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tag niet gevonden");
    }

    @Test
    void deleteTag_shouldNotDeleteTagFromOtherSchool() {
        TagDTO schoolBTag = tagService.createTag(
                new TagDTO(null, "Fantasy", "Fantasy boeken", null, null),
                schoolBAuthentication);

        assertThatThrownBy(() -> tagService.deleteTag(schoolBTag.id(), schoolAAuthentication))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tag niet gevonden");

        assertThat(tagRepository.existsById(schoolBTag.id())).isTrue();
    }

    @Test
    void getTagStats_shouldOnlyCountTagsForCurrentSchool() {
        tagService.createTag(new TagDTO(null, "Fantasy", "Fantasy boeken", null, null), schoolAAuthentication);
        tagService.createTag(new TagDTO(null, "Thriller", "Thriller boeken", null, null), schoolAAuthentication);
        tagService.createTag(new TagDTO(null, "Romantiek", "Romantische boeken", null, null), schoolBAuthentication);

        TagStatsDTO stats = tagService.getTagStats(schoolAAuthentication);

        assertThat(stats.totalItems()).isEqualTo(2L);
        assertThat(stats.unActiveTags()).isEqualTo(2L);
    }
}
