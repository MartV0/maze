package nl.uu.maze.search.strategy;

import nl.uu.maze.search.SearchTarget;
import nl.uu.maze.execution.symbolic.SymbolicState;
import nl.uu.maze.search.strategy.PathGenerator.PathGenerator;
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

public class PathStrategy<T extends SearchTarget> extends SearchStrategy<T> {
    private final static Logger logger = LoggerFactory.getLogger(PathStrategy.class);

    private final LinkedList<T> targets = new LinkedList<>();

    // Map from each cfg to all the target paths for that cfg
    // First element in pair is all target paths that are not yet covered by some state
    //  This is needed to know which states containing target path still need to be discovered
    // Second element in pair is all target paths that are not yet covered in some test
    //  This is needed in order to know which states we should keep exploring so they get covered
    //  in an actual test case
    private HashMap<StmtGraph<?>, Pair<PrefixTree<Integer>, PrefixTree<Integer>>> targetPaths = new HashMap<StmtGraph<?>, Pair<PrefixTree<Integer>, PrefixTree<Integer>>>();

    // Used to generate any new target paths encountered
    PathGenerator pathGenerator;

    public PathStrategy(PathGenerator pathGenerator) {
        this.pathGenerator = pathGenerator;
    }

    public String getName() {
        return "PathStrategy_" + pathGenerator.getName();
    }

    @Override
    public void add(T target) {
        var cfg = target.getCFG();
        // if it is the first time seeing this cfg, generate target paths for it
        // we only generate target paths for the top level functions
        if (!targetPaths.containsKey(cfg) && target.getCallDepth() == 0) {
            var tree1 = new PrefixTree<Integer>();
            var tree2 = new PrefixTree<Integer>();
            targetPaths.put(cfg, new Pair<PrefixTree<Integer>, PrefixTree<Integer>>(tree1, tree2));
            var paths = pathGenerator.GeneratePaths(cfg);
            logger.debug("CFG: {}", cfg);
            logger.info("Added {} path targets", paths.size());
            for (var path: paths)
            {
                logger.debug("Added path: {}", path);
                var branchhistory = BranchHistory.ConvertPathToBranchHistory(path, cfg);
                // branchhistory could possibly generate duplicates, but the
                // prefixtree does not add duplicates so this is not a problem
                tree1.insert(branchhistory);
                tree2.insert(branchhistory);
            }
        }
        targetPaths.get(cfg).first().removeSublists(target.getBranchHistory());
        targets.add(target);
    }

    @Override
    public boolean requiresBranchHistoryData() {
        return true;
    }

    @Override
    // TODO: maybe let this return bool, so we can choose if we want to cover a test case
    public void generatedTestCase(SymbolicState state) {
        var paths = targetPaths.get(state.getCFG());
        logger.debug("Branchhistory: {}", state.getBranchHistory());
        logger.debug("Covered: {}", BranchHistory.HistoryToString(state));
        logger.debug("Covered depth: {}", state.getDepth());
        // Remove covered paths from the set of paths that still need to be tested
        if(!paths.second().removeSublists(state.getBranchHistory())){
            logger.warn("Generated test case doesn't cover any target path");
        } 
        else {
            logger.debug("Covered prime path");
        }
    }

    @Override
    public void remove(T target) {
        targets.remove(target);
    }

    @Override
    public T next() {
        // Only continue searching if there is an uncovered target path
        if (targetPathsEmpty()) {
            logger.info("All target paths covered");
            targets.clear();
            return null;
        }

        // First try to find a target path that hasn't been explored yet
        var nextState = nextUncoveredInState();
        if (nextState != null) {
            logger.debug("Returning next undiscovered state");
            return nextState;
        }

        // Else try to find a state that contains a target path that hasn't had a 
        // test case generated for it yet
        var nextState2 = nextUncoveredInTests();
        if (nextState2 != null) {
            logger.debug("Returning next uncovered test state");
            return nextState2;
        }

        // If no target paths matches any of the states fall back on BFS
        // Needed to find beginning of a target path
        // TODO: maybe replace this with more direct search strategy (similar to distance to uncovered heuristic)?
        if (targets.isEmpty()) {
            logger.info("Search space has been exhausted");
            return null;
        }
        else {
            return targets.remove();
        }
    }

    /** try to find a target path that hasn't been explored yet */
    private T nextUncoveredInState() {
        return nextUncoveredState(false);
    }

    /** try to find a state that contains a target path that doesn't have a test case yet */
    private T nextUncoveredInTests() {
        return nextUncoveredState(true);
    }

    private T nextUncoveredState(boolean tests) {
        var iterator = tests ? targets.iterator() : targets.descendingIterator();
        while (iterator.hasNext()) {
            var target = iterator.next();
            var paths = targetPaths.get(target.getCFG());
            if (paths == null) continue;
            var containsTarget = tests ? paths.second().containsSublist(target.getBranchHistory()) : paths.first().containsPrefix(target.getBranchHistory());
            // If the history matches with any of the target paths, take this state next
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

    /** Returns true if no more target paths are present */
    // TODO: rekening houden met constructors?
    private boolean targetPathsEmpty() {
        for (var entry: targetPaths.values()) {
            if(!entry.first().empty() || !entry.second().empty()) {
                return false;
            }
        }
        return true;
    }
}