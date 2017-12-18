package fr.estela.peerframe.device.entity;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import fr.estela.peerframe.device.service.AbstractDownloadManager;
import fr.estela.peerframe.device.service.SmugmugDownloadManager;

@Entity
@DiscriminatorValue("SmugmugProvider")
public class SmugmugProviderEntity extends ProviderEntity {

    private String albumId;
    @Column(name="clientId") private String consumerKey;
    @Column(name="clientSecret") private String consumerSecret;
    private String accessToken;
    private String accessTokenSecret;

    public String getAlbumId() {
        return albumId;
    }
    public void setAlbumId(String albumId) {
        this.albumId = albumId;
    }
    
    public String getConsumerKey() {
        return consumerKey;
    }    
    public void setConsumerKey(String consumerKey) {
        this.consumerKey = consumerKey;
    }
    
    public String getConsumerSecret() {
        return consumerSecret;
    }    
    public void setConsumerSecret(String consumerSecret) {
        this.consumerSecret = consumerSecret;
    }
    
    public String getAccessToken() {
        return accessToken;
    }    
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
    
    public String getAccessTokenSecret() {
        return accessTokenSecret;
    }    
    public void setAccessTokenSecret(String accessTokenSecret) {
        this.accessTokenSecret = accessTokenSecret;
    }
    
    public AbstractDownloadManager getDownloadManagerInstance() throws Exception {
        return new SmugmugDownloadManager();
    }
}