package com.example.seleniumdemo.custom.server;

import java.io.File;
import java.nio.charset.StandardCharsets;

import org.json.JSONArray;
import org.json.JSONObject;

import kong.unirest.core.HttpResponse;
import kong.unirest.core.Unirest;

public final class VariableServerClient {

	public record FetchedVariable(byte[] content, String fileName) { }

	private final String baseUrl;
	private final String token;
	private final String application;
	private final String version;
	private final String environment;

	public VariableServerClient(String baseUrl, String token, String application, String version, String environment) {
		this.baseUrl = baseUrl;
		this.token = token;
		this.application = application;
		this.version = version;
		this.environment = environment;
	}

	public VariableServerClient(VariableServerConfig config) {
		this(config.baseUrl(), config.token(), config.application(), config.version(), config.environment());
	}

	public FetchedVariable fetch(String variableName) {
		JSONArray existing = search(variableName);
		for (int i = 0; i < existing.length(); i++) {
			JSONObject node = existing.getJSONObject(i);
			if (!variableName.equals(node.optString("name"))) {
				continue;
			}

			String uploadFileName = node.optString("uploadFile", null);
			if (uploadFileName != null && !uploadFileName.isBlank()) {
				HttpResponse<byte[]> fileResponse = Unirest.get(baseUrl + "/variable/api/variable/" + node.getInt("id") + "/file")
						.header("Authorization", "Token " + token)
						.asBytes();
				if (!fileResponse.isSuccess()) {
					throw new IllegalStateException(
							"Echec telechargement du fichier '" + variableName + "' (HTTP " + fileResponse.getStatus() + ")");
				}
				return new FetchedVariable(fileResponse.getBody(), uploadFileName);
			}

			return new FetchedVariable(node.optString("value", "").getBytes(StandardCharsets.UTF_8), null);
		}
		throw new IllegalStateException("Variable '" + variableName + "' absente du serveur de variable.");
	}

	public void uploadFile(String variableName, File file, String description) {
		JSONArray existing = search(variableName);

		HttpResponse<String> uploadResponse;
		if (existing.length() > 0) {
			int id = existing.getJSONObject(0).getInt("id");
			uploadResponse = Unirest.patch(baseUrl + "/variable/api/variable/" + id + "/")
					.header("Authorization", "Token " + token)
					.field("uploadFile", file)
					.asString();
		} else {
			int applicationId = lookupId("/commons/api/application/", application);
			int environmentId = lookupId("/commons/api/environment/", environment);

			uploadResponse = Unirest.post(baseUrl + "/variable/api/variable/")
					.header("Authorization", "Token " + token)
					.field("name", variableName)
					.field("application", String.valueOf(applicationId))
					.field("environment", String.valueOf(environmentId))
					.field("description", description)
					.field("uploadFile", file)
					.asString();
		}

		if (!uploadResponse.isSuccess()) {
			throw new IllegalStateException(
					"Echec upload de '" + variableName + "' (HTTP " + uploadResponse.getStatus() + "): " + uploadResponse.getBody());
		}
	}

	private JSONArray search(String variableName) {
		HttpResponse<String> searchResponse = Unirest.get(baseUrl + "/variable/api/variable/")
				.header("Authorization", "Token " + token)
				.queryString("application", application)
				.queryString("version", version)
				.queryString("environment", environment)
				.queryString("name", variableName)
				.asString();

		if (!searchResponse.isSuccess()) {
			throw new IllegalStateException(
					"Echec recherche de '" + variableName + "' (HTTP " + searchResponse.getStatus() + "): " + searchResponse.getBody());
		}
		return new JSONArray(searchResponse.getBody());
	}

	private int lookupId(String apiPath, String name) {
		HttpResponse<String> response = Unirest.get(baseUrl + apiPath)
				.header("Authorization", "Token " + token)
				.queryString("name", name)
				.asString();

		if (!response.isSuccess()) {
			throw new IllegalStateException(
					"Echec recherche id pour '" + name + "' sur " + apiPath + " (HTTP " + response.getStatus() + "): " + response.getBody());
		}
		return new JSONObject(response.getBody()).getInt("id");
	}
}
