package fr.estela.peerframe.device.service;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import fr.estela.peerframe.api.model.Event.TypeEnum;
import fr.estela.peerframe.device.entity.ProviderEntity;
import fr.estela.peerframe.device.repository.MediaRepository;
import fr.estela.peerframe.device.repository.ProviderRepository;
import fr.estela.peerframe.device.util.EventCache;

@Component
public class DownloadService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadService.class);
    private static final int INITIAL_DELAY = 30000;
    private static final int RUN_PERIOD = 3600000;

    private Timer timer = new Timer(true);
    private DownloadTimerTask currentTask;
    private boolean alreadyQueued = false;
    private String providerInProgress = null;
    
    @Autowired 
    private ApplicationContext applicationContext;
    
    @Autowired
    private MediaRepository mediaRepository;

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    private EventCache eventCache;
    
    public String getProviderInProgress() {
        return providerInProgress;
    }

    @PostConstruct
    public void initService() {
        LOGGER.info("Starting Download Service...");
        currentTask = applicationContext.getBean(DownloadTimerTask.class);
        timer.schedule(currentTask, INITIAL_DELAY, RUN_PERIOD);
        LOGGER.info("Starting Download Service OK");
    }

    public synchronized void triggerService() {
        if (!alreadyQueued) {
            try {
                LOGGER.info("Triggering Download Service...");
                currentTask.cancel();
                currentTask = applicationContext.getBean(DownloadTimerTask.class);
                timer.schedule(currentTask, 0, RUN_PERIOD);
                LOGGER.info("Triggering Download Service OK");
                alreadyQueued = true;
            }
            catch(Exception e) {
                LOGGER.error("Download triggering failed", e);
            }
        }
        else LOGGER.info("Ignoring Download Service trigger as there is already one queued");
    }

    @Component
    @Scope("prototype")
    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public class DownloadTimerTask extends TimerTask {
        @Override
        public void run() {
            providerInProgress = null;
            try {
                List<ProviderEntity> providerEntities = providerRepository.findAll();
                LOGGER.info("Download loop initiated with {} provider(s)", providerEntities.size());
                for (final ProviderEntity providerEntity : providerEntities) {
                    try {
                        if (providerRepository.findOne(providerEntity.getId()) != null) { // check that the entity still exists at this point
                            LOGGER.info("Download initiated for: {}", providerEntity);
                            providerInProgress = providerEntity.toString();
                            AbstractDownloadManager manager = providerEntity.getDownloadManagerInstance();
                            manager.downloadAndSaveMedias(providerEntity, providerRepository, mediaRepository);
                        }
                    }
                    catch (Exception e) {
                        eventCache.addEvent(e.getClass() + ": " + e.getMessage(), TypeEnum.ERROR);
                        LOGGER.error("Download failed", e);
                    }
                }
                LOGGER.info("Download loop completed");
            }
            catch(Exception e) {
                LOGGER.error("Download failed", e);                
            }
            alreadyQueued = false;
            providerInProgress = null;
        }
    }

}