package nc.deveo.query;

import nc.deveo.query.entity.Contrat;
import nc.deveo.query.repository.ContratRepository;
import nc.deveo.query.service.QueryService;
import org.springframework.stereotype.Service;

@Service
public class ContratQueryService extends QueryService<Contrat, ContratRepository> {

    public ContratQueryService(ContratRepository repository) {
        super(repository);
    }

    @Override
    protected Class<Contrat> getType() {
        return Contrat.class;
    }
}
