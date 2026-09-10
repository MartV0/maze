package nl.uu.maze.execution.symbolic;

import java.util.ArrayList;
import java.util.List;
import nl.uu.maze.util.Pair;
import sootup.core.graph.StmtGraph;
import sootup.core.jimple.common.stmt.Stmt;

/** Full stmt history that also includes the historys of any function calls made
 * and the history of those function calls etc.
 */
public class FullStmtHistory {
    private List<Stmt> history;
    private List<FullStmtHistory> callHistory;
    private StmtGraph<?> cfg;

    public FullStmtHistory(StmtGraph<?> cfg) {
        history = new ArrayList<Stmt>();
        callHistory = new ArrayList<FullStmtHistory>();
        this.cfg = cfg;
    }

    /** Constructor that creates a shallow copy of the given history */
    public FullStmtHistory(FullStmtHistory oldHistory) {
        this.history = new ArrayList<Stmt>(oldHistory.history);
        // A full deep copy is not needed as only previous function calls will not be changed
        callHistory = new ArrayList<FullStmtHistory>(oldHistory.callHistory);
        this.cfg = oldHistory.cfg;
    }

    /** Add a FullStmtHistory to the call history*/
    public void addCallHistory(FullStmtHistory history) {
        callHistory.add(history);
    }

    /** Record a stmt in the current history */
    public void addStmt(Stmt stmt) {
        history.add(stmt);
    }

    /** Returns true iff history of the current function is empty */
    public boolean currentHistoryEmpty() {
        return history.size() == 0;
    }

    /** Set the cfg of the current history */
    public void setCFG(StmtGraph<?> cfg) {
        this.cfg = cfg;
    }

    /** Accumulates all stmt history into a list */
    public List<Pair<List<Stmt>, StmtGraph>> getAllHistorys() {
        List<Pair<List<Stmt>, StmtGraph>> historys = new ArrayList<Pair<List<Stmt>, StmtGraph>>();
        getAllHistorys(historys);
        return historys;
    }

    public List<Stmt> getCurrentHistory() {
        return history;
    }

    private void getAllHistorys(List<Pair<List<Stmt>, StmtGraph>> historys) {
        Pair<List<Stmt>, StmtGraph> history = new Pair<List<Stmt>, StmtGraph>(this.history, cfg);
        historys.add(history);
        for (var call: callHistory) {
            call.getAllHistorys();
        }
    }

}
