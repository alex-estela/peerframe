package fr.estela.peerframe.device.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import fr.estela.peerframe.api.model.Media;
import fr.estela.peerframe.device.entity.MediaEntity;
import fr.estela.peerframe.device.repository.MediaRepository;
import fr.estela.peerframe.device.util.APIException;
import fr.estela.peerframe.device.util.ParsingUtil;

@Controller
@RequestMapping(value = "/api")
public class MediaController extends AbstractController {

    @Autowired
    private MediaRepository mediaRepository;

    @RequestMapping(value = "/medias", method = RequestMethod.GET)
    public ResponseEntity<List<Media>> mediasGet(@RequestParam(value = "random", required = false) Boolean random) {

        List<MediaEntity> mediaEntities = mediaRepository.findAll();
        List<Media> medias = new ArrayList<Media>();

        for (MediaEntity mediaEntity : mediaEntities) {

            Media media = new Media();
            media.setId(mediaEntity.getId().toString());
            media.setMediaType(mediaEntity.getMediaType());
            media.setWidth(mediaEntity.getLocalContent().getWidth());
            media.setHeight(mediaEntity.getLocalContent().getHeight());
            media.setLocationCity(mediaEntity.getLocationCity());
            media.setLocationCountry(mediaEntity.getLocationCountry());
            media.setCreated(ParsingUtil.toISO8601UTCString(mediaEntity.getOriginallyCreated()));

            medias.add(media);
        }

        if (random != null && random) Collections.shuffle(medias);

        return populateRetrievedResponse(medias);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @RequestMapping(value = "/medias/{mediaId}", produces = { "application/json", "image/jpeg", "image/png" }, method = RequestMethod.GET)
    public ResponseEntity<Media> mediasMediaIdGet(HttpServletRequest request, @PathVariable("mediaId") String mediaId)
        throws APIException {

        MediaEntity mediaEntity = mediaRepository.findOne(UUID.fromString(mediaId));
        if (mediaEntity == null) return populateNotFoundResponse();
        
        String requestMediaType = request.getHeader("Accept");
        ResponseEntity response = null;

        if (requestMediaType.contains("application/json")) {

            Media media = new Media();
            media.setId(mediaEntity.getId().toString());
            media.setMediaType(mediaEntity.getMediaType());
            media.setWidth(mediaEntity.getLocalContent().getWidth());
            media.setHeight(mediaEntity.getLocalContent().getHeight());
            media.setLocationCity(mediaEntity.getLocationCity());
            media.setLocationCountry(mediaEntity.getLocationCountry());
            media.setCreated(ParsingUtil.toISO8601UTCString(mediaEntity.getOriginallyCreated()));

            response = populateRetrievedResponse(media);
        }
        else if (requestMediaType.contains("image")) {

            byte[] bytes = mediaEntity.getLocalContent().getContentStream().getBytes();

            //MultivaluedMap<String, String> headers = new MultivaluedHashMap<String, String>();
            //headers.add("Content-Disposition", "attachment; filename=" + mediaEntity + "." + requestMediaType);
            //headers.add("Content-Length", "" + bytes.length);

            response = populateRetrievedResponse(bytes);
            //response.setContentType(new MediaType("image", requestMediaType));
            //response.setHeaders(headers);
        }
        else throw newBadRequestException("Invalid media type: " + requestMediaType);

        return response;
    }
}