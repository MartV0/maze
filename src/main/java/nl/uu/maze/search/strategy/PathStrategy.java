package nl.uu.maze.search.strategy;

import nl.uu.maze.search.SearchTarget;
import nl.uu.maze.execution.symbolic.SymbolicState;
import nl.uu.maze.search.strategy.PathGenerator.PathGenerator;
import nl.uu.maze.util.PrefixTree;
import nl.uu.maze.util.Pair;
import nl.uu.maze.analysis.CFGDistance;
import nl.uu.maze.search.strategy.ProbabilisticSearch;
import nl.uu.maze.search.heuristic.SearchHeuristic;
import nl.uu.maze.execution.concrete.ConcreteExecutor;
import nl.uu.maze.execution.symbolic.SymbolicStateValidator;
import nl.uu.maze.execution.symbolic.SymbolicExecutor;
import nl.uu.maze.analysis.JavaAnalyzer;
import nl.uu.maze.execution.symbolic.PathExecutor;

import java.util.Collection;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.HashMap;
import java.util.Iterator;
import java.util.function.Predicate;
import java.util.function.Function;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import sootup.core.graph.StmtGraph;
import sootup.core.jimple.common.stmt.Stmt;

public class PathStrategy<T extends SearchTarget> extends SearchStrategy<T> {
    public enum SearchOrder {
        BFS,
        DFS,
        Heuristic
    }

    private SearchOrder stringToOrder(String s) {
        s = s.toLowerCase();
        switch (s) {
            case "b", "bfs" -> { return SearchOrder.BFS; }
            case "d", "dfs" -> { return SearchOrder.DFS; }
            case "h", "heuristic" -> { return SearchOrder.Heuristic; }
        }
        throw new Error("Invalid search order");
    }

    private final static Logger logger = LoggerFactory.getLogger(PathStrategy.class);

    private final LinkedList<T> targets = new LinkedList<>();

    // Map from each cfg to all the target paths for that cfg
    // First element in pair is all target paths that are not yet covered by some state
    //  This is needed to know which states containing target path still need to be discovered
    // Second element in pair is all target paths that are not yet covered in some test
    //  This is needed in order to know which states we should keep exploring so they get covered
    //  in an actual test case
    private HashMap<StmtGraph<?>, Pair<PrefixTree<Stmt>, PrefixTree<Stmt>>> targetPaths = new HashMap<StmtGraph<?>, Pair<PrefixTree<Stmt>, PrefixTree<Stmt>>>();

    // Used to check feasibility of target paths
    SymbolicExecutor symbolicExecutor;
    // Used to generate any new target paths encountered
    PathGenerator pathGenerator;
    int maxDepth;
    SearchOrder pathExploration = SearchOrder.DFS;
    SearchOrder pathFinishing = SearchOrder.BFS;
    SearchOrder pathFinding = SearchOrder.BFS;

    public PathStrategy(PathGenerator pathGenerator, int maxDepth, List<String> args) {
        this.pathGenerator = pathGenerator;
        this.maxDepth = maxDepth;
        if (args.size() >= 3) {
            this.pathExploration = stringToOrder(args.get(0));
            this.pathFinishing = stringToOrder(args.get(1));
            this.pathFinding = stringToOrder(args.get(2));
        }
        logger.debug("pathExploration:{}", pathExploration);
        logger.debug("pathFinishing:{}", pathFinishing);
        logger.debug("pathFinishing:{}", pathFinding);
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
            // initialize symbolic executor if necessary
            if (this.symbolicExecutor == null) {
                ConcreteExecutor concrete = new ConcreteExecutor();
                SymbolicStateValidator validator = new SymbolicStateValidator();
                JavaAnalyzer analyzer = JavaAnalyzer.getInstance();
                this.symbolicExecutor = new SymbolicExecutor(concrete, validator, analyzer, false, false, false);
            }
            for (var path: paths)
            {
                // Check if path is feasible
                if (!PathExecutor.executePath(symbolicExecutor, path, target.getMethod())) {
                    logger.debug("Path is infeasible: {}", path);
                    continue;
                }
                tree1.insert(path);
                // We do not require every prime path in the constructor to be
                // covered, only for it to be discovered so other functions can
                // be tested using the state
                if (!target.isCtorState())
                    tree2.insert(path);
                logger.debug("Added path: {}", path);
            }
        }
        targets.add(target);
    }

    @Override
    public boolean requiresStatementHistoryData() {
        return true;
    }

    @Override
    public boolean generatedTestCase(SymbolicState state) {
        var paths = targetPaths.get(state.getCFG());
        logger.debug("Covered depth: {}", state.getDepth());
        // Remove covered paths from the set of paths that still need to be tested
        if(!paths.second().removeSublists(state.getStatementHistory())){
            logger.debug("Ignored: {}", state.getStatementHistory());
            logger.warn("Generated test case doesn't cover any target path");
            return false;
        } 
        else {
            logger.debug("Covered: {}", state.getStatementHistory());
            logger.debug("Covered prime path");
            return true;
        }
    }

    @Override
    public void remove(T target) {
        targets.remove(target);
    }

    @Override
    public T next() {
        if (targets.isEmpty()) {
            logger.info("Search space has been exhausted");
            return null;
        }

        // Only continue searching if there is an uncovered target path
        if (targetPathsEmpty()) {
            logger.info("All target paths covered");
            targets.clear();
            return null;
        }

        // First try to find a target path that hasn't been explored yet
        var nextState = nextUndiscoveredState();
        if (nextState != null) {
            logger.debug("Returning next undiscovered state");
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

        // If no target paths matches any of the states find the first state
        // from which a target path is reachable, in order to find the
        // beginning of a target path
        var nextState3 = nextStateReachingTargetPath();
        if (nextState3 != null) {
            logger.debug("Returning next state reaching target path");
            return nextState3;
        } else {
            logger.info("No state can reach a target path anymore, exiting");
            logger.info("{} Uncovered target paths", uncoveredPaths());
            // Make sure to clear targets, otherwise MAZE might still generate test cases for them
            targets.clear();
            return null;
        }
    }

    private int uncoveredPaths() {
        int total = 0;
        for (var paths: targetPaths.values()) {
            total += paths.second().size();
        }
        return total;
    }

    /** try to find a state from which a target path can be reached */
    private T nextStateReachingTargetPath() {
        if (pathFinding == SearchOrder.Heuristic) {
            return nextStateHeuristic(target -> {
                var undiscoveredFirstStmts = targetPaths.get(target.getCFG()).first().initialElements();
                return distanceToScore(target, stmt -> undiscoveredFirstStmts.contains(stmt));
            });
        }
        else {
            return nextState(pathFinding, target -> {
                // First statements of all the undiscovered paths
                var undiscoveredFirstStmts = targetPaths.get(target.getCFG()).first().initialElements();
                int maxDistance = maxDepth - target.getDepth();
                if (CFGDistance.calculateDistance(target, maxDistance, false, -1, stmt -> undiscoveredFirstStmts.contains(stmt)) != -1)
                    return true;
                return false;
            });
        }
    }

    /** try to find a state where the end of the history matches the beginning
     * of an undiscovered target path */
    private T nextUndiscoveredState() {
        return nextState(pathExploration, target -> {
            var paths = targetPaths.get(target.getCFG());
            if (paths == null) return false;
            return target.getCallDepth() == 0 && paths.first().containsPrefix(target.getStatementHistory());
        });
    }

    /** try to find a state that contains a target path as a subpath that
     * hasn't been covered in a test case yet */
    private T nextUncoveredInTests() {
        if (pathFinishing == SearchOrder.Heuristic) {
            // Heuristic that prioritises states which are closer to terminal states
            return nextStateHeuristic(
                target -> distanceToScore(
                    target,
                    // matches terminal statements
                    stmt -> target.getCFG().successors(stmt).size() == 0
                )
            );
        }
        else {
            return nextState(pathFinishing, target -> {
                var paths = targetPaths.get(target.getCFG());
                if (paths == null) return false;
                return target.getCallDepth() == 0 && paths.second().containsSublist(target.getStatementHistory());
            });
        }
    }

    /** Scores a target based on how close it it to a statement satisfying the predicate */
    private double distanceToScore(T target, Predicate<Stmt> predicate) {
        int maxDistance = maxDepth - target.getDepth();
        // Calculates the distance to first matching statement
        int distance = CFGDistance.calculateDistance(
            target, 
            maxDistance, 
            false, 
            -1,
            stmt -> predicate.test(stmt)
        );
        if (distance == -1) {
            return 0;
        }
        return SearchHeuristic.applyExponentialScaling(
            distance,
            0.1, 
            false
        );
    }

    /** Loops through targets and returns first one matching predicate
    * if order == BFS than the search starts from the start of the queue,
    * if order == DFS than the search starts from the end of the queue */
    private T nextState(SearchOrder order, Predicate<T> predicate) {
        Iterator<T> iterator;
        switch (order) {
            case SearchOrder.BFS:
                iterator = targets.iterator();
                break;
            case SearchOrder.DFS:
                iterator = targets.descendingIterator();
                break;
            default:
                throw new Error("nextState only works with BFS/DFS, not heuristic");
        }
        while (iterator.hasNext()) {
            var target = iterator.next();
            if (predicate.test(target)) {
                targets.remove(target);
                return target;
            }
        }
        return null;
    }

    private T nextStateHeuristic(Function<T, Double> weightFunction) {
        double weights[] = new double[targets.size()];
        double totalWeight = 0;
        for(int i = 0; i < targets.size(); i++) {
            double weight = weightFunction.apply(targets.get(i));
            weights[i] = weight;
            totalWeight += weight;
        }
        if (totalWeight > 0) {
            int index = ProbabilisticSearch.selectWeightedIndex(weights, totalWeight);
            return targets.remove(index);
        }
        else {
            return null;
        }
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