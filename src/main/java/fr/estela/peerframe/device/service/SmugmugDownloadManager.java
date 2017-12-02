package fr.estela.peerframe.device.service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.UUID;

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

import fr.estela.peerframe.api.model.Media.MediaTypeEnum;
import fr.estela.peerframe.device.Application;
import fr.estela.peerframe.device.entity.MediaContentEntity;
import fr.estela.peerframe.device.entity.MediaContentStreamEntity;
import fr.estela.peerframe.device.entity.MediaEntity;
import fr.estela.peerframe.device.entity.ProviderEntity;
import fr.estela.peerframe.device.entity.SmugmugProviderEntity;
import fr.estela.peerframe.device.repository.MediaRepository;
import fr.estela.peerframe.device.repository.ProviderRepository;
import fr.estela.peerframe.device.util.ParsingUtil;
import fr.estela.peerframe.device.util.StreamGobbler;

public class SmugmugDownloadManager implements DownloadManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(SmugmugDownloadManager.class);
    private static final int PAGE_SIZE = 10;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    
    private MediaTypeEnum getMediaTypeFromJsonValue(String value) {
        if (value.equals("jpg") || value.equals("jpeg")) return MediaTypeEnum.JPG;
        LOGGER.warn("Unknown media content type: " + value);
        return null;
    }
    
    @Override
    public void downloadAndSaveMedias(
        ProviderEntity providerEntity,
        ProviderRepository providerRepository,
        MediaRepository mediaRepository) throws Exception {
        
        LOGGER.debug("Smugmug download initiated...");

        SmugmugProviderEntity smugmugProviderEntity = (SmugmugProviderEntity) providerEntity;

        LOGGER.debug("Smugmug consumer key: " + smugmugProviderEntity.getConsumerKey());
        LOGGER.debug("Smugmug consumer secret: " + smugmugProviderEntity.getConsumerSecret());
        LOGGER.debug("Smugmug access token: " + smugmugProviderEntity.getAccessToken());
        LOGGER.debug("Smugmug access token secret: " + smugmugProviderEntity.getAccessTokenSecret());
        LOGGER.debug("Smugmug album: " + smugmugProviderEntity.getAlbumId());
        
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

        int currentPageIndex = 1;
        int totalParsedCount = 0;
        int totalIgnoredCount = 0;
        int totalSavedCount = 0;
        
        while (true) {
            
            LOGGER.debug("> Current Smugmug pageIndex: " + currentPageIndex);

            GenericUrl downloadUrl = new GenericUrl(
                "https://api.smugmug.com/api/v2/album/" + smugmugProviderEntity.getAlbumId() + "!images?start=" + currentPageIndex + "&count=" + PAGE_SIZE);
            HttpRequest downloadRequest = requestFactory.buildGetRequest(downloadUrl);
            downloadRequest.setHeaders(headers);
            HttpResponse downloadResponse = downloadRequest.execute();
            String json = downloadResponse.parseAsString();
            downloadResponse.getContent().close();
            ParsingUtil.printPrettyJson(LOGGER, json);

            LOGGER.debug("> Smugmug response received");

            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(json);

            JsonNode responseNode = rootNode.path("Response");
            if (!ParsingUtil.jsonNodeExists(responseNode)) {
                LOGGER.error("Download error: missing Response node");
                break;
            }

            JsonNode albumImageNode = responseNode.path("AlbumImage");
            if (!ParsingUtil.jsonNodeExists(albumImageNode)) {
                LOGGER.debug("Ending Smugmug downloads at pageIndex " + currentPageIndex);
                break;
            }
            
            totalParsedCount++;

            for (Iterator<JsonNode> i1 = albumImageNode.elements(); i1.hasNext();) {

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
                    LOGGER.debug("Found createdDateNode: " + createdDateNode.textValue());
                    Date originallyCreated = DATE_FORMAT.parse(createdDateNode.textValue().substring(0, 19));

                    GenericUrl contentUrl = new GenericUrl(fileNode.get("ArchivedUri").textValue());
                    HttpRequest contentRequest = requestFactory.buildGetRequest(contentUrl);
                    HttpResponse contentResponse = contentRequest.execute();
                    InputStream contentStream = contentResponse.getContent();

                    UUID mediaId = UUID.randomUUID();
                    String originalPath = Application.getTempFolder() + mediaId + "-original";
                    String convertedPath = Application.getTempFolder() + mediaId + "-converted";
                    Path originalPathObj = Paths.get(originalPath);
                    Path convertedPathObj = Paths.get(convertedPath);

                    LOGGER.debug("> writing to " + originalPath);
                    Files.copy(contentStream, originalPathObj);
                    contentStream.close();

                    String cmd = "convert-peerframe -resize x800 " + originalPath + " " + convertedPath;
                    LOGGER.debug("> converting to " + convertedPath);
                    LOGGER.debug(cmd);

                    Process process = Runtime.getRuntime().exec(cmd);
                    StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream(), LOGGER, "CONVERT-ERROR");
                    errorGobbler.start();
                    StreamGobbler inputGobbler = new StreamGobbler(process.getInputStream(), LOGGER, "CONVERT-INPUT");
                    inputGobbler.start();
                    errorGobbler.join();
                    inputGobbler.join();
                    process.getOutputStream().close();
                    process.waitFor();
                    process.destroy();

                    LOGGER.debug("> reading from " + convertedPath);

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
                    mediaEntity.setMediaType(getMediaTypeFromJsonValue(type));
                    mediaEntity.setOriginallyCreated(originallyCreated);
                    mediaEntity.setLastUpdated(lastUpdated);

                    LOGGER.debug("> downloaded Smugmug " + mediaEntity.getMediaType() + " " + mediaEntity.getRemoteId());

                    mediaEntity.setId(mediaId);
                    mediaEntity.setProvider(providerEntity);

                    mediaRepository.save(mediaEntity);

                    LOGGER.info("Saved remote " + mediaEntity.getRemoteId() + " to local " + mediaEntity.getId());
                    totalSavedCount++;
                }
                else {
                    LOGGER.info("Ignoring remote " + mediaEntity.getRemoteId() + " due to local " + mediaEntity.getId());
                    totalIgnoredCount++;
                }
            }   
            
            currentPageIndex++;
        }

        LOGGER.info("In total, saved " + totalSavedCount + " and ignored " + totalIgnoredCount + " over " + totalParsedCount);
    }
}