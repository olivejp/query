package nc.deveo.query;

import nc.deveo.query.entity.Contrat;
import nc.deveo.query.entity.Facture;
import nc.deveo.query.repository.ContratRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = QueryApplication.class,
        properties = "spring.liquibase.enabled=true")
@AutoConfigureMockMvc
public class ContratControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ContratRepository repository;

    @Before
    public void setup() {
        Contrat contrat = new Contrat();
        contrat.setId(1L);
        contrat.setNom("OLIVE");

        Facture facture = new Facture();
        facture.setId(1L);
        facture.setLibelle("JEAN PAUL");
        facture.setContrat(contrat);
        contrat.setFactures(List.of(facture));
        repository.save(contrat);


        Contrat secondContrat = new Contrat();
        secondContrat.setId(2L);
        secondContrat.setNom("DELESSERT");

        Facture secondFacture = new Facture();
        secondFacture.setId(2L);
        secondFacture.setLibelle("STEPHANIE");
        secondFacture.setContrat(secondContrat);
        secondContrat.setFactures(List.of(secondFacture));
        repository.save(secondContrat);
    }

    @Test
    public void getContainsContrat() throws Exception {
        MvcResult result = mvc.perform(get("/api/contrat?nom|contains=liv"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(Integer.parseInt("1"))))
                .andExpect(jsonPath("$.content[0].nom", is("OLIVE")))
                .andReturn();

        assertThat(result).isNotNull();
    }

    @Test
    public void getStartWithContrat() throws Exception {
        mvc.perform(get("/api/contrat?nom|startsWith=oli"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(Integer.parseInt("1"))))
                .andExpect(jsonPath("$.content[0].nom", is("OLIVE")));
    }

    @Test
    public void getFactureLibelleContainsContrat() throws Exception {
        mvc.perform(get("/api/contrat?factures.libelle|contains=PHA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(Integer.parseInt("2"))))
                .andExpect(jsonPath("$.content[0].nom", is("DELESSERT")));
    }
}
