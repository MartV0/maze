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
        for (int i = 1; i < path.size(); i++) {
            List<SymbolicState> newStates = executor.step(symbolicState, false);
            boolean foundNext = false;
            // try to find a successor state which matches the next statement in the path
            for (int j = 0; j < newStates.size(); j++) {
                if (newStates.get(j).getStmt() == path.get(i)) {
                    symbolicState = newStates.get(j);
                    foundNext = true;
                    break;
                }
            }
            if (!foundNext) {
                return false;
            }
        }
        // Execute final stmt in path
        List<SymbolicState> newStates = executor.step(symbolicState, false);
        assert newStates.stream().anyMatch(state -> validator.validate(state).isPresent()) == newStates.size() >= 1;
        return newStates.size() >= 1;
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
