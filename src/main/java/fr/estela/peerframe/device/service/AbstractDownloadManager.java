package fr.estela.peerframe.device.service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.UUID;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.estela.peerframe.api.model.Media.MediaTypeEnum;
import fr.estela.peerframe.device.Application;
import fr.estela.peerframe.device.entity.MediaContentEntity;
import fr.estela.peerframe.device.entity.MediaContentStreamEntity;
import fr.estela.peerframe.device.entity.MediaEntity;
import fr.estela.peerframe.device.entity.ProviderEntity;
import fr.estela.peerframe.device.repository.MediaRepository;
import fr.estela.peerframe.device.repository.ProviderRepository;
import fr.estela.peerframe.device.util.ParsingUtil;
import fr.estela.peerframe.device.util.StreamGobbler;

public abstract class AbstractDownloadManager {

    private static final String OPENSTREEMATP_QUERY = "http://nominatim.openstreetmap.org/reverse?format=json&lat=%s&lon=%s&zoom=18&addressdetails=1";
    
    public abstract void downloadAndSaveMedias(ProviderEntity providerEntity, ProviderRepository providerRepository, MediaRepository mediaRepository) throws Exception; 

    private class Location {

        private String city;
        private String country;

        public String getCity() {
            return city;
        }

        public String getCountry() {
            return country;
        }
    }

    private Location getLocationFromCoordinates(Logger logger, double latitude, double longitude) {

        try {
            
            String url = String.format(OPENSTREEMATP_QUERY, latitude, longitude);
            HttpClient client = HttpClientBuilder.create().build();
            HttpGet request = new HttpGet(url);
            request.setHeader("Accept-Language", "fr-FR,fr;q=0.9,en-US;q=0.8,en;q=0.7"); // required to get full city names         
            HttpResponse response = client.execute(request);
            String json = EntityUtils.toString(response.getEntity());
            
            ParsingUtil.printPrettyJson(logger, json);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(json);

            JsonNode addressNode = rootNode.path("address");
            if (!ParsingUtil.jsonNodeExists(addressNode)) {
                logger.error("OpenStreetMap error: missing address node");
                return null;
            }

            JsonNode cityNode = addressNode.path("city");
            JsonNode townNode = addressNode.path("town");
            JsonNode villageNode = addressNode.path("village");
            JsonNode countryNode = addressNode.path("country");
            
            Location location = null;
            if (ParsingUtil.jsonNodeExists(cityNode) || ParsingUtil.jsonNodeExists(townNode) 
                || ParsingUtil.jsonNodeExists(villageNode) || ParsingUtil.jsonNodeExists(countryNode)) {
                location = new Location();
                location.city = ParsingUtil.jsonNodeExists(cityNode) ? cityNode.asText() 
                    : (ParsingUtil.jsonNodeExists(townNode) ? townNode.asText() : 
                        (ParsingUtil.jsonNodeExists(villageNode) ? villageNode.asText() : null));
                if (location.city.contains(",")) location.city = location.city.substring(0, location.city.indexOf(",")).trim();
                location.country = ParsingUtil.jsonNodeExists(countryNode) ? countryNode.asText() : null;
                logger.debug("> City: {}", location.city);
                logger.debug("> Country: {}", location.country);
            }
            return location;
        }
        catch (Exception e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }
   
    protected MediaTypeEnum getMediaTypeFromJsonValue(Logger logger, String value) {
        if (value.contains("jpg") || value.contains("jpeg")) return MediaTypeEnum.JPG;
        if (value.contains("png")) return MediaTypeEnum.PNG;
        logger.warn("Unknown media content type: {}", value);
        return null;
    }
    
    protected void resizeAndSaveMedia(
        Logger logger,
        MediaEntity mediaEntity,
        ProviderEntity providerEntity,
        MediaRepository mediaRepository,
        String remoteId,
        int remoteWidth,
        int remoteHeight,
        Date originallyCreated,
        Date lastUpdated,
        MediaTypeEnum mediaType,
        InputStream contentStream) throws Exception {
        
        UUID mediaId = UUID.randomUUID();
        
        String originalPath = Application.getTempFolder() + mediaId + "-original";
        String convertedPath = Application.getTempFolder() + mediaId + "-converted";
        Path originalPathObj = Paths.get(originalPath);
        Path convertedPathObj = Paths.get(convertedPath);

        logger.debug("> writing to {}", originalPath);
        Files.copy(contentStream, originalPathObj);
        contentStream.close();

        String cmd = "convert-peerframe -auto-orient -resize x800 " + originalPath + " " + convertedPath;
        logger.debug("> converting to {}", convertedPath);
        logger.debug("> running: {}", cmd);

        Process process = Runtime.getRuntime().exec(cmd);
        StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream(), logger, "CONVERT-ERROR");
        errorGobbler.start();
        StreamGobbler inputGobbler = new StreamGobbler(process.getInputStream(), logger, "CONVERT-INPUT");
        inputGobbler.start();
        errorGobbler.join();
        inputGobbler.join();
        process.getOutputStream().close();
        process.waitFor();
        process.destroy();

        logger.debug("> reading from {}", convertedPath);

        byte[] localBytes = Files.readAllBytes(convertedPathObj);

        MediaContentStreamEntity localMediaContentStreamEntity = new MediaContentStreamEntity();
        localMediaContentStreamEntity.setBytes(localBytes);

        MediaContentEntity localMediaContentEntity = new MediaContentEntity();
        localMediaContentEntity.setContentStream(localMediaContentStreamEntity);
        localMediaContentEntity.setWidth(remoteWidth); // TODO calculate new local width
        localMediaContentEntity.setHeight(remoteHeight); // TODO calculate new local height
        mediaEntity.setLocalContent(localMediaContentEntity);

        MediaContentEntity remoteMediaContentEntity = new MediaContentEntity();
        remoteMediaContentEntity.setWidth(remoteWidth);
        remoteMediaContentEntity.setHeight(remoteHeight);
        mediaEntity.setRemoteContent(remoteMediaContentEntity);

        Files.delete(Paths.get(originalPath));
        Files.delete(Paths.get(convertedPath));

        if (mediaEntity.getRemoteId() == null) mediaEntity.setRemoteId(remoteId);
        mediaEntity.setMediaType(mediaType);
        mediaEntity.setOriginallyCreated(originallyCreated);
        mediaEntity.setLastUpdated(lastUpdated);
        mediaEntity.setId(mediaId);
        mediaEntity.setProvider(providerEntity);

        // try to identify location
        if (mediaEntity.getLocationLatitude() != null && mediaEntity.getLocationLongitude() != null) {
            Location location = getLocationFromCoordinates(logger, mediaEntity.getLocationLatitude(), mediaEntity.getLocationLongitude());
            if (location != null) {
                mediaEntity.setLocationCity(location.getCity());
                mediaEntity.setLocationCountry(location.getCountry());
            }
        }
        
        mediaRepository.save(mediaEntity);
    }
}
