package nl.uu.maze.examples;

public class CobaSimple {
	
	int x ;
	
	public CobaSimple(int x) {
		if (x<0) 
			throw new IllegalArgumentException() ;
		this.x = x ;
	}
	
	public int foo(int y) {
		if (y<0)
			return 0 ;
		else 
			x += y ;
		return x ;
	}

}
