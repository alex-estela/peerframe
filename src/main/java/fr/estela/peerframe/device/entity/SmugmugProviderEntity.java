package fr.estela.peerframe.device.entity;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("SmugMugProvider")
public class SmugmugProviderEntity extends ProviderEntity {

    private String albumId;
    private String consumerKey;
    private String consumerSecret;
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
}