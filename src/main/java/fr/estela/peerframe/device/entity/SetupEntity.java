package fr.estela.peerframe.device.entity;

import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="setup")
public class SetupEntity {
    
    public static final long ID_DEVICE_SETUP = 1;

    @Id
    private Long id;
    
    private UUID deviceId;
	
	private String deviceName;

    private String ownerId;
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public UUID getDeviceId() {
        return deviceId;
    }
    
    public void setDeviceId(UUID deviceId) {
        this.deviceId = deviceId;
    }
    
    public String getDeviceName() {
        return deviceName;
    }
    
    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }
    
    public String getOwnerId() {
        return ownerId;
    }
    
    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }
}