package nc.deveo.query.repository;

import nc.deveo.query.entity.Contrat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ContratRepository extends JpaRepository<Contrat, Long>, JpaSpecificationExecutor<Contrat> {
}
