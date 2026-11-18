package be.leuven.leuvengo.web.error;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Thrown when a requested entity does not exist; rendered as a 404. */
public class NotFoundException extends ResponseStatusException {

    public NotFoundException(String entity, Object id) {
        super(HttpStatus.NOT_FOUND, entity + " " + id + " not found");
    }
}
