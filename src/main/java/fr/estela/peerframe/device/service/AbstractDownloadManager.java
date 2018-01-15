package fr.estela.peerframe.device.service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.UUID;

import org.slf4j.Logger;

import fr.estela.peerframe.api.model.Media.MediaTypeEnum;
import fr.estela.peerframe.device.Application;
import fr.estela.peerframe.device.entity.MediaContentEntity;
import fr.estela.peerframe.device.entity.MediaContentStreamEntity;
import fr.estela.peerframe.device.entity.MediaEntity;
import fr.estela.peerframe.device.entity.ProviderEntity;
import fr.estela.peerframe.device.repository.MediaRepository;
import fr.estela.peerframe.device.repository.ProviderRepository;
import fr.estela.peerframe.device.util.StreamGobbler;

public abstract class AbstractDownloadManager {

    public abstract void downloadAndSaveMedias(ProviderEntity providerEntity, ProviderRepository providerRepository, MediaRepository mediaRepository) throws Exception; 

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

        mediaRepository.save(mediaEntity);
    }
}
