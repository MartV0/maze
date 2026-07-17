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
            basisSets.put(cfg, new BasisSet(cfg));
        }
        targets.add(target);
    }

    @Override
    public void remove(T target) {
        targets.remove(target);
    }

    @Override
    public T next() {
        if (basisSetsComplete()) return null;
        // First try to find a state with an uncovered branch in the history
        for (T target: targets) {
            var basisset = basisSets.get(target.getCFG());
            if (basisset.containsUncoveredBranch(target.getBranchHistory())) {
                targets.remove(target);
                logger.debug("returning uncovered state");
                return target;
            }
        }
        // Second try to find a state that can reach an uncovered branch
        for (T target: targets) {
            var basisset = basisSets.get(target.getCFG());
            if (basisset.canReachUncoveredBranch(target, maxDepth)) {
                targets.remove(target);
                logger.debug("returning reachable state");
                return target;
            }
        }
        logger.debug("returning next state");
        // If no such states are found, return first target
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
        // TODO: only return potentially independent paths?
        return targets;
    }

    @Override
    public boolean requiresBranchHistoryData() {
        return true;
    }

    @Override
    public boolean generatedTestCase(SymbolicState state) {
        BasisSet set = basisSets.get(state.getCFG());
        boolean added = set.addPath(state.getBranchHistory());
        if (added) logger.debug("Covered: {}", BranchHistory.HistoryToString(state));
        else logger.debug("Ignored: {}", BranchHistory.HistoryToString(state));
        return added;
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
            return CFGDistance.calculateDistance(state, maxDistance, false, -1, stmt -> statementUncovered(stmt, state.getCFG())) != -1;
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
            int columns = basisSet.size() + 1;
            int rows = branches.size();
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
            return basisSet.size() == cyclomaticComplexity;
        }
    }
}
