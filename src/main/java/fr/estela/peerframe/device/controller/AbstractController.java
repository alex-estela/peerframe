package fr.estela.peerframe.device.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import fr.estela.peerframe.device.util.APIException;

public abstract class AbstractController {
    
    public APIException newBadRequestException(String message) {
        return new APIException(400, message);
    }

    public <T> ResponseEntity<T> populateResponse(int status, T entity) {
        return new ResponseEntity<T>(entity, HttpStatus.valueOf(status));
    }

    public <T> ResponseEntity<T> populateRetrievedResponse(T entity) {
        return populateResponse(200, entity);
    }

    public <T> ResponseEntity<T> populateCreatedResponse(T entity) {
        return populateResponse(201, entity);
    }

    public ResponseEntity<Void> populateDeletedResponse() {
        return populateResponse(204, null);
    }
}