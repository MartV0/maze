package nl.uu.maze.execution.symbolic;

import java.util.List;
import java.util.LinkedList;

import com.microsoft.z3.*;
import sootup.core.jimple.basic.Local;
import sootup.core.jimple.common.ref.*;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.graph.StmtGraph;
import sootup.java.core.JavaSootMethod;

import nl.uu.maze.transform.JimpleToZ3Transformer;

/** Can execute a path in isolation */
public class PathExecutor {
    /** Symbolically executes the given path and returns whether or not the path is feasible */
    public static boolean executePath(SymbolicExecutor executor, List<Stmt> path, JavaSootMethod method) {
        // expand the path backward, thus adding more context to the path, which can 
        // help detect additional infeasible paths
        path = expandPathBackward(path, method);        
        SymbolicStateValidator validator = new SymbolicStateValidator();
        SymbolicState symbolicState = initializeState(path, method);
        return followPath(executor, path, 0, symbolicState, validator);
    }

    // Uses depht first search to follow the given path.
    // DFS is required because even within a given path the state can still fork
    // on aliases and array accesses.
    static boolean followPath(SymbolicExecutor executor, List<Stmt> path, int index, SymbolicState state, SymbolicStateValidator validator) {
        assert(path.get(index) == state.getStmt());
        List<SymbolicState> newStates = executor.step(state, false);
        // base case: check if the final stmt had any successors
        if (index == path.size() - 1) {
            assert newStates.stream().anyMatch(newState -> validator.validate(newState).isPresent()) == newStates.size() >= 1;
            return newStates.size() >= 1;
        }
        // recursive case: check if any of the successors follow the path to a feasible state
        return newStates.stream()
            .filter(newState -> !newState.isFinalState())
            .anyMatch(newState -> {
                Stmt stmt = newState.getStmt();
                // It is possible we are still on the same stmt after a execution
                // step when the state forks on an alias
                if (stmt == path.get(index)){
                    return followPath(executor, path, index, newState, validator);
                }
                else if (stmt == path.get(index + 1)){
                    return followPath(executor, path, index + 1, newState, validator);
                }
                return false;
            });
    }

    /** expands the given path backwards as long as there is only one predecessor */
    static List<Stmt> expandPathBackward(List<Stmt> path, JavaSootMethod method) {
        LinkedList<Stmt> newPath = new LinkedList<Stmt>(path);
        StmtGraph cfg = method.getBody().getStmtGraph();
        List<Stmt> preds;
        while ((preds = cfg.predecessors(newPath.get(0))).size() == 1) {
            newPath.addFirst(preds.get(0));
        }
        return newPath;
    }

    /** Creates a new symbolic state starting in the first statement of path and
      * initializing any variables that are used in the path */
    static SymbolicState initializeState(List<Stmt> path, JavaSootMethod method) {
        SymbolicState symbolicState = new SymbolicState(method, method.getBody().getStmtGraph(), path.get(0));
        symbolicState.switchToMethodState();
        for(Stmt stmt: path) {
            stmt.getUses().forEach(value -> {
                value.getUses().forEach(use -> {
                    String var = use.toString();
                    // parameters (JParameterRef) are initialized during execution
                    if (
                        (use instanceof JArrayRef ||
                        use instanceof JFieldRef ||
                        use instanceof JThisRef ||
                        use instanceof Local) &&
                        !symbolicState.exists(var)
                    )
                    {
                        Expr<?> expr = JimpleToZ3Transformer.newVariable(symbolicState, var, use.getType(), null);
                        symbolicState.assign(var, expr);
                    }
                });
            });
        }
        return symbolicState;
    }
}
