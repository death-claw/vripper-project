package tn.mnlr.vripper.web.restendpoints;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.mnlr.vripper.SpringContext;

@RestController
@CrossOrigin(value = "*")
public class ShutdownRestEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(ShutdownRestEndpoint.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity handleException(Exception e) {
        logger.error("Error when process request", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(e.getMessage());
    }

    @PostMapping("/shutdown")
    @ResponseStatus(value = HttpStatus.OK)
    public ResponseEntity shutdown() {
        logger.info("Shutting down request received");
        new Thread(SpringContext::close).start();
        return new ResponseEntity(HttpStatus.OK);
    }

}
