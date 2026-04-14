package nl.uu.maze.search.strategy;

import nl.uu.maze.search.SearchTarget;
import nl.uu.maze.execution.symbolic.SymbolicState;
import nl.uu.maze.search.strategy.PrimePath.PrimePathGenerator;
import nl.uu.maze.util.BranchHistory;
import nl.uu.maze.util.PrefixTree;
import nl.uu.maze.util.Pair;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;
import java.util.HashMap;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import sootup.core.graph.StmtGraph;

public class PrimePathStrategy<T extends SearchTarget> extends SearchStrategy<T> {
    private final static Logger logger = LoggerFactory.getLogger(PrimePathStrategy.class);

    private final LinkedList<T> targets = new LinkedList<>();

    // Map from each cfg to all the prime paths for that cfg
    // First element in pair is all primepaths that are not yet covered by some state
    //  This is needed to know which states containing prime path still need to be discovered
    // Second element in pair is all primepaths that are not yet covered in some test
    //  This is needed in order to know which states we should keep exploring so they get covered
    private HashMap<StmtGraph<?>, Pair<PrefixTree<Integer>, PrefixTree<Integer>>> primePaths = new HashMap<StmtGraph<?>, Pair<PrefixTree<Integer>, PrefixTree<Integer>>>();

    public String getName() {
        return "PrimePathStrategy";
    }

    @Override
    public void add(T target) {
        var cfg = target.getCFG();
        // if it is the first time seeing this cfg, generate prime paths for it
        // we only generate prime paths for the top level functions
        if (!primePaths.containsKey(cfg) && target.getCallDepth() == 0) {
            var tree1 = new PrefixTree<Integer>();
            var tree2 = new PrefixTree<Integer>();
            primePaths.put(cfg, new Pair<PrefixTree<Integer>, PrefixTree<Integer>>(tree1, tree2));
            var paths = PrimePathGenerator.GeneratePaths(cfg);
            logger.info("Added {} prime path targets", paths.size());
            for (var path: paths)
            {
                var branchhistory = BranchHistory.ConvertPathToBranchHistory(path, cfg);
                // branchhistory could possibly generate duplicates, but the
                // prefixtree does not add duplicates so this is not a problem
                tree1.insert(branchhistory);
                tree2.insert(branchhistory);
            }
        }
        primePaths.get(cfg).first().removeSublists(target.getBranchHistory());
        targets.add(target);
    }

    @Override
    public boolean requiresBranchHistoryData() {
        return true;
    }

    @Override
    public void generatedTestCase(SymbolicState state) {
        var paths = primePaths.get(state.getCFG());
        // Remove covered paths from the set of paths that still need to be tested
        if(!paths.second().removeSublists(state.getBranchHistory())){
            logger.warn("Generated test case doesn't cover any prime path");
            BranchHistory.LogHistory(state);
        }
    }

    @Override
    public void remove(T target) {
        targets.remove(target);
    }

    @Override
    public T next() {
        // Only continue searching if there is an uncovered prime path
        if (primePathsEmpty()) {
            logger.info("All prime paths covered");
            targets.clear();
            return null;
        }

        // First try to find a prime path that hasn't been explored yet
        var nextState = nextUncoveredInState();
        if (nextState != null) {
            logger.info("Returning next state");
            return nextState;
        }

        // Else try to find a state that contains a primepath that hasn't had a 
        // test case generated for it yet
        var nextState2 = nextUncoveredInTests();
        if (nextState2 != null) {
            logger.info("Returning next test state");
            return nextState2;
        }

        // If no prime paths matches any of the states fall back on BFS
        // Needed to find beginning of any prime path, or to finish a prime path to a terminal state
        // TODO: maybe replace this with more direct search strategy (similar to distance to uncovered heuristic)?
        return targets.isEmpty() ? null : targets.remove();
    }

    /** try to find a prime path that hasn't been explored yet */
    private T nextUncoveredInState() {
        return nextUncoveredState(false);
    }

    /** try to find a state that contains a primepath that doesn't have a test case yet */
    private T nextUncoveredInTests() {
        return nextUncoveredState(true);
    }

    private T nextUncoveredState(boolean tests) {
        var iterator = tests ? targets.iterator() : targets.descendingIterator();
        while (iterator.hasNext()) {
            var target = iterator.next();
            var paths = primePaths.get(target.getCFG());
            if (paths == null) continue;
            var containsTarget = tests ? paths.second().containsSublist(target.getBranchHistory()) : paths.first().containsPrefix(target.getBranchHistory());
            // If the history matches with any of the goal prime paths, take this state next
            if (target.getCallDepth() == 0 && containsTarget) {
                targets.remove(target);
                return target;
            }
        }
        return null;
    }

    @Override
    public int size() {
        return targets.size();
    }

    @Override
    public void reset() {
        targets.clear();
    }

    @Override
    public Collection<T> getAll() {
        return targets;
    }

    /** Returns true if no more prime paths targets are present */
    private boolean primePathsEmpty() {
        for (var entry: primePaths.values()) {
            if(!entry.first().empty() || !entry.second().empty()) {
                return false;
            }
        }
        return true;
    }
}