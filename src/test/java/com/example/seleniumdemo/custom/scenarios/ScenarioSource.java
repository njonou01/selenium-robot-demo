package com.example.seleniumdemo.custom.scenarios;

import java.util.List;

public interface ScenarioSource {

	List<ScenarioDef> parse(byte[] content);
}
