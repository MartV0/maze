package nl.uu.maze.search.strategy.PathGenerator;

import sootup.core.graph.BasicBlock;
import sootup.core.graph.StmtGraph;
import sootup.core.model.SootMethod;
import java.util.ArrayList;
import java.util.List;
import nl.uu.maze.search.strategy.PathGenerator.PathGenerator;
import nl.uu.maze.search.strategy.PathGenerator.PrimePathGenerator;

import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.jimple.common.stmt.JIfStmt;
import sootup.core.jimple.javabytecode.stmt.JSwitchStmt;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class NodeGenerator implements PathGenerator {
    public <V extends BasicBlock<V>> ArrayList<ArrayList<Stmt>> GeneratePaths(StmtGraph<V> cfg){
        ArrayList<ArrayList<Stmt>> paths = new ArrayList<ArrayList<Stmt>>();
        for (Stmt stmt: cfg.getNodes()) {
            ArrayList<Stmt> path = new ArrayList<Stmt>();
            path.add(stmt);
            paths.add(path);
        }
        return paths;
    }

    public String getName() {
        return "Node";
    }
}
