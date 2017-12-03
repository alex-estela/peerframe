package fr.estela.peerframe.device.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import fr.estela.peerframe.device.entity.SetupEntity;

public interface SetupRepository extends JpaRepository<SetupEntity, Long> {
    
}