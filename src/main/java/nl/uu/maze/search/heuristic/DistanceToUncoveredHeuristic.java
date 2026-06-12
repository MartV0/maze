package nl.uu.maze.search.heuristic;

import nl.uu.maze.execution.symbolic.CoverageTracker;
import nl.uu.maze.search.SearchTarget;
import nl.uu.maze.analysis.CFGDistance;

/**
 * Distance To Uncovered Heuristic (DTUH)
 * <p>
 * Assigns weights based on how close a target is to reaching uncovered code.
 * Targets that are fewer steps away from uncovered statements receive higher
 * priority, guiding the search toward unexplored regions of the program.
 */
public class DistanceToUncoveredHeuristic extends SearchHeuristic {
    /**
     * Maximum distance to uncovered for a target, used to limit the search and for
     * targets that cannot reach any uncovered statement.
     */
    private static final int MAX_DISTANCE = 100;
    private static final CoverageTracker coverageTracker = CoverageTracker.getInstance();

    public DistanceToUncoveredHeuristic(double weight) {
        super(weight);
    }

    @Override
    public String getName() {
        return "DistanceToUncoveredHeuristic";
    }

    @Override
    public boolean requiresCoverageData() {
        return true;
    }

    @Override
    public <T extends SearchTarget> double calculateWeight(T target) {
        return applyExponentialScaling(
            CFGDistance.calculateDistance(
                target, 
                MAX_DISTANCE, 
                true, 
                MAX_DISTANCE,
                stmt -> !coverageTracker.isCovered(stmt)
            ),
            0.1, 
            false
        );
    }
}
