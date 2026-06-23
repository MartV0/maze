package nl.uu.tests.maze;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.io.File;
import nl.uu.maze.analysis.JavaAnalyzer;
import nl.uu.maze.search.strategy.PathGenerator.EdgePairGenerator;
import org.junit.jupiter.api.Test;
import sootup.core.model.SootClass;
import sootup.java.core.JavaSootMethod;
import sootup.java.core.types.JavaClassType;
import nl.uu.maze.analysis.JavaAnalyzer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * For testing the edge pair path generator
 */
public class EdgePairGeneratorTest {
	static public class CUT_SimpleLoop {
		public static int func_with_loop(int max) {
			int r = 0;

			for (int i = 0; i < max; i++) {
				if (i % 2 == 0) {
					r += i;
				} else {
					r++;
				}
			}

			return r;
		}
	}
	String binClassesDir = "./target/test-classes" ;
	
	@SuppressWarnings("rawtypes")
	Class CUT     = CUT_SimpleLoop.class ;
	SootClass SootCUT;

	@Test
	void test_edge_pair_generation() throws Exception {
		// Set up to be able to extract cfg from the test class
		String[] paths = binClassesDir.split(File.pathSeparator);
		URL[] urls = new URL[paths.length];
		for (int i = 0; i < paths.length; i++) {
			urls[i] = Paths.get(paths[i]).toUri().toURL();
		}
		var classLoader = new URLClassLoader(urls);
		var analyzer = JavaAnalyzer.initialize(binClassesDir, classLoader);
        JavaClassType classType = analyzer.getClassType(CUT.getName());
        var sootClass = analyzer.getSootClass(classType);
        var methods = sootClass.getMethods();
		JavaSootMethod method = null;
		for (JavaSootMethod m: methods) {
			if (m.getName().equals("func_with_loop")) {
				method = m;
			}
		}
		var cfg = analyzer.getCFG(method);

		// Evoke the path generator
		EdgePairGenerator generator = new EdgePairGenerator();
		var pathss = generator.GeneratePaths(cfg).toString();
		// Paths are converted to string, makes creating the assertions much easier
		assertTrue(pathss.contains("if i >= max, $stack3 = i % 2, if $stack3 != 0, r = r + i"));
		assertTrue(pathss.contains("if i >= max, $stack3 = i % 2, if $stack3 != 0, r = r + 1"));
		assertTrue(pathss.contains("max := @parameter0: int, r = 0, i = 0, if i >= max, $stack3 = i % 2"));
		assertTrue(pathss.contains("max := @parameter0: int, r = 0, i = 0, if i >= max, return r"));
		assertTrue(pathss.contains("if $stack3 != 0, r = r + i, goto, i = i + 1, goto, if i >= max, $stack3 = i % 2"));
		assertTrue(pathss.contains("if $stack3 != 0, r = r + i, goto, i = i + 1, goto, if i >= max, return r"));
		assertTrue(pathss.contains("if $stack3 != 0, r = r + 1, i = i + 1, goto, if i >= max, $stack3 = i % 2"));
		assertTrue(pathss.contains("if $stack3 != 0, r = r + 1, i = i + 1, goto, if i >= max, return r"));
	}
}
