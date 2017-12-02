package fr.estela.peerframe.device.entity;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name="mediacontent")
public class MediaContentEntity {

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;
	
	@OneToOne(cascade=CascadeType.ALL, fetch=FetchType.LAZY)
	private MediaContentStreamEntity contentStream;

	private int width;
	private int height;

	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public int getWidth() {
		return width;
	}
	public void setWidth(int width) {
		this.width = width;
	}
	public int getHeight() {
		return height;
	}
	public void setHeight(int height) {
		this.height = height;
	}
	public MediaContentStreamEntity getContentStream() {
		return contentStream;
	}
	public void setContentStream(MediaContentStreamEntity contentStream) {
		this.contentStream = contentStream;
	}
}