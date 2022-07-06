package nc.deveo.query.controller;

import lombok.RequiredArgsConstructor;
import nc.deveo.query.ContratQueryService;
import nc.deveo.query.entity.Contrat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ContratController {

    private final ContratQueryService service;

    @GetMapping("/contrat")
    public Page<Contrat> getContrat(@RequestParam Map<String, String> allParams, final Pageable pageable) {
        return service.findByCriteria(allParams, pageable);
    }
}