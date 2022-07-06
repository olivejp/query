package nc.deveo.query.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

@Entity
@Getter
@Setter
public class Facture {

    @Id
    private Long id;

    private String libelle;

    @ManyToOne
    private Contrat contrat;
}
