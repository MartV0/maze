package nl.uu.tests.maze;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import nl.uu.maze.analysis.JavaAnalyzer;
import nl.uu.maze.execution.DSEController;
import nl.uu.maze.search.strategy.PathStrategy;
import nl.uu.maze.main.cli.MazeCLI;
import nl.uu.maze.util.Z3ContextProvider;
import picocli.CommandLine;

/**
 * For testing MAZE generation of normal float/double values for parameters,
 * and special values like NaN and infinity.
 */
public class PrimePathStrategyTest {
	
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

	static public class CUT_BinarySearch {
		/** Returns the index of the target in the sorted array. */
		public static int binarySearch(int[] arr, int target) {
			int low = 0, high = arr.length - 1;

			while (low <= high) {
				int mid = low + ((high - low) >>> 1);
				if (arr[mid] == target) {
					return mid;
				} else if (arr[mid] < target) {
					low = mid + 1;
				} else {
					high = mid - 1;
				}
			}

			return -1; // Target not found
		}
	}
	
	String binClassesDir = "./target/test-classes" ;
	String outputDir = "./tmp" ;
	
	@SuppressWarnings("rawtypes")
	Class CUT     = CUT_SimpleLoop.class ;
	Class CUT2     = CUT_BinarySearch.class ;
	String sp = " " ;
	
	LoggerInterceptor interceptor ;
	
	@BeforeEach
	void setup() {
		// make the JavaAnalyzer to drop its current instance, to force a fresh one
		// to be created:
		JavaAnalyzer.dropInstance();
		
		// setting logger interceptor:
		Logger logger = (Logger) LoggerFactory.getLogger(PathStrategy.class);
		this.interceptor = new LoggerInterceptor() ;
		interceptor.start();
		logger.setLevel(Level.DEBUG);
		logger.addAppender(interceptor);

		// remove the output-test-file produced by MAZE:
		TestUtils.removeFile(Path.of(outputDir, CUT.getSimpleName() + "Test.java"));
		TestUtils.removeFile(Path.of(outputDir, CUT2.getSimpleName() + "Test.java"));
	}
	
	//@AfterAll  
	static void cleanup() {
		// ... does not work, will cause other test classes invoking MAZE to crash
		Z3ContextProvider.close();
	}
	
	@Test
	void simple_loop_covered() throws IOException {

		String argz =   "--classpath=" + binClassesDir
				      + sp + "--classname=" + CUT.getName() 
				      + sp + "--output-path=" + outputDir 
				      + sp + "--do-not-close-z3-context=true" // don't close z3 context, or else the next tests will crash
				      + sp + "--strategy=PrimePath"
				      + sp + "--log-level=DEBUG"
				      ;
	    int exitCode = new CommandLine(new MazeCLI()).execute(argz.split(" ") );
	    
		// Infeasible paths have been commented out
		// TODO: these were copied from path generator output, check them manually
		String[] primePaths = {
			"goto, if i >= max, $stack3 = i % 2, if $stack3 != 0, r = r + i, goto, i = i + 1, goto",
			"goto, if i >= max, $stack3 = i % 2, if $stack3 != 0, r = r + 1, i = i + 1, goto",
			"if $stack3 != 0, r = r + i, goto, i = i + 1, goto, if i >= max, $stack3 = i % 2, if $stack3 != 0",
			"if $stack3 != 0, r = r + 1, i = i + 1, goto, if i >= max, $stack3 = i % 2, if $stack3 != 0",
			//"goto, i = i + 1, goto, if i >= max, $stack3 = i % 2, if $stack3 != 0, r = r + i, goto",
			"max := @parameter0: int, r = 0, i = 0, if i >= max, $stack3 = i % 2, if $stack3 != 0, r = r + i, goto, i = i + 1, goto",
			//"max := @parameter0: int, r = 0, i = 0, if i >= max, $stack3 = i % 2, if $stack3 != 0, r = r + 1, i = i + 1, goto",
			"max := @parameter0: int, r = 0, i = 0, if i >= max, return r",
			"r = r + 1, i = i + 1, goto, if i >= max, $stack3 = i % 2, if $stack3 != 0, r = r + i, goto",
			//"r = r + 1, i = i + 1, goto, if i >= max, $stack3 = i % 2, if $stack3 != 0, r = r + 1",
			"i = i + 1, goto, if i >= max, $stack3 = i % 2, if $stack3 != 0, r = r + i, goto, i = i + 1",
			"i = i + 1, goto, if i >= max, $stack3 = i % 2, if $stack3 != 0, r = r + 1, i = i + 1",
			"$stack3 = i % 2, if $stack3 != 0, r = r + i, goto, i = i + 1, goto, if i >= max, $stack3 = i % 2",
			"$stack3 = i % 2, if $stack3 != 0, r = r + i, goto, i = i + 1, goto, if i >= max, return r",
			"$stack3 = i % 2, if $stack3 != 0, r = r + 1, i = i + 1, goto, if i >= max, $stack3 = i % 2",
			"$stack3 = i % 2, if $stack3 != 0, r = r + 1, i = i + 1, goto, if i >= max, return r",
			//"r = r + i, goto, i = i + 1, goto, if i >= max, $stack3 = i % 2, if $stack3 != 0, r = r + i",
			"r = r + i, goto, i = i + 1, goto, if i >= max, $stack3 = i % 2, if $stack3 != 0, r = r + 1",
			"if i >= max, $stack3 = i % 2, if $stack3 != 0, r = r + i, goto, i = i + 1, goto, if i >= max",
			"if i >= max, $stack3 = i % 2, if $stack3 != 0, r = r + 1, i = i + 1, goto, if i >= max",
		};
		
		// Assert all prime paths have been covered
		for (String primePath: primePaths) {
			// System.out.println(primePath);
			assertTrue(interceptor.anyMatch(msg -> msg.contains("Covered:") && msg.contains(primePath)));
		}

		// TODO: shouldn't happen if handling infeasible paths well
		// assertFalse(interceptor.anyMatch(msg -> msg.contains("Search space has been exhausted")));
	}

	@Test
	void bin_search_covered() throws IOException {

		String argz =   "--classpath=" + binClassesDir
				      + sp + "--classname=" + CUT2.getName() 
				      + sp + "--output-path=" + outputDir 
				      + sp + "--do-not-close-z3-context=true" // don't close z3 context, or else the next tests will crash
				      + sp + "--strategy=PrimePath"
				      + sp + "--log-level=DEBUG"
				      ;
	    int exitCode = new CommandLine(new MazeCLI()).execute(argz.split(" ") );
	    
		// Infeasible paths have been commented out
		// TODO: these were copied from path generator output, check them manually
		String[] primePaths = {
			"$stack6 = high - low, $stack7 = $stack6 >>> 1, mid = low + $stack7, $stack8 = arr[mid], if $stack8 != target, $stack9 = arr[mid], if $stack9 >= target, low = mid + 1, goto, goto, if low > high, $stack6 = high - low",
			"$stack6 = high - low, $stack7 = $stack6 >>> 1, mid = low + $stack7, $stack8 = arr[mid], if $stack8 != target, $stack9 = arr[mid], if $stack9 >= target, low = mid + 1, goto, goto, if low > high, return -1",
			"$stack6 = high - low, $stack7 = $stack6 >>> 1, mid = low + $stack7, $stack8 = arr[mid], if $stack8 != target, $stack9 = arr[mid], if $stack9 >= target, high = mid - 1, goto, if low > high, $stack6 = high - low",
			"$stack6 = high - low, $stack7 = $stack6 >>> 1, mid = low + $stack7, $stack8 = arr[mid], if $stack8 != target, $stack9 = arr[mid], if $stack9 >= target, high = mid - 1, goto, if low > high, return -1",
			"$stack7 = $stack6 >>> 1, mid = low + $stack7, $stack8 = arr[mid], if $stack8 != target, $stack9 = arr[mid], if $stack9 >= target, low = mid + 1, goto, goto, if low > high, $stack6 = high - low, $stack7 = $stack6 >>> 1",
			"$stack7 = $stack6 >>> 1, mid = low + $stack7, $stack8 = arr[mid], if $stack8 != target, $stack9 = arr[mid], if $stack9 >= target, high = mid - 1, goto, if low > high, $stack6 = high - low, $stack7 = $stack6 >>> 1",
			"low = mid + 1, goto, goto, if low > high, $stack6 = high - low, $stack7 = $stack6 >>> 1, mid = low + $stack7, $stack8 = arr[mid], if $stack8 != target, $stack9 = arr[mid], if $stack9 >= target, low = mid + 1",
			"low = mid + 1, goto, goto, if low > high, $stack6 = high - low, $stack7 = $stack6 >>> 1, mid = low + $stack7, $stack8 = arr[mid], if $stack8 != target, $stack9 = arr[mid], if $stack9 >= target, high = mid - 1",
			"if low > high, $stack6 = high - low, $stack7 = $stack6 >>> 1, mid = low + $stack7, $stack8 = arr[mid], if $stack8 != target, $stack9 = arr[mid], if $stack9 >= target, low = mid + 1, goto, goto, if low > high",
			"if low > high, $stack6 = high - low, $stack7 = $stack6 >>> 1, mid = low + $stack7, $stack8 = arr[mid], if $stack8 != target, $stack9 = arr[mid], if $stack9 >= target, high = mid - 1, goto, if low > high",
			"$stack8 = arr[mid], if $stack8 != target, $stack9 = arr[mid], if $stack9 >= target, low = mid + 1, goto, goto, if low > high, $stack6 = high - low, $stack7 = $stack6 >>> 1, mid = low + $stack7, $stack8 = arr[mid]",
			"$stack8 = arr[mid], if $stack8 != target, $stack9 = arr[mid], if $stack9 >= target, high = mid - 1, goto, if low > high, $stack6 = high - low, $stack7 = $stack6 >>> 1, mid = low + $stack7, $stack8 = arr[mid]",
			"high = mid - 1, goto, if low > high, $stack6 = high - low, $stack7 = $stack6 >>> 1, mid = low + $stack7, $stack8 = arr[mid], if $stack8 != target, $stack9 = arr[mid], if $stack9 >= target, low = mid + 1, goto",
			"high = mid - 1, goto, if low > high, $stack6 = high - low, $stack7 = $stack6 >>> 1, mid = low + $stack7, $stack8 = arr[mid], if $stack8 != target, $stack9 = arr[mid], if $stack9 >= target, high = mid - 1",
			"goto, goto, if low > high, $stack6 = high - low, $stack7 = $stack6 >>> 1, mid = low + $stack7, $stack8 = arr[mid], if $stack8 != target, $stack9 = arr[mid], if $stack9 >= target, low = mid + 1, goto",
			"$stack9 = arr[mid], if $stack9 >= target, low = mid + 1, goto, goto, if low > high, $stack6 = high - low, $stack7 = $stack6 >>> 1, mid = low + $stack7, $stack8 = arr[mid], if $stack8 != target, return mid",
			"$stack9 = arr[mid], if $stack9 >= target, low = mid + 1, goto, goto, if low > high, $stack6 = high - low, $stack7 = $stack6 >>> 1, mid = low + $stack7, $stack8 = arr[mid], if $stack8 != target, $stack9 = arr[mid]",
			"$stack9 = arr[mid], if $stack9 >= target, high = mid - 1, goto, if low > high, $stack6 = high - low, $stack7 = $stack6 >>> 1, mid = low + $stack7, $stack8 = arr[mid], if $stack8 != target, return mid",
			"$stack9 = arr[mid], if $stack9 >= target, high = mid - 1, goto, if low > high, $stack6 = high - low, $stack7 = $stack6 >>> 1, mid = low + $stack7, $stack8 = arr[mid], if $stack8 != target, $stack9 = arr[mid]",
			"mid = low + $stack7, $stack8 = arr[mid], if $stack8 != target, $stack9 = arr[mid], if $stack9 >= target, low = mid + 1, goto, goto, if low > high, $stack6 = high - low, $stack7 = $stack6 >>> 1, mid = low + $stack7",
			"mid = low + $stack7, $stack8 = arr[mid], if $stack8 != target, $stack9 = arr[mid], if $stack9 >= target, high = mid - 1, goto, if low > high, $stack6 = high - low, $stack7 = $stack6 >>> 1, mid = low + $stack7",
			"if $stack8 != target, $stack9 = arr[mid], if $stack9 >= target, low = mid + 1, goto, goto, if low > high, $stack6 = high - low, $stack7 = $stack6 >>> 1, mid = low + $stack7, $stack8 = arr[mid], if $stack8 != target",
			"if $stack8 != target, $stack9 = arr[mid], if $stack9 >= target, high = mid - 1, goto, if low > high, $stack6 = high - low, $stack7 = $stack6 >>> 1, mid = low + $stack7, $stack8 = arr[mid], if $stack8 != target",
			"if $stack9 >= target, low = mid + 1, goto, goto, if low > high, $stack6 = high - low, $stack7 = $stack6 >>> 1, mid = low + $stack7, $stack8 = arr[mid], if $stack8 != target, $stack9 = arr[mid], if $stack9 >= target",
			"if $stack9 >= target, high = mid - 1, goto, if low > high, $stack6 = high - low, $stack7 = $stack6 >>> 1, mid = low + $stack7, $stack8 = arr[mid], if $stack8 != target, $stack9 = arr[mid], if $stack9 >= target",
			"arr := @parameter0: int[], target := @parameter1: int, low = 0, $stack5 = lengthof arr, high = $stack5 - 1, if low > high, $stack6 = high - low, $stack7 = $stack6 >>> 1, mid = low + $stack7, $stack8 = arr[mid], if $stack8 != target, return mid",
			"arr := @parameter0: int[], target := @parameter1: int, low = 0, $stack5 = lengthof arr, high = $stack5 - 1, if low > high, $stack6 = high - low, $stack7 = $stack6 >>> 1, mid = low + $stack7, $stack8 = arr[mid], if $stack8 != target, $stack9 = arr[mid], if $stack9 >= target, low = mid + 1, goto, goto",
			"arr := @parameter0: int[], target := @parameter1: int, low = 0, $stack5 = lengthof arr, high = $stack5 - 1, if low > high, $stack6 = high - low, $stack7 = $stack6 >>> 1, mid = low + $stack7, $stack8 = arr[mid], if $stack8 != target, $stack9 = arr[mid], if $stack9 >= target, high = mid - 1, goto",
			"arr := @parameter0: int[], target := @parameter1: int, low = 0, $stack5 = lengthof arr, high = $stack5 - 1, if low > high, return -1",
			"goto, if low > high, $stack6 = high - low, $stack7 = $stack6 >>> 1, mid = low + $stack7, $stack8 = arr[mid], if $stack8 != target, $stack9 = arr[mid], if $stack9 >= target, low = mid + 1, goto, goto",
			"goto, if low > high, $stack6 = high - low, $stack7 = $stack6 >>> 1, mid = low + $stack7, $stack8 = arr[mid], if $stack8 != target, $stack9 = arr[mid], if $stack9 >= target, high = mid - 1, goto",
		};
		
		// Assert all prime paths have been covered
		for (String primePath: primePaths) {
			// System.out.println(primePath);
			assertTrue(interceptor.anyMatch(msg -> msg.contains("Covered:") && msg.contains(primePath)));
		}

		// All target paths are feasible so these shouldn't happen
		assertFalse(interceptor.anyMatch(msg -> msg.contains("Search space has been exhausted")));
	}
}
