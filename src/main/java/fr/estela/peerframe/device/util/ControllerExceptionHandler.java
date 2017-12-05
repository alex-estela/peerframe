package fr.estela.peerframe.device.util;

import javax.servlet.http.HttpServletResponse;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@ControllerAdvice
public class ControllerExceptionHandler {
    
    @SuppressWarnings("unused")
    @ExceptionHandler(Exception.class)
    public @ResponseBody APIException handleAPIException(final Exception e) {
        final HttpServletResponse response = ((ServletRequestAttributes) RequestContextHolder
            .currentRequestAttributes()).getResponse();
        if (e instanceof APIException) return (APIException) e;
        return new APIException(500, e.getMessage());
    }
}