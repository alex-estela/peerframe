package fr.estela.peerframe.device.service;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.auth.oauth.OAuthHmacSigner;
import com.google.api.client.auth.oauth.OAuthParameters;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.javanet.NetHttpTransport;

import fr.estela.peerframe.device.entity.MediaEntity;
import fr.estela.peerframe.device.entity.ProviderEntity;
import fr.estela.peerframe.device.entity.SmugmugProviderEntity;
import fr.estela.peerframe.device.repository.MediaRepository;
import fr.estela.peerframe.device.repository.ProviderRepository;
import fr.estela.peerframe.device.util.ParsingUtil;

public class SmugmugDownloadManager extends AbstractDownloadManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(SmugmugDownloadManager.class);
    private static final int PAGE_SIZE = 10;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    @Override
    public void downloadAndSaveMedias(
        ProviderEntity providerEntity,
        ProviderRepository providerRepository,
        MediaRepository mediaRepository) throws Exception {

        LOGGER.debug("Smugmug download initiated...");

        SmugmugProviderEntity smugmugProviderEntity = (SmugmugProviderEntity) providerEntity;

        LOGGER.debug("Smugmug consumer key: {}", smugmugProviderEntity.getConsumerKey());
        LOGGER.debug("Smugmug consumer secret: {}", smugmugProviderEntity.getConsumerSecret());
        LOGGER.debug("Smugmug access token: {}", smugmugProviderEntity.getAccessToken());
        LOGGER.debug("Smugmug access token secret: {}", smugmugProviderEntity.getAccessTokenSecret());
        LOGGER.debug("Smugmug album: {}", smugmugProviderEntity.getAlbumId());

        if (smugmugProviderEntity.getConsumerKey() == null || smugmugProviderEntity.getConsumerSecret() == null
            || smugmugProviderEntity.getAccessToken() == null || smugmugProviderEntity.getAccessTokenSecret() == null
            || smugmugProviderEntity.getAlbumId() == null) {
            LOGGER.error("Invalid Smugmug provider configuration, exiting");
            return;
        }

        OAuthHmacSigner signer = new OAuthHmacSigner();
        signer.clientSharedSecret = smugmugProviderEntity.getConsumerSecret();
        signer.tokenSharedSecret = smugmugProviderEntity.getAccessTokenSecret();

        OAuthParameters oauthParameters = new OAuthParameters();
        oauthParameters.signer = signer;
        oauthParameters.consumerKey = smugmugProviderEntity.getConsumerKey();
        oauthParameters.token = smugmugProviderEntity.getAccessToken();

        HttpRequestFactory requestFactory = new NetHttpTransport().createRequestFactory(oauthParameters);

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept("application/json");

        int currentStartIndex = 1;
        int totalParsedCount = 0;
        int totalIgnoredCount = 0;
        int totalSavedCount = 0;
        Integer totalExpected = null;

        while (true) {

            LOGGER.debug("> Current Smugmug startIndex: {}", currentStartIndex);

            GenericUrl downloadUrl = new GenericUrl(
                String.format("https://api.smugmug.com/api/v2/album/%s!images?start=%s&count=%s",
                    smugmugProviderEntity.getAlbumId(), currentStartIndex, PAGE_SIZE));
            HttpRequest downloadRequest = requestFactory.buildGetRequest(downloadUrl);
            downloadRequest.setHeaders(headers);
            LOGGER.debug("> Smugmug request: {}", downloadUrl.toString());
            HttpResponse downloadResponse = downloadRequest.execute();
            LOGGER.debug("> Smugmug response code: {}", downloadResponse.getStatusCode());

            String json = downloadResponse.parseAsString();
            downloadResponse.getContent().close();
            ParsingUtil.printPrettyJson(LOGGER, json);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(json);

            JsonNode responseNode = rootNode.path("Response");
            if (!ParsingUtil.jsonNodeExists(responseNode)) {
                LOGGER.error("Download error: missing Response node");
                break;
            }

            JsonNode albumImageNode = responseNode.path("AlbumImage");
            if (!ParsingUtil.jsonNodeExists(albumImageNode)) {
                LOGGER.debug("Ending Smugmug downloads at startIndex {}", currentStartIndex);
                break;
            }

            // only parse total expected once
            if (totalExpected == null) {
                JsonNode pagesNode = responseNode.path("Pages");
                if (ParsingUtil.jsonNodeExists(pagesNode)) {
                    JsonNode totalNode = pagesNode.path("Total");
                    if (ParsingUtil.jsonNodeExists(totalNode)) {
                        totalExpected = totalNode.asInt();
                        LOGGER.debug("Total expected: {}", totalExpected);
                    }
                }
            }

            for (Iterator<JsonNode> i1 = albumImageNode.elements(); i1.hasNext();) {

                totalParsedCount++;

                JsonNode fileNode = i1.next();

                String remoteId = fileNode.get("ImageKey").textValue();                
                Date lastUpdated = DATE_FORMAT.parse(fileNode.get("LastUpdated").textValue());
                String type = fileNode.get("Format").textValue().trim().toLowerCase();
                int remoteWidth = fileNode.get("OriginalWidth").intValue();
                int remoteHeight = fileNode.get("OriginalHeight").intValue();

                MediaEntity mediaEntity = mediaRepository.findByRemoteId(remoteId);
                if (mediaEntity == null) mediaEntity = new MediaEntity();

                if (mediaEntity.getLastUpdated() == null
                    || !DATE_FORMAT.format(mediaEntity.getLastUpdated()).equals(DATE_FORMAT.format(lastUpdated))) {

                    String metaUrlPath = fileNode.get("Uris").get("ImageMetadata").get("Uri").textValue();
                    GenericUrl metaUrl = new GenericUrl("https://api.smugmug.com" + metaUrlPath);
                    HttpRequest metaRequest = requestFactory.buildGetRequest(metaUrl);
                    metaRequest.setHeaders(headers);
                    HttpResponse metaResponse = metaRequest.execute();
                    String metaJson = metaResponse.parseAsString();
                    metaResponse.getContent().close();
                    ParsingUtil.printPrettyJson(LOGGER, metaJson);

                    ObjectMapper metaMapper = new ObjectMapper();
                    JsonNode metaNode = metaMapper.readTree(metaJson);

                    JsonNode responseMetaNode = metaNode.get("Response");
                    JsonNode imageMetaNode = responseMetaNode.get("ImageMetadata");
                    JsonNode createdDateNode = imageMetaNode.get("DateCreated");
                    if (createdDateNode == null || createdDateNode.textValue().trim().equals(""))
                        createdDateNode = imageMetaNode.get("DateTimeCreated");
                    if (createdDateNode == null || createdDateNode.textValue().trim().equals(""))
                        createdDateNode = imageMetaNode.get("DateDigitized");
                    LOGGER.debug("Found createdDateNode: {}", createdDateNode.textValue());
                    Date originallyCreated = DATE_FORMAT.parse(createdDateNode.textValue().substring(0, 19));

                    GenericUrl contentUrl = new GenericUrl(fileNode.get("ArchivedUri").textValue());
                    HttpRequest contentRequest = requestFactory.buildGetRequest(contentUrl);
                    HttpResponse contentResponse = contentRequest.execute();
                    InputStream contentStream = contentResponse.getContent();

                    LOGGER.debug("> downloaded Smugmug {}", remoteId);
                    
                    try {
                        double latitude = imageMetaNode.get("Latitude").doubleValue();
                        double longitude = imageMetaNode.get("Longitude").doubleValue();
                        if (!(latitude == 0 && longitude == 0)) {
                            mediaEntity.setLocationLatitude(latitude);
                            mediaEntity.setLocationLongitude(longitude);
                        }
                    }
                    catch(Exception e) {
                        LOGGER.error(e.getMessage(), e);
                    }

                    resizeAndSaveMedia(LOGGER, mediaEntity, smugmugProviderEntity, mediaRepository, remoteId,
                        remoteWidth, remoteHeight, originallyCreated, lastUpdated, getMediaTypeFromJsonValue(LOGGER, type),
                        contentStream);

                    LOGGER.info("Saved remote {} to local {} ({}/{})", mediaEntity.getRemoteId(), mediaEntity.getId(), totalParsedCount, totalExpected);
                    totalSavedCount++;
                }
                else {
                    LOGGER.info("Ignoring remote {} due to local {} ({}/{})", mediaEntity.getRemoteId(),
                        mediaEntity.getId(), totalParsedCount, totalExpected);
                    totalIgnoredCount++;
                }
            }

            currentStartIndex += PAGE_SIZE;
        }

        LOGGER.info("In total, saved {} and ignored {} over {}", totalSavedCount, totalIgnoredCount, totalParsedCount);
    }
}