package fr.estela.peerframe.device.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import fr.estela.peerframe.device.entity.ProviderEntity;
import fr.estela.peerframe.device.repository.MediaRepository;
import fr.estela.peerframe.device.repository.ProviderRepository;

@Component
public class DownloadService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadService.class);
    
    @Autowired
    private MediaRepository mediaRepository;
    
    @Autowired
    private ProviderRepository providerRepository;
    
    @Scheduled(fixedDelay = 3600000, initialDelay = 1000)
    public void scheduleFixedRateWithInitialDelayTask() throws Exception {
        
        List<ProviderEntity> providerEntities = providerRepository.findAll();
        for (final ProviderEntity providerEntity : providerEntities) {            
            try {
                LOGGER.info("Download initiated for: {}", providerEntity);
                DownloadManager manager = providerEntity.getDownloadManagerInstance();
                manager.downloadAndSaveMedias(providerEntity, providerRepository, mediaRepository);
            }
            catch(Exception e) {
                LOGGER.error("Download failed", e);
            }
        }
    }
}
