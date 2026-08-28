package com.example.seleniumdemo.custom.integration;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.example.seleniumdemo.custom.scenarios.ExcelScenarioSource;
import com.example.seleniumdemo.custom.scenarios.JsonScenarioSource;
import com.example.seleniumdemo.custom.scenarios.ScenarioDef;
import com.example.seleniumdemo.custom.server.VariableServerClient;
import com.example.seleniumdemo.custom.server.VariableServerConfig;
import com.example.seleniumdemo.custom.tests.ServerDrivenScenarioTest;

/**
 * Complete 'VariableServerChainingIntegrationTest' (qui ne couvrait que la lecture de la
 * variable 'workflow.scenarios' partagee): ici, chemin d'upload (jamais teste avant - seul le
 * fetch l'etait), variable absente, doublon de nom via un vrai aller-retour HTTP, round-trip
 * Excel reel. Chaque test utilise sa PROPRE variable ('test.integration.*'), jamais
 * 'workflow.scenarios', pour ne pas interferer avec des runs manuels en cours. Pas de suppression
 * possible cote client (VariableServerClient n'expose pas de delete) - ces variables de test
 * restent sur le serveur apres coup, assume, meme principe que le reste de la session.
 */
public class VariableServerRoundTripIntegrationTest {

	private static final String SERVER_URL = "http://localhost:8000";
	private static final String APPLICATION = "seleniumdemo";
	private static final String VERSION = "1.0";
	private static final String ENVIRONMENT = "DEV";

	private VariableServerClient client() {
		String token = System.getenv("SELENIUM_ROBOT_SERVER_TOKEN");
		VariableServerConfig config = new VariableServerConfig(SERVER_URL, token, APPLICATION, VERSION, ENVIRONMENT);
		return new VariableServerClient(config);
	}

	private File writeTempFile(String suffix, byte[] content) throws Exception {
		File file = File.createTempFile("integration-test-", suffix);
		file.deleteOnExit();
		Files.write(file.toPath(), content);
		return file;
	}

	@Test
	public void uploadThenFetchRoundTripsTheSameJsonContent() throws Exception {
		String json = "[{\"name\":\"RoundTrip " + System.currentTimeMillis() + "\",\"sinistre\":\"S1\","
				+ "\"sbc\":false,\"steps\":[],\"dataSet\":{}}]";
		File file = writeTempFile(".json", json.getBytes(StandardCharsets.UTF_8));

		client().uploadFile("test.integration.roundtrip", file, "Ecrit par VariableServerRoundTripIntegrationTest");
		VariableServerClient.FetchedVariable fetched = client().fetch("test.integration.roundtrip");

		Assert.assertEquals(new String(fetched.content(), StandardCharsets.UTF_8), json,
				"le contenu recupere doit etre identique, octet pour octet, a ce qui a ete uploade");
	}

	@Test(expectedExceptions = IllegalStateException.class,
			expectedExceptionsMessageRegExp = ".*absente du serveur de variable.*")
	public void fetchingAnUnknownVariableFailsWithClearMessage() {
		client().fetch("test.integration.does.not.exist." + System.currentTimeMillis());
	}

	/**
	 * Meme controle que 'ServerDrivenScenarioTest.requireUniqueNames' teste en memoire dans
	 * 'unitaire/', mais ici via un vrai aller-retour HTTP: upload d'un JSON avec 2 scenarios de
	 * meme nom, fetch, parse, puis le meme appel que fait vraiment le DataProvider en
	 * production.
	 */
	@Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = ".*apparait plusieurs fois.*")
	public void duplicateScenarioNameUploadedToRealServerIsRejectedAfterRealFetch() throws Exception {
		String json = "[{\"name\":\"Doublon Integration\",\"sinistre\":\"S1\",\"sbc\":false,\"steps\":[]},"
				+ "{\"name\":\"Doublon Integration\",\"sinistre\":\"S2\",\"sbc\":false,\"steps\":[]}]";
		File file = writeTempFile(".json", json.getBytes(StandardCharsets.UTF_8));
		client().uploadFile("test.integration.duplicate", file, "Ecrit par VariableServerRoundTripIntegrationTest");

		VariableServerClient.FetchedVariable fetched = client().fetch("test.integration.duplicate");
		List<ScenarioDef> scenarios = new JsonScenarioSource().parse(fetched.content());

		ServerDrivenScenarioTest.requireUniqueNames("test.integration.duplicate", scenarios);
	}

	@Test
	public void excelVariableRoundTripsThroughRealServerAndParsesCorrectly() throws Exception {
		byte[] workbook = buildMinimalWorkbook();
		File file = writeTempFile(".xlsx", workbook);

		client().uploadFile("test.integration.excel", file, "Ecrit par VariableServerRoundTripIntegrationTest");
		VariableServerClient.FetchedVariable fetched = client().fetch("test.integration.excel");

		Assert.assertTrue(fetched.fileName() != null && fetched.fileName().toLowerCase().endsWith(".xlsx"),
				"le serveur doit renvoyer le nom de fichier original pour detecter le format Excel");

		ScenarioDef scenario = new ExcelScenarioSource().parse(fetched.content()).get(0);
		Assert.assertNull(scenario.parseError());
		Assert.assertEquals(scenario.steps(), List.of("integration.step"));
	}

	private byte[] buildMinimalWorkbook() throws Exception {
		try (XSSFWorkbook workbook = new XSSFWorkbook()) {
			XSSFSheet sheet = workbook.createSheet("Scenarios");
			XSSFRow header = sheet.createRow(0);
			header.createCell(0).setCellValue("scenario_name");
			header.createCell(1).setCellValue("sinistre");
			header.createCell(2).setCellValue("step");
			header.createCell(3).setCellValue("sbc");
			header.createCell(4).setCellValue("dataSet");

			XSSFRow row = sheet.createRow(1);
			row.createCell(0).setCellValue("Excel RoundTrip");
			row.createCell(1).setCellValue("S1");
			row.createCell(2).setCellValue("integration.step");

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			workbook.write(out);
			return out.toByteArray();
		}
	}
}
