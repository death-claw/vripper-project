package tn.mnlr.vripper.web.restendpoints;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@CrossOrigin(value = "*")
public class UserRestEndpoint {

    @RequestMapping("/user")
    public Principal user(Principal user) {
        return user;
    }

}
