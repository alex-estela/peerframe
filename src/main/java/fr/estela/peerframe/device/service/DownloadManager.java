package fr.estela.peerframe.device.service;

import fr.estela.peerframe.device.entity.ProviderEntity;
import fr.estela.peerframe.device.repository.MediaRepository;
import fr.estela.peerframe.device.repository.ProviderRepository;

public interface DownloadManager {

    public void downloadAndSaveMedias(ProviderEntity providerEntity, ProviderRepository providerRepository, MediaRepository mediaRepository) throws Exception;    
}
