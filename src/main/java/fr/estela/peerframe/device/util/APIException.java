package fr.estela.peerframe.device.util;

public class APIException extends Exception {

    private static final long serialVersionUID = 8894580920018294756L;

    private int code;
    private String message;
    
    public APIException(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }
    
    public void setCode(int code) {
        this.code = code;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}