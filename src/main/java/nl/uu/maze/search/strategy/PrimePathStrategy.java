package nl.uu.maze.search.strategy;

import nl.uu.maze.search.SearchTarget;
import nl.uu.maze.execution.symbolic.SymbolicState;
import nl.uu.maze.search.strategy.PrimePath.PrimePathGenerator;
import nl.uu.maze.util.BranchHistory;
import nl.uu.maze.util.PrefixTree;

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
    // Map from each cfg to all the prime paths for that cfg, stored as a prefix
    // tree for efficient querying
    private HashMap<StmtGraph<?>, PrefixTree<Integer>> primePaths = new HashMap<StmtGraph<?>, PrefixTree<Integer>>();

    public String getName() {
        return "PrimePathStrategy";
    }

    @Override
    public void add(T target) {
        var cfg = target.getCFG();
        // if it is the first time seeing this cfg, generate prime paths for it
        // we only generate prime paths for the top level functions
        if (!primePaths.containsKey(cfg) && target.getCallDepth() == 0) {
            var tree = new PrefixTree<Integer>();
            primePaths.put(cfg, tree);
            var paths = PrimePathGenerator.GeneratePaths(cfg);
            for (var path: paths)
            {
                var branchhistory = BranchHistory.ConvertPathToBranchHistory(path, cfg);
                // branchhistory could possibly generate duplicates, but the
                // prefixtree does not add duplicates so this is not a problem
                tree.insert(branchhistory);
            }
        }
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
        if(!paths.removeSublists(state.getBranchHistory())){
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
            targets.clear();
            return null;
        }
        // TODO: Maybe do this in reverse order? that way new prime paths get fully discovered before moving on to other targets
        for (var target: targets) {
            var paths = primePaths.get(target.getCFG());
            // If the history matches with any of the goal prime paths, take this state next
            if (paths != null && target.getCallDepth() == 0 && paths.containsPrefix(target.getBranchHistory())) {
                targets.remove(target);
                return target;
            }
        }
        // If no prime paths matches any of the states fall back on BFS
        // Needed to find beginning of any prime path, or to finish a prime path to a terminal state
        // TODO: maybe replace this with more direct search strategy (similar to distance to uncovered heuristic)?
        return targets.isEmpty() ? null : targets.remove();
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
            if(!entry.empty()) {
                return false;
            }
        }
        return true;
    }
}