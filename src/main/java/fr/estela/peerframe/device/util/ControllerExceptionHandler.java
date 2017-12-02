package fr.estela.peerframe.device.util;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@ControllerAdvice
public class ControllerExceptionHandler {
    
    private Logger LOGGER = LoggerFactory.getLogger(ControllerExceptionHandler.class);
    
    @SuppressWarnings("unused")
    @ExceptionHandler(Exception.class)
    public @ResponseBody APIException handleAPIException(final Exception e) {
        LOGGER.error("Printing handled exception", e);
        final HttpServletResponse response = ((ServletRequestAttributes) RequestContextHolder
            .currentRequestAttributes()).getResponse();
        if (e instanceof APIException) return (APIException) e;
        return new APIException(500, e.getMessage());
    }
}