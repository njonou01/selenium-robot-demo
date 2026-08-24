package com.example.seleniumdemo.custom.scenarios;

import java.util.List;
import java.util.Map;

public record ScenarioDef(String name, String sinistre, List<String> steps, boolean sbc, Map<String, Object> dataSet) { }
