package com.example.seleniumdemo.custom.unitaire;

import java.util.concurrent.atomic.AtomicInteger;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.example.seleniumdemo.custom.utils.Lazy;

public class LazyTest {

	@Test
	public void factoryIsCalledOnlyOnceAcrossMultipleGets() throws Exception {
		AtomicInteger callCount = new AtomicInteger();
		Lazy<String> lazy = new Lazy<>(() -> {
			callCount.incrementAndGet();
			return "value";
		});

		lazy.get();
		lazy.get();
		String result = lazy.get();

		Assert.assertEquals(result, "value");
		Assert.assertEquals(callCount.get(), 1, "la factory ne doit etre appelee qu'une seule fois");
	}

	@Test
	public void factoryIsNotCalledBeforeFirstGet() {
		AtomicInteger callCount = new AtomicInteger();
		new Lazy<>(() -> {
			callCount.incrementAndGet();
			return "value";
		});

		Assert.assertEquals(callCount.get(), 0, "la factory ne doit pas s'executer tant que get() n'a pas ete appele");
	}

	@Test(expectedExceptions = IllegalStateException.class)
	public void exceptionFromFactoryPropagates() throws Exception {
		Lazy<String> lazy = new Lazy<>(() -> {
			throw new IllegalStateException("boom");
		});

		lazy.get();
	}
}
