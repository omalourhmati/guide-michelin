package fr.michelin.guide;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Index Lucene dans un dossier à part, pour ne pas gêner l'application si elle tourne en même temps
@SpringBootTest(properties = "lucene.dossier=target/index-test")
class GuideApplicationTests {

	@Test
	void contextLoads() {
	}

}
