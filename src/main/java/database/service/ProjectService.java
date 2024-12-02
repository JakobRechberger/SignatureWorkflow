package database.service;

import database.models.Link;
import database.models.Project;
import database.models.User;
import database.repository.LinkRepository;
import database.repository.ProjectRepository;
import database.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LinkRepository linkRepository;

    @Autowired
    private EmailService emailService;
    @Transactional
    public void createProjectWithUsers(ProjectRequest projectRequest)  {
        Project project = new Project();
        project.setFileName(projectRequest.getFileName());
        project.setFileHash(projectRequest.getFileHash());
        project.setFileData(projectRequest.getFile());
        project = projectRepository.save(project);

        for (String email : projectRequest.getUserEmails()) {
            User user = new User();
            user.setEmail(email);
            userRepository.save(user);
            String token = UUID.randomUUID().toString();
            Link link = new Link();
            link.setToken(token);
            link.setUser(user);
            link.setProject(project);
            link.setExpiryTimestamp(LocalDateTime.now().plusDays(7));
            linkRepository.save(link);
            String downloadUrl = "http://localhost:3000/contributor?token=" + token;
            String subject = "Verification needed for project";
            String text = "Hello,\n please see the link:\n" + downloadUrl + "\nto verify the project. \nUse the hash for verfication: " + project.getFileHash();
            emailService.sendEmail("testing.software0611@gmail.com", subject, text);
        }
    }

}

