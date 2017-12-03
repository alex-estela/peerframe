package fr.estela.peerframe.device.service;

import java.net.InetAddress;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fr.estela.peerframe.device.entity.SetupEntity;
import fr.estela.peerframe.device.repository.SetupRepository;

@Component
public class SetupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SetupService.class);
    
    @Autowired
    private SetupRepository setupRepository;
    
    @PostConstruct
    public void initDeviceSetup() {
        try {
            LOGGER.info("Initializing device setup");
            SetupEntity deviceSetupEntity = setupRepository.findOne(SetupEntity.ID_DEVICE_SETUP);
            if (deviceSetupEntity == null) {
                LOGGER.info("Device setup does not exist yet, creating it");
                deviceSetupEntity = new SetupEntity();
                deviceSetupEntity.setId(SetupEntity.ID_DEVICE_SETUP);
                deviceSetupEntity.setDeviceId(UUID.randomUUID());
                deviceSetupEntity.setDeviceName(InetAddress.getLocalHost().toString());
                setupRepository.save(deviceSetupEntity);
            }
            else LOGGER.info("Device setup already exists");
            LOGGER.info("Current device has id: {} / name: {} / owner: {}", 
                deviceSetupEntity.getDeviceId(), deviceSetupEntity.getDeviceName(), deviceSetupEntity.getOwnerId());
        }
        catch(Exception e) {
            LOGGER.error("Could not initialize device setup", e);
        }
    }
}
