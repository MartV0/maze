package nl.uu.maze.search.strategy;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import nl.uu.maze.util.BranchHistory;
import nl.uu.maze.execution.symbolic.SymbolicState;
import Jama.Matrix;
import java.util.HashMap;
import sootup.core.graph.StmtGraph;
import sootup.core.jimple.common.stmt.Stmt;
import nl.uu.maze.util.Pair;
import nl.uu.maze.analysis.CFGDistance;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import nl.uu.maze.search.SearchTarget;

/**
 *  Basis Path strategy. Automatically generates a set of paths that form a basis
 *  set. When program paths are expressed as vectors, any path vector can be 
 *  expressed as a linear combination of vectors in the basis set.
 */
public class BasisPathStrategy<T extends SearchTarget> extends SearchStrategy<T> {
    private final static Logger logger = LoggerFactory.getLogger(BasisPathStrategy.class);

    private final Queue<T> targets = new LinkedList<>();

    private HashMap<StmtGraph<?>, BasisSet> basisSets = new HashMap<StmtGraph<?>, BasisSet>();

    int maxDepth;

    public String getName() {
        return "BasisPathStrategy";
    }

    public BasisPathStrategy(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    @Override
    public void add(T target) {
        var cfg = target.getCFG();
        if (!basisSets.containsKey(cfg)) {
            logger.debug("Added: {}", target.getCFG());
            BasisSet basis = new BasisSet(cfg);
            logger.info("Added new target with cyclomatic complexity {}", basis.cyclomaticComplexity);
            basisSets.put(cfg, basis);
        }
        targets.add(target);
    }

    @Override
    public void remove(T target) {
        targets.remove(target);
    }

    @Override
    public T next() {
        if (basisSetsComplete()) {
            logger.info("Basis coverage achieved");
            targets.clear();
            return null;
        }
        // First try to find a state with an uncovered branch in the history
        for (T target: targets) {
            if (stateContainsUncoveredBranch(target)) {
                targets.remove(target);
                logger.trace("returning uncovered state");
                return target;
            }
        }
        // Second try to find a state that can reach an uncovered branch
        for (T target: targets) {
            if (stateReachesUncoveredBranch(target)) {
                targets.remove(target);
                logger.trace("returning reachable state");
                return target;
            }
        }
        logger.debug("returning next state");
        // If no such states are found, return first target
        if (targets.isEmpty()) {
            logger.info("Search space exhausted");
            return null;
        } else {
            return targets.remove();
        }
    }

    /// True iff history contains an uncovered branch
    private boolean stateContainsUncoveredBranch(T target) {
        var currentHistory = target.getFullStatementHistory().getCurrentHistory();
        for (var frame: target.getCallStack()) {
            for (var history: frame.getFullStatementHistory().getAllHistorys()) {
                var stmtHistory = history.first();
                // if we are at the history of the current target make sure to add
                // the current statement to have a complete history
                if (currentHistory == stmtHistory) {
                    stmtHistory = new ArrayList<Stmt>(stmtHistory);
                    stmtHistory.add(target.getStmt());
                }
                List<Integer> branchhistory = BranchHistory.ConvertPathToBranchHistory(stmtHistory, history.second());
                if (basisSets.get(history.second()).containsUncoveredBranch(branchhistory)) {
                    return true;
                }
            }
        }
        return false;
    }

    /// True iff state can reach an uncovered from here
    private boolean stateReachesUncoveredBranch(T target) {
        int maxDistance = maxDepth - target.getDepth();
        int distance = CFGDistance.calculateDistance(target, maxDistance, false, -1, (stmt, cfg) -> {
            var basisSet = basisSets.get(cfg);
            if (basisSet == null) return false;
            return basisSet.statementUncovered(stmt, cfg);
        });
        return distance != -1;
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
        // TODO: only return potentially independent paths?
        return targets;
    }

    @Override
    public boolean requiresFullStatementHistoryData() {
        return true;
    }

    @Override
    public boolean generatedTestCase(SymbolicState state) {
        var historys = state.getFullStatementHistory().getAllHistorys();
        boolean coverage = false;
        for (var history: historys) {
            BasisSet set = basisSets.get(history.second());
            if(set != null && set.addPath(BranchHistory.ConvertPathToBranchHistory(history.first(), history.second()))){
                coverage = true;
            }
        }
        if (coverage) logger.debug("Covered: {}", BranchHistory.HistoryToString(state));
        else logger.debug("Ignored: {}", BranchHistory.HistoryToString(state));
        return coverage;
    }

    private static int calculateCyclomaticComplexity(StmtGraph<?> cfg) {
        var nodes = cfg.getNodes();
        int N = nodes.size();
        int E = 0;
        for (var node: nodes) {
            E += cfg.successors(node).size();
        }
        // cyclomatic complexity needs to be calculated on a connected graph
        // To make the graph connected we add a imaginary edge from every return
        // statement to the first statement
        E += cfg.getTails().size();
        // P = 1 because the graph is connected
        return E - N + 1;
    }

    /// Tests whether all the basissets are complete
    private boolean basisSetsComplete() {
        for (var set: basisSets.values()) {
            if (!set.isComplete()) return false;
        }
        return true;
    }

    @Override
    public void executionFinished() {
        int missing = 0;
        for (var set: basisSets.values()) {
            missing += set.cyclomaticComplexity - set.basisSet.size();
        }
        logger.info("{} too few independent paths in basis sets", missing);
    }

    class BasisSet {
        int cyclomaticComplexity;
        // determines which columns represent which branches when representing a path as a vector
        List<Integer> branches = new ArrayList<Integer>();
        List<List<Integer>> basisSet = new ArrayList<List<Integer>>();
        private final static Logger logger = LoggerFactory.getLogger(BasisPathStrategy.class);

        public BasisSet(StmtGraph<?> cfg) {
            this.cyclomaticComplexity = calculateCyclomaticComplexity(cfg);
            logger.debug("cyclomaticComplexity: {}", cyclomaticComplexity);
            var nodes = cfg.getNodes();
            branches = new ArrayList<Integer>();
            for (var node: nodes) {
                var successors = cfg.successors(node);
                if (successors.size() > 1) {
                    for (int i = 0; i < successors.size(); i++) {
                        branches.add(BranchHistory.ToBranchHistory(node, i));
                    }
                }
            }
            logger.debug("branches: {}", branches.size());
        }

        /// Whether branchhistory contains an branch that is uncoverd in the basisSet
        public boolean containsUncoveredBranch(Collection<Integer> branchHistory) {
            for (int i = 0; i < branches.size(); i++) {
                if (branchHistory.contains(branches.get(i))) {
                    if (isBranchUncovered(i))
                        return true;
                }
            }
            return false;
        }

        /// Whether state can reach any branch that is still uncovered in the basis set
        public boolean canReachUncoveredBranch(T state, int maxDepth) {
            int maxDistance = maxDepth - state.getDepth();
            return CFGDistance.calculateDistance(state, maxDistance, false, -1, (stmt, cfg) -> statementUncovered(stmt, cfg)) != -1;
        }

        /// Whether the statement contains an uncovered branch
        boolean statementUncovered(Stmt statement, StmtGraph cfg) {
            var successors = cfg.successors(statement);
            if (successors.size() > 1) {
                for (int i = 0; i < successors.size(); i++) {
                    int branch = BranchHistory.ToBranchHistory(statement, i);
                    if (isBranchUncovered(branches.indexOf(branch))){
                        return true;
                    }
                }
            }
            return false;
        }

        /// Checks if branch at index is uncovered in the basisset
        boolean isBranchUncovered(int branchIndex) {
            for (var pathVector: basisSet) {
                if (pathVector.get(branchIndex) > 0) {
                    return false;
                }
            }
            return true;
        }

        /// add path to basis set if it is linearly independent
        public boolean addPath(List<Integer> branchHistory) {
            // create a matrix where every column corresponds to a path vector
            int rows = branches.size();
            if (rows == 0) return false;
            int columns = basisSet.size() + 1;
            double matrix[][] = new double[rows][columns];
            intoMatrix(matrix);
            List<Integer> newVector = new ArrayList<Integer>();
            // add last column, which is the path to be added
            for (int i = 0; i < rows; i++) {
                int branch = branches.get(i);
                // how much this branch occured
                int branchCount = (int) branchHistory.stream().filter(b -> b == branch).count();
                newVector.add(branchCount);
                matrix[i][columns-1] = branchCount;
            }

            Matrix set = new Matrix(matrix);
            boolean independent = set.rank() == columns;
            if (independent) {
                basisSet.add(newVector);
                logger.info("Found new independent vector");
                logger.debug("Added vector {}", newVector);
            }
            return independent;
        }

        // Reads the path vectors into the columns of the given 2d array
        public void intoMatrix(double matrix[][]) {
            for (int i = 0; i < branches.size(); i++) {
                for (int j = 0; j < basisSet.size(); j++) {
                    matrix[i][j] = basisSet.get(j).get(i);
                }
            }
        }

        // checks whether current basisset is complete
        public boolean isComplete() {
            return basisSet.size() >= cyclomaticComplexity;
        }
    }
}
