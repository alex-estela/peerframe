package fr.estela.peerframe.device.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.estela.peerframe.api.model.DeviceSetup;
import fr.estela.peerframe.api.model.Event;
import fr.estela.peerframe.device.entity.SetupEntity;
import fr.estela.peerframe.device.repository.SetupRepository;
import fr.estela.peerframe.device.service.DownloadService;
import fr.estela.peerframe.device.util.EventCache;

@Controller
@RequestMapping(value = "/api")
public class ToolController extends AbstractController {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ToolController.class);

    @Autowired
    private SetupRepository setupRepository;

    @Autowired
    private DownloadService downloadService;

    @Autowired
    private EventCache eventCache;
    
    @RequestMapping(value = "/tools/ping", method = RequestMethod.GET)
    public ResponseEntity<String> toolsPingGet() {
        return populateRetrievedResponse("1");
    }
    
    @RequestMapping(value = "/tools/deviceSetup", method = RequestMethod.GET)
    public ResponseEntity<DeviceSetup> toolsDeviceSetupGet() throws Exception {

        SetupEntity setupEntity = setupRepository.findOne(SetupEntity.ID_DEVICE_SETUP);
        DeviceSetup deviceSetup = new DeviceSetup();
        deviceSetup.setDeviceId(setupEntity.getDeviceId().toString());
        deviceSetup.setDeviceName(setupEntity.getDeviceName());
        deviceSetup.setOwnerId(setupEntity.getOwnerId()); 
        deviceSetup.setProviderInProgress(downloadService.getProviderInProgress());
        return populateRetrievedResponse(deviceSetup);
    }

    @RequestMapping(value = "/tools/deviceSetup", method = RequestMethod.PUT)
    public ResponseEntity<DeviceSetup> toolsDeviceSetupPut(
        @RequestBody DeviceSetup deviceSetup) throws Exception {

        SetupEntity setupEntity = setupRepository.findOne(SetupEntity.ID_DEVICE_SETUP);
        if (deviceSetup.getDeviceName() != null) setupEntity.setDeviceName(deviceSetup.getDeviceName());
        if (deviceSetup.getOwnerId() != null) setupEntity.setOwnerId(deviceSetup.getOwnerId());
        setupRepository.save(setupEntity);
        LOGGER.info("Updated device setup");

        deviceSetup.setDeviceId(setupEntity.getDeviceId().toString());
        deviceSetup.setDeviceName(setupEntity.getDeviceName());
        deviceSetup.setOwnerId(setupEntity.getOwnerId()); 
        deviceSetup.setProviderInProgress(downloadService.getProviderInProgress());       
        return populateRetrievedResponse(deviceSetup);
    }
    
    @RequestMapping(value = "/tools/events", method = RequestMethod.GET)
    public ResponseEntity<List<Event>> toolsEventsGet() throws Exception {

        return populateRetrievedResponse(eventCache.getEvents());
    }

}