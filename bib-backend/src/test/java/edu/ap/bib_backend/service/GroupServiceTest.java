package edu.ap.bib_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.ap.bib_backend.jpa.entity.Group;
import edu.ap.bib_backend.jpa.repository.GroupRepository;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock
    private GroupRepository groupRepository;

    @InjectMocks
    private GroupService groupService;

    @Test
    void GivenExistingGroupName_WhenGetGroup_ThenReturnGroup() {
        // Arrange
        String groupName = "1A";
        Group existingGroup = new Group();
        existingGroup.setName(groupName);
        when(groupRepository.findById(groupName)).thenReturn(Optional.of(existingGroup));

        // Act
        Group result = groupService.getGroup(groupName);

        // Assert
        assertNotNull(result);
        assertEquals(groupName, result.getName());
        verify(groupRepository, times(1)).findById(groupName);
    }

    @Test
    void GivenNonExistingGroupName_WhenGetGroup_ThenThrowRuntimeException() {
        // Arrange
        String groupName = "9Z";
        when(groupRepository.findById(groupName)).thenReturn(Optional.empty());

        // Act
        RuntimeException exception = assertThrows(RuntimeException.class, () -> groupService.getGroup(groupName));

        // Assert
        assertEquals("Groep niet gevonden", exception.getMessage());
        verify(groupRepository, times(1)).findById(groupName);
    }

    @Test
    void GivenGroupNameThatDoesNotExist_WhenCreateGroup_ThenSaveAndReturnGroup() {
        // Arrange
        String groupName = "2B";
        when(groupRepository.existsById(groupName)).thenReturn(false);
        when(groupRepository.save(any(Group.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Group result = groupService.createGroup(groupName);

        // Assert
        assertNotNull(result);
        assertEquals(groupName, result.getName());
        verify(groupRepository, times(1)).existsById(groupName);
        verify(groupRepository, times(1)).save(any(Group.class));
    }

    @Test
    void GivenGroupNameThatAlreadyExists_WhenCreateGroup_ThenThrowRuntimeException() {
        // Arrange
        String groupName = "2B";
        when(groupRepository.existsById(groupName)).thenReturn(true);

        // Act
        RuntimeException exception = assertThrows(RuntimeException.class, () -> groupService.createGroup(groupName));

        // Assert
        assertEquals("Groep bestaat al", exception.getMessage());
        verify(groupRepository, times(1)).existsById(groupName);
        verify(groupRepository, never()).save(any(Group.class));
    }
}