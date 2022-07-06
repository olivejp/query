package nc.deveo.query;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {
        QueryApplication.class,}, properties = "spring.liquibase.enabled=true")
class QueryTests {

    @Autowired
    public ContratQueryService queryService;

    @Test
    void contextLoads() {
        assertThat(true);
    }

}
