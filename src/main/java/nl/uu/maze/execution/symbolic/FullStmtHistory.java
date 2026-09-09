package nl.uu.maze.execution.symbolic;

import java.util.ArrayList;
import java.util.List;
import sootup.core.jimple.common.stmt.Stmt;

/** Full stmt history that also includes the historys of any function calls made
 * and the history of those function calls etc.
 */
public class FullStmtHistory {
    List<Stmt> history;
    List<FullStmtHistory> callHistory;

    public FullStmtHistory() {
        history = new ArrayList<Stmt>();
        callHistory = new ArrayList<FullStmtHistory>();
    }

    /** Constructor that creates a shallow copy of the given history */
    public FullStmtHistory(FullStmtHistory oldHistory) {
        this.history = new ArrayList<Stmt>(oldHistory.history);
        // A full deep copy is not needed as only previous function calls will not be changed
        callHistory = new ArrayList<FullStmtHistory>(oldHistory.callHistory);
    }

    /** Add a FullStmtHistory to the call history*/
    void addCallHistory(FullStmtHistory history) {
        callHistory.add(history);
    }

    /** Record a stmt in the current history */
    void addStmt(Stmt stmt) {
        history.add(stmt);
    }

    /** Returns true iff history of the current function is empty */
    boolean currentHistoryEmpty() {
        return history.size() == 0;
    }
}
