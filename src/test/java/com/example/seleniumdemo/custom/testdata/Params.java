package com.example.seleniumdemo.custom.testdata;

import java.util.Map;

public interface Params {

	String get(String path);

	String get(String path, String defaultValue);

	Map<String, String> getSubtree(String path);
}
