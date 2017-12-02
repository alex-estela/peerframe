package fr.estela.peerframe.device.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping(value = "/api")
public class PingController extends AbstractController {

    @RequestMapping(value = "/tools/ping", method = RequestMethod.GET)
    public ResponseEntity<String> toolsPingGet() {
        return populateRetrievedResponse("1");
    }
}