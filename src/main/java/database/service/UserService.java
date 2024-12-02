package database.service;

import database.models.Link;
import database.models.User;
import database.repository.LinkRepository;
import database.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    public void setUserSignature(UserSignatureRequest userSignatureRequest){
        Link link = userSignatureRequest.getLink();
        User user = link.getUser();
        if (user == null) {
            throw new IllegalStateException("No user associated with the provided token");
        }
        user.setSignature(userSignatureRequest.getSignature());
        user.setPublicKey(userSignatureRequest.getPublicKey());
        user.setTimestamp(userSignatureRequest.getTimestamp());
        userRepository.save(user);
    }
}
