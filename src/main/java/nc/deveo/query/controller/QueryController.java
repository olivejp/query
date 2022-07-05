package nc.deveo.query.controller;

import nc.deveo.query.service.QueryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

public interface QueryController<E, T extends QueryService<E, ?>> {

    T getQueryService();

    @GetMapping(path = "/query")
    default Page<E> query(@RequestParam final Map<String, String> allParams, final Pageable pageable) {
        return getQueryService().findByCriteria(allParams, pageable);
    }
}
