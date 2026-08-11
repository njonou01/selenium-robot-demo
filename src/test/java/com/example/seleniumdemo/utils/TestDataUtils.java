package com.example.seleniumdemo.utils;

import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;

public class TestDataUtils {

	private static final String[] FIRST_NAMES = {"Jean", "Sophie", "Pierre", "Marie", "Robert", "Alice", "Thomas", "Claire"};
	private static final String[] LAST_NAMES = {"Dupont", "Bernard", "Martin", "Lefevre", "Wilson", "Smith", "Rousseau", "Girard"};
	private static final String[] CITIES = {"Paris", "Lyon", "Marseille", "Bordeaux", "Lille", "Nantes", "Toulouse"};
	private static final String[] COUNTRIES = {"France", "Belgium", "Switzerland", "Canada", "USA"};
	private static final String[] STREET_NAMES = {"avenue de la Republique", "rue de Paris", "boulevard Voltaire", "rue Victor Hugo"};

	private TestDataUtils() {
	}

	public static String uniqueReference(String prefix) {
		return prefix + System.currentTimeMillis() + randomDigits(4);
	}

	public static String uniqueUsername(String prefix) {
		return uniqueReference(prefix);
	}

	public static String uniqueEmail(String prefix) {
		return uniqueReference(prefix) + "@example.com";
	}

	public static String uniqueSinistreReference() {
		return "SINISTRE-" + LocalDate.now().getYear() + "-" + randomDigits(6);
	}

	public static String uniqueOrderReference() {
		return "ORD-" + System.currentTimeMillis();
	}

	public static String uniqueTransactionReference() {
		return "TXN-" + System.currentTimeMillis() + randomDigits(3);
	}

	public static String randomPhoneNumber() {
		return "06" + randomDigits(8);
	}

	public static String randomAccountNumber() {
		return randomDigits(6);
	}

	public static String randomDigits(int count) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < count; i++) {
			sb.append(ThreadLocalRandom.current().nextInt(10));
		}
		return sb.toString();
	}

	public static String randomAmount(int min, int max) {
		return String.valueOf(ThreadLocalRandom.current().nextInt(min, max + 1));
	}

	public static String randomFirstName() {
		return pick(FIRST_NAMES);
	}

	public static String randomLastName() {
		return pick(LAST_NAMES);
	}

	public static String randomFullName() {
		return randomFirstName() + " " + randomLastName();
	}

	public static String randomCity() {
		return pick(CITIES);
	}

	public static String randomCountry() {
		return pick(COUNTRIES);
	}

	public static String randomStreetAddress() {
		return ThreadLocalRandom.current().nextInt(1, 200) + " " + pick(STREET_NAMES);
	}

	public static String randomZipCode() {
		return randomDigits(5);
	}

	public static String randomCreditCardNumber() {
		return "4" + randomDigits(15);
	}

	public static String randomExpiryMonth() {
		return String.format("%02d", ThreadLocalRandom.current().nextInt(1, 13));
	}

	public static String randomExpiryYear() {
		return String.valueOf(LocalDate.now().getYear() + ThreadLocalRandom.current().nextInt(1, 6));
	}

	private static String pick(String[] values) {
		return values[ThreadLocalRandom.current().nextInt(values.length)];
	}
}
