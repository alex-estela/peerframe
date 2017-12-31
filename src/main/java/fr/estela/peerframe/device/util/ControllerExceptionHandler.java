package fr.estela.peerframe.device.util;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
public class ControllerExceptionHandler {
    
    public class APIError {    
        private int code;
        private String message;        
        public APIError(int code, String message) {
            this.code = code;
            this.message = message;
        }
        public APIError(APIException exception) {
            this.code = exception.getCode();
            this.message = exception.getMessage();
        }        
        public int getCode() {
            return code;
        }       
        public String getMessage() {
            return message;
        }
    }
    
    @ExceptionHandler(Exception.class)
    public @ResponseBody APIError handleException(final Exception e) {
        if (e instanceof APIException) return new APIError((APIException) e);
        return new APIError(500, e.getMessage());
    }
}