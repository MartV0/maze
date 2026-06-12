package nl.uu.maze.search.strategy;

import nl.uu.maze.search.SearchTarget;
import nl.uu.maze.execution.symbolic.SymbolicState;
import nl.uu.maze.search.strategy.PathGenerator.PathGenerator;
import nl.uu.maze.util.PrefixTree;
import nl.uu.maze.util.Pair;

import java.util.Collection;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Queue;
import java.util.HashMap;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import sootup.core.graph.StmtGraph;
import sootup.core.jimple.common.stmt.Stmt;

public class PathStrategy<T extends SearchTarget> extends SearchStrategy<T> {
    private final static Logger logger = LoggerFactory.getLogger(PathStrategy.class);

    private final LinkedList<T> targets = new LinkedList<>();

    // Map from each cfg to all the target paths for that cfg
    // First element in pair is all target paths that are not yet covered by some state
    //  This is needed to know which states containing target path still need to be discovered
    // Second element in pair is all target paths that are not yet covered in some test
    //  This is needed in order to know which states we should keep exploring so they get covered
    //  in an actual test case
    private HashMap<StmtGraph<?>, Pair<PrefixTree<Stmt>, PrefixTree<Stmt>>> targetPaths = new HashMap<StmtGraph<?>, Pair<PrefixTree<Stmt>, PrefixTree<Stmt>>>();

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
            var tree1 = new PrefixTree<Stmt>();
            var tree2 = new PrefixTree<Stmt>();
            targetPaths.put(cfg, new Pair<PrefixTree<Stmt>, PrefixTree<Stmt>>(tree1, tree2));
            var paths = pathGenerator.GeneratePaths(cfg);
            logger.debug("CFG: {}", cfg);
            logger.info("Added {} path targets", paths.size());
            for (var path: paths)
            {
                logger.debug("Added path: {}", path);
                tree1.insert(path);
                // We do not require every prime path in the constructor to be
                // covered, only for it to be discovered so other functions can
                // be tested using the state
                if (!target.isCtorState())
                    tree2.insert(path);
            }
        }
        targets.add(target);
    }

    @Override
    public boolean requiresStatementHistoryData() {
        return true;
    }

    @Override
    // TODO: maybe let this return bool, so we can choose if we want to cover a test case
    public void generatedTestCase(SymbolicState state) {
        var paths = targetPaths.get(state.getCFG());
        logger.debug("Covered: {}", state.getStatementHistory());
        logger.debug("Covered depth: {}", state.getDepth());
        // Remove covered paths from the set of paths that still need to be tested
        if(!paths.second().removeSublists(state.getStatementHistory())){
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
            // TODO: this is a bit inefficient
            // Copy the history and add the current statement to it so the history is complete
            var completeHistory = new ArrayList<Stmt>(nextState.getStatementHistory());
            completeHistory.add(nextState.getStmt());
            targetPaths.get(nextState.getCFG()).first().removeSublists(completeHistory);
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
            var containsTarget = tests ? paths.second().containsSublist(target.getStatementHistory()) : paths.first().containsPrefix(target.getStatementHistory());
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
    private boolean targetPathsEmpty() {
        for (var entry: targetPaths.values()) {
            if(!entry.first().empty() || !entry.second().empty()) {
                return false;
            }
        }
        return true;
    }
}