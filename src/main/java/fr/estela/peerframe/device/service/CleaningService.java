package fr.estela.peerframe.device.service;

import java.io.File;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import fr.estela.peerframe.device.Application;

@Component
public class CleaningService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CleaningService.class);
    
    @PostConstruct
    public void cleanTmpFolder() {
        try {
            LOGGER.info("Cleaning folder: {}", Application.getTempFolder());
            File folder = new File(Application.getTempFolder());
            for (String fileName : folder.list()) {
                LOGGER.info("> Deleting {}", fileName);
                new File(fileName).delete();
            }
        }
        catch(Exception e) {
            LOGGER.error("Could not clean folder", e);
        }
    }
}
