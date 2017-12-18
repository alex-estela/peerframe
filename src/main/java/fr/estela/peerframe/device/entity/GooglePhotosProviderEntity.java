package fr.estela.peerframe.device.entity;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import fr.estela.peerframe.device.service.AbstractDownloadManager;
import fr.estela.peerframe.device.service.GooglePhotosDownloadManager;

@Entity
@DiscriminatorValue("GooglePhotosProvider")
public class GooglePhotosProviderEntity extends ProviderEntity {

    private String projectId;
    private String clientId;
    private String clientSecret;
    private String authorizationCode;
    private String albumName;
    
    public String getProjectId() {
        return projectId;
    }
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }
    
    public String getClientId() {
        return clientId;
    }    
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }    
    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }
    
    public String getAuthorizationCode() {
        return authorizationCode;
    }    
    public void setAuthorizationCode(String authorizationCode) {
        this.authorizationCode = authorizationCode;
    }
    
    public String getAlbumName() {
        return albumName;
    }    
    public void setAlbumName(String albumName) {
        this.albumName = albumName;
    }
    
    public AbstractDownloadManager getDownloadManagerInstance() throws Exception {
        return new GooglePhotosDownloadManager();
    }
}