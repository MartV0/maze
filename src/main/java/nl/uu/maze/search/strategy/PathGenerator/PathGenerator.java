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
}
