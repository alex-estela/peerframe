package fr.estela.peerframe.device.controller;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import fr.estela.peerframe.api.model.DeviceSetup;
import fr.estela.peerframe.api.model.Event;
import fr.estela.peerframe.device.Application;
import fr.estela.peerframe.device.entity.SetupEntity;
import fr.estela.peerframe.device.repository.SetupRepository;
import fr.estela.peerframe.device.service.DownloadService;
import fr.estela.peerframe.device.util.EventCache;
import fr.estela.peerframe.device.util.StreamGobbler;

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
        deviceSetup.setApplicationVersion(Application.getVersion());
        
        WifiSettings wifi = getWifiSettings();
        deviceSetup.setWifiSSID(wifi.ssid);
        deviceSetup.setWifiKey(wifi.key);
        
        return populateRetrievedResponse(deviceSetup);
    }

    @RequestMapping(value = "/tools/deviceSetup", method = RequestMethod.PUT)
    public ResponseEntity<DeviceSetup> toolsDeviceSetupPut(
        @RequestBody DeviceSetup deviceSetup, @RequestParam(required = false) Boolean upgradeDeviceVersion) throws Exception {
        LOGGER.info("Device setup PUT operation, with upgradeVersion: " + upgradeDeviceVersion);
        
        if (upgradeDeviceVersion != null && upgradeDeviceVersion) {
            LOGGER.info("Upgrading device version, device should reboot...");
            try {
                Process process = Runtime.getRuntime().exec("upgradedevice");
                StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream(), LOGGER, "UPDATEWIFI-ERROR");
                errorGobbler.start();
                StreamGobbler inputGobbler = new StreamGobbler(process.getInputStream(), LOGGER, "UPDATEWIFI-INPUT");
                inputGobbler.start();
                errorGobbler.join();
                inputGobbler.join();
                process.getOutputStream().close();
                process.waitFor();
                process.destroy();
            }
            catch(Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
            return populateRetrievedResponse(deviceSetup);
        }
        
        SetupEntity setupEntity = setupRepository.findOne(SetupEntity.ID_DEVICE_SETUP);
        if (deviceSetup.getDeviceName() != null || deviceSetup.getOwnerId() != null) {
            LOGGER.info("Updating device setup...");        
            if (deviceSetup.getDeviceName() != null) setupEntity.setDeviceName(deviceSetup.getDeviceName());
            if (deviceSetup.getOwnerId() != null) setupEntity.setOwnerId(deviceSetup.getOwnerId());
            setupRepository.save(setupEntity);
            LOGGER.info("Updated device setup");
        }

        deviceSetup.setDeviceId(setupEntity.getDeviceId().toString());
        deviceSetup.setDeviceName(setupEntity.getDeviceName());
        deviceSetup.setOwnerId(setupEntity.getOwnerId()); 
        deviceSetup.setProviderInProgress(downloadService.getProviderInProgress());  
        deviceSetup.setApplicationVersion(Application.getVersion());     
        
        WifiSettings wifi = getWifiSettings();        
        if (deviceSetup.getWifiSSID() != null && !deviceSetup.getWifiSSID().trim().equals("")
            && deviceSetup.getWifiKey() != null && !deviceSetup.getWifiKey().trim().equals("")
            && (!deviceSetup.getWifiSSID().equals(wifi.ssid) || !deviceSetup.getWifiKey().equals(wifi.key))) {
            LOGGER.info("Updating wifi config...");
            try {
                Process process = Runtime.getRuntime().exec(String.format("updatewifi %s %s", deviceSetup.getWifiSSID(), deviceSetup.getWifiKey()));
                StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream(), LOGGER, "UPDATEWIFI-ERROR");
                errorGobbler.start();
                StreamGobbler inputGobbler = new StreamGobbler(process.getInputStream(), LOGGER, "UPDATEWIFI-INPUT");
                inputGobbler.start();
                errorGobbler.join();
                inputGobbler.join();
                process.getOutputStream().close();
                process.waitFor();
                process.destroy();
            }
            catch(Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
            wifi = getWifiSettings();
            LOGGER.info("Updated wifi config");
        }        
        deviceSetup.setWifiSSID(wifi.ssid);
        deviceSetup.setWifiKey(wifi.key);
        
        return populateRetrievedResponse(deviceSetup);
    }
    
    @RequestMapping(value = "/tools/events", method = RequestMethod.GET)
    public ResponseEntity<List<Event>> toolsEventsGet() throws Exception {

        return populateRetrievedResponse(eventCache.getEvents());
    }
    
    private WifiSettings getWifiSettings() throws Exception {
        List<String> lines = Files.readAllLines(Paths.get("/etc/wpa_supplicant/wpa_supplicant.conf"));
        WifiSettings settings = new WifiSettings();
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("ssid")) {
                settings.ssid = line.substring("ssid=".length()).replace("\"", "");
            }
            else if (line.startsWith("psk")) {
                settings.key = line.substring("psk=".length()).replace("\"", "");                
            }            
        }
        return settings;
    }
    
    private class WifiSettings {
        private String ssid;
        private String key;
    }
    
}