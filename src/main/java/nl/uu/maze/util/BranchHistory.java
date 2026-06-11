package nl.uu.maze.util;

import nl.uu.maze.search.SearchTarget;
import sootup.core.graph.StmtGraph;
import sootup.core.jimple.common.stmt.Stmt;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class BranchHistory {
    private final static Logger logger = LoggerFactory.getLogger(BranchHistory.class);

    /** Convert a program path into branch history
     * Includes branch that was needed to arrive at the path, if unambigiuous.
     * For example if we have: if (a) b(); if (c) d();
     * And we have the path b(); if (c) d(); we return [if(true), if(true)]
     * Even though the first if wasn't in the path, but it is needed to reach b()
     * Sometimes a statement can have multiple predecessors, these*/
    public static ArrayList<Integer> ConvertPathToBranchHistory(List<Stmt> path, StmtGraph<?> cfg){
        var history = new ArrayList<Integer>();
        Integer preceding;
        if (cfg.successors(path.get(0)).size() <= 1 
            && (preceding = FindFirstPrecedingBranch(path.get(0), cfg)) != null) {
            history.add(preceding);
        }
        for (int i = 0; i < path.size(); i++) {
            var stmt = path.get(i);
            var successors = cfg.successors(stmt);
            if (successors.size() > 1 && i < path.size() - 1) {
                int branchIndex = ListUtils.IndexOf(successors, path.get(i+1));
                if (branchIndex == -1) throw new java.lang.Error("Next item from path not found in list of successors");
                history.add(ToBranchHistory(stmt, branchIndex));
            }
        }
        return history;
    }

    private static Integer FindFirstPrecedingBranch(Stmt stmt, StmtGraph<?> cfg) {
        Stmt current = stmt;
        Stmt previous = null;
        while (cfg.successors(current).size() <= 1) {
            var preds = cfg.predecessors(current);
            if (preds.size() == 1) {
                previous = current;
                current = preds.get(0);
            }
            else {
                // Either no predecessors, or multiple which make it ambiguous, so we return null
                return null;
            }
        }
        return ToBranchHistory(current, ListUtils.IndexOf(cfg.successors(current), previous));
    }
    
    public static String HistoryToString(SearchTarget state) {
        return GetPathFromBranchHistory(state.getBranchHistory(), state.getCFG(), state.getStmt()).toString();
    }

    /** Converts branch history for a given CFG to a list of statements */
    public static ArrayList<Stmt> GetPathFromBranchHistory(List<Integer> branch_history, StmtGraph<?> cfg, Stmt target) {
        var path = new ArrayList<Stmt>();
        Stmt current_statement = cfg.getStartingStmt();
        path.add(current_statement);
        int i = 0;
        while (current_statement != null && (current_statement != target || i < branch_history.size())) {
            var successors = cfg.successors(current_statement);
            switch (successors.size()) {
                case 0:
                    return path;
                case 1:
                    current_statement = successors.get(0);
                    break;
                default:
                    if (i >= branch_history.size()) return path;
                    current_statement = findSuccesor(current_statement, branch_history.get(i++), successors);
                    break;
            }
            path.add(current_statement);
        }
        assert(path.getLast() == target);
        return path;
    }

    static Stmt findSuccesor(Stmt statement, int brachHistory, List<Stmt> successors) {
        for (int i = 0; i < successors.size(); i++) {
            if (ToBranchHistory(statement, i) == brachHistory) {
                return successors.get(i);
            }
        }
        throw new Error("No matching successor statement");
    }

    /** Converts a branch taken to an integer representation */
    public static int ToBranchHistory(Stmt branchStmt, int branchIndex) {
        return branchStmt.hashCode() + 31 * branchIndex;
    }
}
