package nl.uu.maze.search.strategy.PathGenerator;

import sootup.core.graph.BasicBlock;
import sootup.core.graph.StmtGraph;
import sootup.core.model.SootMethod;
import java.util.ArrayList;
import java.util.List;
import nl.uu.maze.search.strategy.PathGenerator.PathGenerator;

import sootup.core.jimple.common.stmt.Stmt;

/** PathGenerator interface generates required path based on a input CFG, used by PathStrategy*/
public interface PathGenerator {
    public <V extends BasicBlock<V>> ArrayList<ArrayList<Stmt>> GeneratePaths(StmtGraph<V> cfg);
    public String getName();

    /** Removes any paths from paths that are postfixes of another path */
    public static void remove_postfixes(ArrayList<ArrayList<Stmt>> paths) {
        for (int i = 0; i < paths.size(); i++) {
            for (int j = 0; j < paths.size(); j++) {
                if (i == j) continue;
                if (is_postfix(paths.get(i), paths.get(j))) {
                    paths.remove(i);
                    i--;
                    break;
                }
            }
        }
    }

    /** returns true iff path1 is a postfix of path2 */
    private static boolean is_postfix (ArrayList<Stmt> path1, ArrayList<Stmt> path2) {
        if (path1.size() > path2.size()) return false;
        for (int i = 0; i < path1.size(); i++) {
            var i1 = path1.size() - i - 1;
            var i2 = path2.size() - i - 1;
            if (path1.get(i1) != path2.get(i2)) return false;
        }

        return true;
    }
}
