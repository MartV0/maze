package nl.uu.maze.search.strategy.PathGenerator;

import sootup.core.graph.BasicBlock;
import sootup.core.graph.StmtGraph;
import sootup.core.model.SootMethod;
import java.util.ArrayList;
import java.util.List;
import nl.uu.maze.search.strategy.PathGenerator.PathGenerator;

import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.jimple.common.stmt.JIfStmt;
import sootup.core.jimple.javabytecode.stmt.JSwitchStmt;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class PrimePathGenerator implements PathGenerator {
    private final static Logger logger = LoggerFactory.getLogger(PrimePathGenerator.class);

    /** Generate all prime paths in a CFG */
    public <V extends BasicBlock<V>> ArrayList<ArrayList<Stmt>> GeneratePaths(StmtGraph<V> cfg){
        var paths = new ArrayList<ArrayList<Stmt>>();
        // initialize path list
        for (var node: cfg.getNodes()) {
            var path = new ArrayList<Stmt>();
            path.add(node);
            paths.add(path);
        }

        var done = false;
        while (!done) {
            done = true;
            var new_paths = new ArrayList<ArrayList<Stmt>>();
            // expand paths
            for (var path: paths) {
                var new_added = extend_path(path, new_paths, cfg);
                if(!new_added) {
                    new_paths.add(path);
                } else {
                    done = false;
                }
            }
            paths = new_paths;
        }

        PathGenerator.remove_postfixes(paths);

        return paths;
    }

    public String getName() {
        return "PrimePath";
    }

    /** Extend path by following outgoing edges, adds the new paths into buffer
      * returns true iff new paths were added */
    static <V extends BasicBlock<V>> boolean extend_path(ArrayList<Stmt> path, ArrayList<ArrayList<Stmt>> buffer, StmtGraph<V> cfg) {
        var added_new_paths = false;
        var lastNode = path.getLast();
        // successors is better than getAllSuccessors, because getAll also includes exceptional flow
        List<Stmt> successors = cfg.successors(lastNode);
        if (successors.size() > 1 && !(lastNode instanceof JIfStmt) && !(lastNode instanceof JSwitchStmt)) {
            logger.error("None if or switch statement with more than one successor {}", lastNode);
            throw new RuntimeException("None if or switch statement with more than one successor");
        }
        for (Stmt successor: successors) {
            if (can_add_node(path, successor)) {
                var path_copy = new ArrayList<Stmt>(path);
                path_copy.add(successor);
                buffer.add(path_copy);
                added_new_paths = true;
            }
        }
        return added_new_paths;
    }

    /** checks if path still remains a simple path or loop after adding a new node
    * assumes path is currently simple path or loop already */
    static boolean can_add_node(ArrayList<Stmt> path, Stmt new_node) {
        // path is simple loop, can't add anything else to it
        if (path.size() > 1 && path.get(0) == path.getLast()) {
            return false;
        }
        // start at second element, because first element is allowed to be the same as that would be a simple loop
        for (int i = 1; i < path.size(); i++) {
            if (path.get(i) == new_node) return false;
        }
        return true;
    }
}
