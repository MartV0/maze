package nl.uu.maze.search.strategy;

import nl.uu.maze.search.SearchTarget;
import nl.uu.maze.search.strategy.PrimePath.PrimePathGenerator;
import nl.uu.maze.util.BranchHistory;
import nl.uu.maze.util.PrefixTree;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;
import java.util.HashSet;
import sootup.core.graph.StmtGraph;

public class PrimePathStrategy<T extends SearchTarget> extends SearchStrategy<T> {
    private final Queue<T> targets = new LinkedList<>();
    // All cfgs encountered during the search, for every cfg we generate prime paths
    private HashSet<StmtGraph<?>> addedCFGs = new HashSet<StmtGraph<?>>();
    // PrexifTree so we can efficiently query if any branch history matches a prime path
    private PrefixTree<Integer> primePaths = new PrefixTree<Integer>();

    public String getName() {
        return "PrimePathStrategy";
    }

    @Override
    public void add(T target) {
        var cfg = target.getCFG();
        // if it is the first time seeing this cfg, generate prime paths for it
        if (!addedCFGs.contains(cfg)) {
            addedCFGs.add(cfg);
            for (var path: PrimePathGenerator.GeneratePaths(cfg))
            {
                var branchhistory = BranchHistory.ConvertPathToBranchHistory(path, cfg);
                // branchhistory could possibly generate duplicates, but the
                // prefixtree does not add duplicates so this is not a problem
                primePaths.insert(branchhistory);
            }
        }
        targets.add(target);
    }

    @Override
    public boolean requiresBranchHistoryData() {
        return true;
    }

    @Override
    public void remove(T target) {
        targets.remove(target);
    }

    @Override
    public T next() {
        // TODO: Maybe do this in reverse order? that way new prime paths get fully discovered before moving on to other targets
        for (var target: targets) {
            // If the history matches with any of the goal prime paths, take this state next
            if (primePaths.containsPrefix(target.getBranchHistory())) {
                targets.remove(target);
                return target;
            }
        }
        // If no prime paths matches any of the states fall back on BFS
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
}