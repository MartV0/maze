package nl.uu.maze.search.strategy.PathGenerator;

import sootup.core.graph.BasicBlock;
import sootup.core.graph.StmtGraph;
import sootup.core.model.SootMethod;
import java.util.ArrayList;
import java.util.List;
import nl.uu.maze.search.strategy.PathGenerator.PathGenerator;
import nl.uu.maze.search.strategy.PathGenerator.PrimePathGenerator;

import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.jimple.common.stmt.JIfStmt;
import sootup.core.jimple.javabytecode.stmt.JSwitchStmt;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class EdgePairGenerator implements PathGenerator {
    /** Generate all edge pairs in a CFG
     * We are using a slightly different definition of edge pair, where we 
     * ignore any nodes with indegree <= 2, essentially generating all 
     * combinations of adjacent branches.
     * This was used so that nodes not affecting the control flow in any way
     * effect the combination of control flow covered. */
    public <V extends BasicBlock<V>> ArrayList<ArrayList<Stmt>> GeneratePaths(StmtGraph<V> cfg){
        ArrayList<ArrayList<Stmt>> paths = new ArrayList<ArrayList<Stmt>>();
        for (Stmt stmt: cfg.getNodes()) {
            AddEdgePair(stmt, paths, cfg);
        }
        // Remove any overlapping paths
        PathGenerator.remove_postfixes(paths);
        return paths;
    }

    /** add all edge pairs starting in stmt */
    private static void AddEdgePair(Stmt stmt, ArrayList<ArrayList<Stmt>> pairs, StmtGraph<?> cfg) {
        ArrayList<Stmt> newPath = new ArrayList<Stmt>();
        newPath.add(stmt);
        // Get the first successors of stmt, so the first outgoing edges
        var successors = cfg.successors(stmt);
        for (Stmt succ: successors) {
            ArrayList<Stmt> newPath2 = new ArrayList<Stmt>(newPath);
            newPath2.add(succ);
            var succs2 = cfg.successors(succ);
            // Keep expanding the path until a node with outdegree >= 2 is found this
            // way stmts without any control flow do not effect the edge pair generation
            while (succs2.size() == 1) {
                succ = succs2.get(0);
                newPath2.add(succ);
                succs2 = cfg.successors(succ);
            }
            if (succs2.size() == 0) {
                pairs.add(newPath2);
            }
            else {
                for (Stmt succ3: succs2) {
                    ArrayList<Stmt> newPath3 = new ArrayList<Stmt>(newPath2);
                    newPath3.add(succ3);
                    pairs.add(newPath3);
                }
            }
        }
    }

    public String getName() {
        return "Edge-Pair";
    }
}
