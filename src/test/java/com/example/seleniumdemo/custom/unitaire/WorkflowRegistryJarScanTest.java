package com.example.seleniumdemo.custom.unitaire;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.example.seleniumdemo.custom.reporting.WorkflowRegistry;
import com.example.seleniumdemo.workflows.BankingWorkflow;

/**
 * 'WorkflowRegistry.discoverWorkflowClasses()' a une branche dediee a l'execution packagee
 * (jar) - c'est justement celle qui a eu un vrai bug ("URI is not hierarchical", voir
 * jenkins-jar-fixes.md) avant d'etre corrigee. Jamais couverte par un test automatise depuis -
 * seulement verifiee manuellement a l'epoque de l'incident. Empaquette les classes compilees
 * du projet dans un vrai jar temporaire, force la resolution des ressources a passer par une
 * URL "jar:", et verifie que le scan trouve bien les workflows sans lever d'exception.
 */
public class WorkflowRegistryJarScanTest {

	@Test
	public void discoversWorkflowClassesThroughAJarProtocolUrlWithoutCrashing() throws Exception {
		File jarFile = buildJarFromTestClasses();
		ClassLoader original = Thread.currentThread().getContextClassLoader();
		URLClassLoader jarClassLoader = new URLClassLoader(new URL[] { jarFile.toURI().toURL() }, original);

		try {
			Thread.currentThread().setContextClassLoader(jarClassLoader);

			List<Class<?>> discovered = WorkflowRegistry.discoverWorkflowClasses();

			// Class.forName(String) resout via le classloader de l'appelant (WorkflowRegistry),
			// pas via le contexte du thread - la classe retournee est donc la meme, chargee
			// normalement, que via le jar. Ce qui est prouve ici, c'est la partie qui comptait
			// dans l'incident historique: parcourir une entree "jar:" (JarURLConnection ->
			// JarFile -> JarEntry, plus de "new File(uri)") sans lever "URI is not hierarchical",
			// et retrouver correctement les classes @Workflow par leur chemin dans l'archive.
			Assert.assertTrue(discovered.stream().anyMatch(c -> c.getName().equals(BankingWorkflow.class.getName())),
					"le parcours de l'entree jar doit retrouver BankingWorkflow sans exception");
		} finally {
			Thread.currentThread().setContextClassLoader(original);
			jarClassLoader.close();
			jarFile.delete();
		}
	}

	private File buildJarFromTestClasses() throws Exception {
		Path classesRoot = Path.of("target", "test-classes");
		File jarFile = File.createTempFile("workflow-registry-jar-scan-", ".jar");
		jarFile.deleteOnExit();

		try (JarOutputStream jarOut = new JarOutputStream(Files.newOutputStream(jarFile.toPath()))) {
			try (Stream<Path> paths = Files.walk(classesRoot)) {
				for (Path path : paths.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".class")).toList()) {
					String entryName = classesRoot.relativize(path).toString().replace(File.separatorChar, '/');
					jarOut.putNextEntry(new JarEntry(entryName));
					try (FileInputStream in = new FileInputStream(path.toFile())) {
						in.transferTo(jarOut);
					}
					jarOut.closeEntry();
				}
			}
		}
		return jarFile;
	}
}
