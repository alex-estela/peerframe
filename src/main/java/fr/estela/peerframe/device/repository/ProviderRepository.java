package fr.estela.peerframe.device.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import fr.estela.peerframe.device.entity.ProviderEntity;

public interface ProviderRepository extends JpaRepository<ProviderEntity, UUID> {

}