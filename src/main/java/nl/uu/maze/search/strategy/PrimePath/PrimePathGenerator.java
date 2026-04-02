package nl.uu.maze.search.strategy.PrimePath;

import sootup.core.graph.BasicBlock;
import sootup.core.graph.StmtGraph;
import sootup.core.model.SootMethod;
import java.util.ArrayList;
import java.util.List;

import sootup.core.jimple.common.stmt.Stmt;

public class PrimePathGenerator {
    /** Generate all prime paths in a CFG */
    public static <V extends BasicBlock<V>> ArrayList<ArrayList<Stmt>> GeneratePaths(StmtGraph<V> cfg){
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

        for (int i = 0; i < paths.size(); i++) {
            for (int j = 0; j < paths.size(); j++) {
                if (i == j) continue;
                // TODO: replace with postfix tree
                if (is_postfix(paths.get(i), paths.get(j))) {
                    paths.remove(i);
                    i--;
                    break;
                }
            }
        }

        return paths;
    }

    /** Extend path by following outgoing edges, adds the new paths into buffer
      * returns true iff new paths were added */
    static <V extends BasicBlock<V>> boolean extend_path(ArrayList<Stmt> path, ArrayList<ArrayList<Stmt>> buffer, StmtGraph<V> cfg) {
        var added_new_paths = false;
        List<Stmt> successors = cfg.getAllSuccessors(path.getLast());
        for (Stmt successor: successors) {
            if (can_add_node(path, successor)){
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
        // start at second element, because first element is allowed to be the same as that would be a simple loop
        for (int i = 1; i < path.size(); i++) {
            if (path.get(i) == new_node) return false;
        }
        return true;
    }

    /** returns true iff path1 is a postfix of path2 */
    static boolean is_postfix (ArrayList<Stmt> path1, ArrayList<Stmt> path2) {
        if (path1.size() > path2.size()) return false;
        for (int i = 0; i < path1.size(); i++) {
            var i1 = path1.size() - i - 1;
            var i2 = path2.size() - i - 1;
            if (path1.get(i1) != path2.get(i2)) return false;
        }

        return true;
    }
}
