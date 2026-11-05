package nl.uu.maze.search.strategy.PathGenerator;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import sootup.core.graph.BasicBlock;
import sootup.core.graph.StmtGraph;
import sootup.core.model.SootMethod;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import nl.uu.maze.search.strategy.PathGenerator.PathGenerator;
import nl.uu.maze.util.PrefixTree;

import sootup.core.jimple.common.stmt.Stmt;

/** PathGenerator interface generates required path based on a input CFG, used by PathStrategy*/
public interface PathGenerator {
    final static Logger logger = LoggerFactory.getLogger(PrimePathGenerator.class);

    public <V extends BasicBlock<V>> ArrayList<ArrayList<Stmt>> GeneratePaths(StmtGraph<V> cfg);
    public String getName();

    /** Removes any paths from paths that are postfixes of another path */
    public static void remove_postfixes(ArrayList<ArrayList<Stmt>> paths) {
        PrefixTree<Stmt> pathsTree = new PrefixTree<Stmt>();
        // paths have to be reversed because it is a prefix tree, but we really need a postfix tree
        for(var path: paths) {
            pathsTree.insert(reversePath(path));
        }
        paths.removeIf(path -> pathsTree.isStrictPrefix(reversePath(path)));
    }

    private static List<Stmt> reversePath(List<Stmt> path) {
        var reversed = new ArrayList<Stmt>(path.size());
        for (int i = path.size() - 1; i >= 0; i--) {
            reversed.add(path.get(i));
        }
        return reversed;
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
