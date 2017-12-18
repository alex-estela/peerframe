package fr.estela.peerframe.device.entity;

import java.util.UUID;

import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

import fr.estela.peerframe.device.service.AbstractDownloadManager;

@Entity
@Table(name="provider")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name="type", discriminatorType=DiscriminatorType.STRING)
public class ProviderEntity {

    @Id
    @Type(type="pg-uuid")
    private UUID id;
	
	private String name;
    private String lastDownloadInfo;
	
	public UUID getId() {
		return id;
	}
	public void setId(UUID id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	} 
    public String getLastDownloadInfo() {
        return lastDownloadInfo;
    }    
    public void setLastDownloadInfo(String lastDownloadInfo) {
        this.lastDownloadInfo = lastDownloadInfo;
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + " / " + id.toString() + " (" + name + ")";
    }
    
    public AbstractDownloadManager getDownloadManagerInstance() throws Exception {
        throw new Exception("Download Manager not defined");
    }
}