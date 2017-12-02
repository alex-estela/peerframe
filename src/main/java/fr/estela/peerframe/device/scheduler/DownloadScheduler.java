package fr.estela.peerframe.device.scheduler;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import fr.estela.peerframe.device.Application;
import fr.estela.peerframe.device.entity.ProviderEntity;
import fr.estela.peerframe.device.repository.ProviderRepository;

@Component
public class DownloadScheduler {

    private static Logger LOGGER = LoggerFactory.getLogger(Application.class);
    
    @Autowired
    private ProviderRepository providerRepository;
    
    @Scheduled(fixedDelay = 600000, initialDelay = 1000)
    public void scheduleFixedRateWithInitialDelayTask() throws Exception {
        
        // TODO clean /peerframe/tmp folder at startup
        
        List<ProviderEntity> providerEntities = providerRepository.findAll();
        for (final ProviderEntity providerEntity : providerEntities) {
            
            LOGGER.info("Downloading for provider: " + providerEntity);
            
        }
    }
}
