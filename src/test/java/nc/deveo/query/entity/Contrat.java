package nc.deveo.query.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.util.List;

@Entity
@Getter
@Setter
public class Contrat {

    @Id
    private Long id;

    private String nom;

    @JsonIgnoreProperties("contrat")
    @OneToMany(mappedBy = "contrat", cascade = CascadeType.ALL)
    private List<Facture> factures;
}
