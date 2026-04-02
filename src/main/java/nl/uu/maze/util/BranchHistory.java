package nl.uu.maze.util;

import sootup.core.graph.StmtGraph;
import sootup.core.jimple.common.stmt.Stmt;
import java.util.ArrayList;
import java.util.List;

public class BranchHistory {
    /** Convert a program path into branch history */
    public static ArrayList<Integer> ConvertPathToBranchHistory(
List<Stmt> path, StmtGraph<?> cfg){
        var history = new ArrayList<Integer>();
        for (int i = 0; i < path.size(); i++) {
            var stmt = path.get(i);
            var successors = cfg.getAllSuccessors(stmt);
            if (successors.size() > 1) {
                int branchIndex = ListUtils.IndexOf(successors, path.get(i+1));
                if (branchIndex == -1) throw new java.lang.Error("Next item from path not found in list of successors");
                history.add(ToBranchHistory(stmt, branchIndex));
            }
        }
        return history;
    }

    /** Converts a branch taken to an integer representation */
    public static int ToBranchHistory(Stmt branchStmt, int branchIndex) {
        return branchStmt.hashCode() + 31 * branchIndex;
    }
}
