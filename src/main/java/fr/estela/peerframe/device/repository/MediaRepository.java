package fr.estela.peerframe.device.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import fr.estela.peerframe.device.entity.MediaEntity;

public interface MediaRepository extends JpaRepository<MediaEntity, UUID> {
	
	public MediaEntity findByRemoteId(String remoteId);
}