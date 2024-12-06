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
import java.util.List;
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
        System.out.println(project.getFileHash());

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
            System.out.println(downloadUrl);
            String subject = "Verification needed for project";
            String text = "Hello,\n please see the link:\n" + downloadUrl + "\nto verify the project. \nUse the hash for verfication: " + project.getFileHash();
            //emailService.sendEmail("your-email@mail.com", subject, text);
            //replace hardcoded email string with user.getEmail() to send mail to the actual contributors (not recommended for research -> use your own mail to get all links created)
        }
    }
    public List<Project> getProjectByName(String fileName) {
        return projectRepository.findByProjectName(fileName);
    }

}

