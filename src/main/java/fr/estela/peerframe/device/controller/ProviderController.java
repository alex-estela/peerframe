package fr.estela.peerframe.device.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.estela.peerframe.api.model.Provider;
import fr.estela.peerframe.api.model.Provider.ProviderTypeEnum;
import fr.estela.peerframe.device.entity.ProviderEntity;
import fr.estela.peerframe.device.entity.SmugmugProviderEntity;
import fr.estela.peerframe.device.repository.ProviderRepository;
import fr.estela.peerframe.api.model.SmugmugProvider;

@Controller
@RequestMapping(value = "/api")
@Transactional
public class ProviderController extends AbstractController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProviderController.class);
    
    @Autowired
    private ProviderRepository providerRepository;

    private Provider toProvider(ProviderEntity providerEntity) throws Exception {

        Provider provider = null;      
        
        if (providerEntity instanceof SmugmugProviderEntity) {
            SmugmugProviderEntity smugmugProviderEntity = (SmugmugProviderEntity) providerEntity;
            SmugmugProvider smugmugProvider = new SmugmugProvider();
            smugmugProvider.setProviderType(ProviderTypeEnum.SMUGMUGPROVIDER);
            smugmugProvider.setAlbumId(smugmugProviderEntity.getAlbumId());
            smugmugProvider.setConsumerKey(smugmugProviderEntity.getConsumerKey());
            smugmugProvider.setConsumerSecret(smugmugProviderEntity.getConsumerSecret());
            smugmugProvider.setAccessToken(smugmugProviderEntity.getAccessToken());
            smugmugProvider.setAccessTokenSecret(smugmugProviderEntity.getAccessTokenSecret());
            provider = smugmugProvider;
        }
        else throw newBadRequestException("Unsupported provider type: " + providerEntity);

        provider.setId(providerEntity.getId().toString());
        provider.setName(providerEntity.getName());
        
        return provider;
    }

    private ProviderEntity toProviderEntity(Provider provider) throws Exception {

        ProviderEntity providerEntity = null;
        if (provider instanceof SmugmugProvider) {
            SmugmugProvider smugmugProvider = (SmugmugProvider) provider;
            SmugmugProviderEntity smugmugProviderEntity = new SmugmugProviderEntity();
            smugmugProviderEntity.setAlbumId(smugmugProvider.getAlbumId());
            smugmugProviderEntity.setConsumerKey(smugmugProvider.getConsumerKey());
            smugmugProviderEntity.setConsumerSecret(smugmugProvider.getConsumerSecret());
            smugmugProviderEntity.setAccessToken(smugmugProvider.getAccessToken());
            smugmugProviderEntity.setAccessTokenSecret(smugmugProvider.getAccessTokenSecret());
            providerEntity = smugmugProviderEntity;
        }
        else throw newBadRequestException("Unsupported provider type: " + provider);

        providerEntity.setId(provider.getId() == null ? UUID.randomUUID() : UUID.fromString(provider.getId()));
        providerEntity.setName(provider.getName());
        return providerEntity;
    }

    @RequestMapping(value = "/providers", method = RequestMethod.GET)
    public ResponseEntity<List<Provider>> providersGet() throws Exception {

        List<ProviderEntity> providerEntities = providerRepository.findAll();
        List<Provider> providers = new ArrayList<Provider>();
        for (ProviderEntity providerEntity : providerEntities) {
            Provider provider = toProvider(providerEntity);
            providers.add(provider);
        }

        return populateRetrievedResponse(providers);
    }

    @RequestMapping(value = "/providers", method = RequestMethod.POST)
    public ResponseEntity<Provider> providersPost(@RequestBody Provider provider) throws Exception {
        
        ProviderEntity providerEntity = toProviderEntity(provider);
        providerEntity = providerRepository.save(providerEntity);
        provider.setId(providerEntity.getId().toString());
        LOGGER.info("Created provider: {}", providerEntity);
        
        return populateCreatedResponse(provider);
    }

    @RequestMapping(value = "/providers/{providerId}", method = RequestMethod.PUT)
    public ResponseEntity<Provider> providersProviderIdPut(
        @PathVariable("providerId") String providerId,
        @RequestBody Provider provider) throws Exception {

        ProviderEntity providerEntity = toProviderEntity(provider);
        providerEntity.setId(UUID.fromString(providerId));
        providerRepository.save(providerEntity);
        LOGGER.info("Updated provider: {}", providerEntity);

        return populateRetrievedResponse(provider);
    }

    @RequestMapping(value = "/providers/{providerId}", method = RequestMethod.DELETE)
    public ResponseEntity<Void> providersProviderIdDelete(@PathVariable("providerId") String providerId) {

        providerRepository.delete(UUID.fromString(providerId));
        LOGGER.info("Deleted provider with id: {}", providerId);

        return populateDeletedResponse();
    }

}