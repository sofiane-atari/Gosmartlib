package edu.ap.bib_backend.service;

import org.apache.xmlbeans.impl.xb.xsdschema.GroupRef;
import org.springframework.stereotype.Service;

import edu.ap.bib_backend.jpa.entity.Group;
import edu.ap.bib_backend.jpa.repository.GroupRepository;

@Service
public class GroupService {
    private final GroupRepository groupRepository;
   
    public GroupService(GroupRepository groupRepository) {
        this.groupRepository = groupRepository;
    }


    public Group getGroup(String groupName) {
        return groupRepository.findById(groupName)
            .orElseThrow(() -> new RuntimeException("Groep niet gevonden"));
    }

    public Group createGroup(String groupName) throws RuntimeException {
        if (groupRepository.existsById(groupName)) {
            throw new RuntimeException("Groep bestaat al");
        }

        Group group = new Group();
        group.setName(groupName);
        return groupRepository.save(group);
    }
}