package fr.estela.peerframe.device.entity;

import java.util.Date;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

import fr.estela.peerframe.api.model.Media.MediaTypeEnum;

@Entity
@Table(name="media", indexes = {
	@Index(name="media_remoteId_index", columnList="remoteId", unique=true)
})
public class MediaEntity {

	@Id
	@Type(type="pg-uuid")
	private UUID id;
	
	@ManyToOne(fetch=FetchType.LAZY)
	private ProviderEntity provider;
	
	@Enumerated(EnumType.STRING)
	private MediaTypeEnum mediaType;
	
	private String remoteId;

	@ManyToOne(cascade=CascadeType.ALL, fetch=FetchType.LAZY)
	private MediaContentEntity remoteContent;
	
	@ManyToOne(cascade=CascadeType.ALL, fetch=FetchType.LAZY)
	private MediaContentEntity localContent;

	private Date originallyCreated;
	private Date lastUpdated;
	
	public UUID getId() {
		return id;
	}
	public void setId(UUID id) {
		this.id = id;
	}
	public ProviderEntity getProvider() {
		return provider;
	}
	public void setProvider(ProviderEntity provider) {
		this.provider = provider;
	}
	public MediaTypeEnum getMediaType() {
		return mediaType;
	}
	public void setMediaType(MediaTypeEnum mediaType) {
		this.mediaType = mediaType;
	}
	public String getRemoteId() {
		return remoteId;
	}
	public void setRemoteId(String remoteId) {
		this.remoteId = remoteId;
	}
	public Date getOriginallyCreated() {
		return originallyCreated;
	}
	public void setOriginallyCreated(Date originallyCreated) {
		this.originallyCreated = originallyCreated;
	}
	public Date getLastUpdated() {
		return lastUpdated;
	}
	public void setLastUpdated(Date lastUpdated) {
		this.lastUpdated = lastUpdated;
	}
	public MediaContentEntity getRemoteContent() {
		return remoteContent;
	}
	public void setRemoteContent(MediaContentEntity remoteContent) {
		this.remoteContent = remoteContent;
	}
	public MediaContentEntity getLocalContent() {
		return localContent;
	}
	public void setLocalContent(MediaContentEntity localContent) {
		this.localContent = localContent;
	}
}