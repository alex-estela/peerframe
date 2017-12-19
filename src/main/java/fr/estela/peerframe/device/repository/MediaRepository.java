package fr.estela.peerframe.device.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import fr.estela.peerframe.device.entity.MediaEntity;

public interface MediaRepository extends JpaRepository<MediaEntity, UUID> {
	
	public MediaEntity findByRemoteId(String remoteId);
    public List<MediaEntity> findByProviderId(UUID providerId);
}