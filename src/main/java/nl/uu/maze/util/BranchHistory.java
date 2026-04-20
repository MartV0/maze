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

    /** Convert a program path into branch history */
    public static ArrayList<Integer> ConvertPathToBranchHistory(List<Stmt> path, StmtGraph<?> cfg){
        var history = new ArrayList<Integer>();
        for (int i = 0; i < path.size(); i++) {
            var stmt = path.get(i);
            var successors = cfg.getAllSuccessors(stmt);
            if (successors.size() > 1 && i < path.size() - 1) {
                int branchIndex = ListUtils.IndexOf(successors, path.get(i+1));
                if (branchIndex == -1) throw new java.lang.Error("Next item from path not found in list of successors");
                history.add(ToBranchHistory(stmt, branchIndex));
            }
        }
        return history;
    }
    
    public static void LogHistory(SearchTarget state) {
        try {
            logger.info(GetPathFromBranchHistory(state.getBranchHistory(), state.getCFG(), state.getStmt()).toString());
        } catch (Exception e) {
            logger.error(e.toString());
        }
    }

    /** Converts branch history for a given CFG to a list */
    public static ArrayList<Stmt> GetPathFromBranchHistory(List<Integer> branch_history, StmtGraph<?> cfg, Stmt target) throws Exception {
        var path = new ArrayList<Stmt>();
        Stmt current_statement = cfg.getStartingStmt();
        path.add(current_statement);
        int i = 0;
        while (current_statement != null && (current_statement != target || i < branch_history.size())) {
            var successors = cfg.getAllSuccessors(current_statement);
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
        return path;
    }

    static Stmt findSuccesor(Stmt statement, int brachHistory, List<Stmt> successors) throws Exception {
        for (int i = 0; i < successors.size(); i++) {
            if (ToBranchHistory(statement, i) == brachHistory) {
                return successors.get(i);
            }
        }
        throw new Exception("No matching successor statement");
    }

    /** Converts a branch taken to an integer representation */
    public static int ToBranchHistory(Stmt branchStmt, int branchIndex) {
        return branchStmt.hashCode() + 31 * branchIndex;
    }
}
