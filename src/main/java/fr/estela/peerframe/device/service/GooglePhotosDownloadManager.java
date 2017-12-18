package fr.estela.peerframe.device.service;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpExecuteInterceptor;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.gdata.client.photos.PicasawebService;
import com.google.gdata.data.photos.AlbumFeed;
import com.google.gdata.data.photos.GphotoEntry;
import com.google.gdata.data.photos.PhotoFeed;
import com.google.gdata.data.photos.UserFeed;

import fr.estela.peerframe.device.Application;
import fr.estela.peerframe.device.entity.GooglePhotosProviderEntity;
import fr.estela.peerframe.device.entity.MediaEntity;
import fr.estela.peerframe.device.entity.ProviderEntity;
import fr.estela.peerframe.device.repository.MediaRepository;
import fr.estela.peerframe.device.repository.ProviderRepository;

public class GooglePhotosDownloadManager extends AbstractDownloadManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(GooglePhotosDownloadManager.class);
    private static final int PAGE_SIZE = 10;

    private static final Credential.AccessMethod AUTH_ACCESSMETHOD = BearerToken.authorizationHeaderAccessMethod();
    private static final NetHttpTransport AUTH_TRANSPORT = new NetHttpTransport();
    private static final JacksonFactory AUTH_JSONFACTORY = JacksonFactory.getDefaultInstance();
    private static final String AUTH_TOKENSERVERURL = "https://www.googleapis.com/oauth2/v4/token?access_type=offline&prompt=consent&client_id=%s&client_secret=%s&redirect_uri=urn:ietf:wg:oauth:2.0:oob";;
    private static final String AUTH_AUTHSERVERURL = "https://accounts.google.com/o/oauth2/v2/auth?access_type=offline&prompt=consent&scope=http%3A%2F%2Fphotos.googleapis.com%2Fdata%2F&redirect_uri=urn:ietf:wg:oauth:2.0:oob&response_type=code&client_id=";
    private static final HttpExecuteInterceptor AUTH_INTERCEPTOR = httpRequest -> {};
    private static final String AUTH_SCOPE = "http://picasaweb.google.com/data/";
    private static final File DATA_FOLDER = new File(Application.getDataFolder() + "/GooglePhotos");
    private static final String AUTHORIZATIONCODE_TAMPERED = "<tampered>";
    private static final String URL_ALBUMS = "http://photos.googleapis.com/data/feed/api/user/default?kind=album";
    private static final String URL_PHOTOS_PAGED = "http://photos.googleapis.com/data/feed/api/user/default/albumid/%s?kind=photo&start-index=%s&max-results=%s";
    private static final String URL_PHOTO_SUFFIX = "?imgmax=d";
    private static final HttpRequestFactory REQUESTFACTORY = AUTH_TRANSPORT.createRequestFactory();

    private void refreshTokenIfNecessary(Credential credential) throws Exception {
        if (credential.getExpiresInSeconds() <= 0) {
            if (LOGGER.isDebugEnabled()) LOGGER.debug("Refreshing token");
            credential.refreshToken();
        }
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void downloadAndSaveMedias(
        ProviderEntity providerEntity,
        ProviderRepository providerRepository,
        MediaRepository mediaRepository) throws Exception {

        LOGGER.debug("Google Photos download initiated...");

        GooglePhotosProviderEntity googlePhotosProviderEntity = (GooglePhotosProviderEntity) providerEntity;

        LOGGER.debug("GooglePhotos project id: {}", googlePhotosProviderEntity.getProjectId());
        LOGGER.debug("GooglePhotos client id: {}", googlePhotosProviderEntity.getClientId());
        LOGGER.debug("GooglePhotos client secret: {}", googlePhotosProviderEntity.getClientSecret());
        LOGGER.debug("GooglePhotos authorization code: {}", googlePhotosProviderEntity.getAuthorizationCode());
        LOGGER.debug("GooglePhotos album: {}", googlePhotosProviderEntity.getAlbumName());

        if (googlePhotosProviderEntity.getProjectId() == null || googlePhotosProviderEntity.getClientId() == null
            || googlePhotosProviderEntity.getClientSecret() == null
            || googlePhotosProviderEntity.getAuthorizationCode() == null
            || googlePhotosProviderEntity.getAlbumName() == null) {
            LOGGER.error("Invalid GooglePhotos provider configuration, exiting");
            return;
        }

        FileDataStoreFactory dataStoreFactory = new FileDataStoreFactory(DATA_FOLDER);

        AuthorizationCodeFlow.Builder builder = new AuthorizationCodeFlow.Builder(
            AUTH_ACCESSMETHOD, 
            AUTH_TRANSPORT,
            AUTH_JSONFACTORY,
            new GenericUrl(String.format(AUTH_TOKENSERVERURL, googlePhotosProviderEntity.getClientId(), googlePhotosProviderEntity.getClientSecret())),
            AUTH_INTERCEPTOR, 
            googlePhotosProviderEntity.getClientId(),
            AUTH_AUTHSERVERURL + googlePhotosProviderEntity.getClientId())
                .setDataStoreFactory(dataStoreFactory)
                .setScopes(Arrays.asList(AUTH_SCOPE));

        AuthorizationCodeFlow flow = builder.build();

        Credential credential = null;
        if (googlePhotosProviderEntity.getAuthorizationCode().equals(AUTHORIZATIONCODE_TAMPERED)) {
            credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize(googlePhotosProviderEntity.getClientId());
            LOGGER.debug("Loading existing credentials, expiration time is: {}", credential.getExpiresInSeconds());
        }
        else {
            TokenResponse response = flow.newTokenRequest(googlePhotosProviderEntity.getAuthorizationCode()).execute();
            LOGGER.debug("Generating new credentials with code: {}", googlePhotosProviderEntity.getAuthorizationCode());
            credential = flow.createAndStoreCredential(response, googlePhotosProviderEntity.getClientId());
            googlePhotosProviderEntity.setAuthorizationCode(AUTHORIZATIONCODE_TAMPERED);
            providerRepository.save(googlePhotosProviderEntity);
            if (LOGGER.isDebugEnabled()) LOGGER.debug("Updated provider entity with tampered code");
        }

        PicasawebService service = new PicasawebService(googlePhotosProviderEntity.getProjectId());
        service.setOAuth2Credentials(credential);

        refreshTokenIfNecessary(credential);
        UserFeed userFeed = service.getFeed(new URL(URL_ALBUMS), UserFeed.class);
        String albumId = null;

        LOGGER.debug("Found {} albums");
        for (GphotoEntry entry : userFeed.getEntries()) {
            String name = entry.getTitle().getPlainText();
            LOGGER.debug("> Album: {}", name);
            if (name.equals(googlePhotosProviderEntity.getAlbumName())) {
                albumId = entry.getGphotoId();
                LOGGER.debug("> Found target album path: {}", albumId);
                break;
            }
        }

        int currentStartIndex = 1;
        int totalParsedCount = 0;
        int totalIgnoredCount = 0;
        int totalSavedCount = 0;

        while (true) {

            refreshTokenIfNecessary(credential);
            AlbumFeed albumFeed = service.getFeed(new URL(String.format(URL_PHOTOS_PAGED, albumId, currentStartIndex, PAGE_SIZE)), AlbumFeed.class);
            LOGGER.debug("Fetched photos: {}", albumFeed.getEntries().size());

            for (GphotoEntry photo : albumFeed.getEntries()) {

                LOGGER.debug("GET photo: {}", photo.getFeedLink().getHref());
                refreshTokenIfNecessary(credential);
                PhotoFeed photoFeed = service.getFeed(new URL(photo.getFeedLink().getHref() + URL_PHOTO_SUFFIX), PhotoFeed.class);

                String remoteId = photoFeed.getGphotoId();
                MediaEntity mediaEntity = mediaRepository.findByRemoteId(remoteId);
                
                if (mediaEntity == null) {
                    mediaEntity = new MediaEntity();

                    int remoteWidth = Integer.valueOf(photoFeed.getWidth().toString());
                    int remoteHeight = Integer.valueOf(photoFeed.getHeight().toString());
                    Date originallyCreated = photoFeed.getTimestamp();
                    String photoURL = photoFeed.getMediaContents().get(0).getUrl();                    

                    LOGGER.debug("Downloading {}", photoURL);
                    HttpRequest contentRequest = REQUESTFACTORY.buildGetRequest(new GenericUrl(photoURL));
                    HttpResponse contentResponse = contentRequest.execute();
                    InputStream contentStream = contentResponse.getContent();
                    String type = contentResponse.getContentType();
                    
                    resizeAndSaveMedia(LOGGER, mediaEntity, googlePhotosProviderEntity, mediaRepository, remoteId,
                        remoteWidth, remoteHeight, originallyCreated, new Date(), getMediaTypeFromJsonValue(LOGGER, type),
                        contentStream);

                    LOGGER.info("Saved remote {} to local {} ({}/?)", mediaEntity.getRemoteId(), mediaEntity.getId(), totalParsedCount);
                    totalSavedCount++;

                }
                else {
                    LOGGER.info("Ignoring remote {} due to local {} ({}/?)", mediaEntity.getRemoteId(), mediaEntity.getId(), totalParsedCount);
                    totalIgnoredCount++;
                }

                totalParsedCount++;
            }

            if (albumFeed.getEntries().size() < PAGE_SIZE) break;
            else currentStartIndex += PAGE_SIZE;
        }

        LOGGER.info("In total, saved {} and ignored {} over {}", totalSavedCount, totalIgnoredCount, totalParsedCount);
    }

}