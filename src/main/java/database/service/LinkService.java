package database.service;

import database.models.Link;
import database.repository.LinkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LinkService {

    private final LinkRepository linkRepository;

    @Autowired
    public LinkService(LinkRepository linkRepository) {
        this.linkRepository = linkRepository;
    }

    public List<Link> getLinksByToken(String token) {
        return linkRepository.findByToken(token);
    }
    public List<Link> getLinksByProjectID(Long id) {
        return linkRepository.findLinksByProjectID(id);
    }
    public List<Link> getLinksByUserID(Long id) {
        return linkRepository.findLinksByUserID(id);
    }
}
