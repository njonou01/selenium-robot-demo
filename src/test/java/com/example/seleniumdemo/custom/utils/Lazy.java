package com.example.seleniumdemo.custom.utils;

import java.util.concurrent.Callable;

public class Lazy<T> {

	private final Callable<T> factory;
	private T value;

	public Lazy(Callable<T> factory) {
		this.factory = factory;
	}

	public T get() throws Exception {
		if (value == null) {
			value = factory.call();
		}
		return value;
	}
}
