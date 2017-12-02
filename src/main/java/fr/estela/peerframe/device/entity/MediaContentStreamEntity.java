package fr.estela.peerframe.device.entity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="mediacontentstream")
public class MediaContentStreamEntity {

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;

	private byte[] bytes;

	public byte[] getBytes() {
		return bytes;
	}
	public void setBytes(byte[] bytes) {
		this.bytes = bytes;
	}
}