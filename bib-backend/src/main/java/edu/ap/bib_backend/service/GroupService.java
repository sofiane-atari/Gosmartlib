package edu.ap.bib_backend.service;

import org.springframework.stereotype.Service;

import edu.ap.bib_backend.jpa.entity.Group;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.repository.GroupRepository;

@Service
public class GroupService {

    private final GroupRepository groupRepository;

    public GroupService(GroupRepository groupRepository) {
        this.groupRepository = groupRepository;
    }

    public Group getGroup(Long schoolId, String groupName) {
        return groupRepository.findBySchoolIdAndName(schoolId, groupName)
                .orElseThrow(() -> new RuntimeException("Groep niet gevonden"));
    }

    public Group getOrCreateGroup(Long schoolId, String groupName) {
        return groupRepository.findBySchoolIdAndName(schoolId, groupName)
                .orElseGet(() -> createGroup(schoolId, groupName));
    }

    public Group createGroup(Long schoolId, String groupName) {
        if (groupRepository.existsBySchoolIdAndName(schoolId, groupName)) {
            throw new RuntimeException("Groep bestaat al");
        }

        Group group = new Group();
        group.setName(groupName);

        School school = new School();
        school.setId(schoolId);
        group.setSchool(school);

        return groupRepository.save(group);
    }
}
