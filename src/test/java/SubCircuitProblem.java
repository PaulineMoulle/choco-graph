/*
 * Copyright (c) 1999-2012, Ecole des Mines de Nantes
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Ecole des Mines de Nantes nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package test.java;

import org.testng.Assert;
import org.testng.annotations.Test;
import samples.AbstractProblem;
import samples.input.GraphGenerator;
import solver.Cause;
import solver.Solver;
import solver.constraints.Constraint;
import solver.constraints.IntConstraintFactory;
import solver.cstrs.basic.PropNbNodes;
import solver.cstrs.connectivity.PropNbSCC;
import solver.cstrs.degree.PropNodeDegree_AtLeast_Coarse;
import solver.cstrs.degree.PropNodeDegree_AtMost_Incr;
import solver.exception.ContradictionException;
import solver.search.GraphStrategyFactory;
import solver.search.strategy.arcs.RandomArc;
import solver.search.strategy.nodes.RandomNode;
import solver.search.strategy.strategy.AbstractStrategy;
import solver.search.strategy.GraphStrategy;
import solver.variables.GraphVarFactory;
import solver.variables.IntVar;
import solver.variables.VariableFactory;
import solver.variables.IDirectedGraphVar;
import util.objects.graphs.DirectedGraph;
import util.objects.graphs.Orientation;
import util.objects.setDataStructures.SetType;

import java.util.Random;

public class SubCircuitProblem extends AbstractProblem {

	//***********************************************************************************
	// VARIABLES
	//***********************************************************************************

	private static SetType gt;

	private int n;
	private IDirectedGraphVar graph;
	private IntVar circuitLength;
	private boolean[][] adjacencyMatrix;
	private Constraint gc;
	// model parameters
	private long seed;

	//***********************************************************************************
	// CONSTRUCTORS
	//***********************************************************************************

	private void set(boolean[][] matrix, long s) {
		seed = s;
		n = matrix.length;
		adjacencyMatrix = matrix;
	}

	//***********************************************************************************
	// MODEL
	//***********************************************************************************


	@Override
	public void createSolver() {
		solver = new Solver();
	}

	@Override
	public void buildModel() {
		// create model
		DirectedGraph GLB = new DirectedGraph(solver, n, SetType.LINKED_LIST, false);
		DirectedGraph GUB = new DirectedGraph(solver, n, gt, false);
		for (int i = 0; i < n; i++) {
			if(!adjacencyMatrix[i][i]){
				GLB.activateNode(i);
			}
			for (int j = 0; j < n; j++) {
				if (adjacencyMatrix[i][j]) {
					GUB.addArc(i, j);
				}
			}
		}
		graph = GraphVarFactory.directedGraph("G", GLB, GUB, solver);
		circuitLength = VariableFactory.bounded("length",0,n,solver);
		solver.post(new Constraint("SubCircuit",
				new PropNbNodes(graph, circuitLength),
				new PropNbSCC(graph,VariableFactory.fixed(1,solver)),
				new PropNodeDegree_AtLeast_Coarse(graph, Orientation.SUCCESSORS, 1),
				new PropNodeDegree_AtLeast_Coarse(graph, Orientation.PREDECESSORS, 1),
				new PropNodeDegree_AtMost_Incr(graph, Orientation.SUCCESSORS, 1),
				new PropNodeDegree_AtMost_Incr(graph, Orientation.PREDECESSORS, 1)
		));
	}

	//***********************************************************************************
	// METHODS
	//***********************************************************************************


	@Override
	public void configureSearch() {
		AbstractStrategy arcs = GraphStrategyFactory.graphStrategy(graph,null,new RandomArc(graph,seed), GraphStrategy.NodeArcPriority.ARCS);
		AbstractStrategy nodes = GraphStrategyFactory.graphStrategy(graph,new RandomNode(graph,seed), null, GraphStrategy.NodeArcPriority.NODES_THEN_ARCS);
		solver.set(arcs,nodes);
//		solver.set(GraphStrategyFactory.random(graph,seed));
//		solver.set(GraphStrategyFactory.lexico(graph));
	}

	@Override
	public void solve() {
		solver.findAllSolutions();
	}

	@Override
	public void prettyOut() {}

	//***********************************************************************************
	// TESTS
	//***********************************************************************************


	@Test(groups = "1s")
	public static void test1() {
		int[] sizes = new int[]{15};
		int[] seeds = new int[]{42};
		double[] densities = new double[]{0.2};
		boolean[][] matrix;
		for (int n : sizes) {
			for (double d : densities) {
				for (int s : seeds) {
//					System.out.println("n:" + n + " d:" + d + " s:" + s);
					GraphGenerator gg = new GraphGenerator(n, s, GraphGenerator.InitialProperty.HamiltonianCircuit);
					matrix = gg.arcBasedGenerator(d);
//					System.out.println("graph generated");
					testModels(matrix, s);
				}
			}
		}
	}

	@Test(groups = "1s")
	public static void test2() {
		int n = 8;
		boolean[][] matrix = new boolean[n][n];
		matrix[0][6] = true;
		matrix[1][0] = true;
		matrix[1][7] = true;
		matrix[2][3] = true;
		matrix[2][5] = true;
		matrix[2][7] = true;
		matrix[3][4] = true;
		matrix[3][5] = true;
		matrix[3][6] = true;
		matrix[4][3] = true;
		matrix[4][5] = true;
		matrix[4][6] = true;
		matrix[4][7] = true;
		matrix[5][0] = true;
		matrix[5][4] = true;
		matrix[6][1] = true;
		matrix[6][4] = true;
		matrix[7][0] = true;
		matrix[7][1] = true;
		matrix[7][2] = true;
		matrix[7][3] = true;
		matrix[7][5] = true;
		long nbSols = referencemodel(matrix,0);
		assert nbSols == referencemodel(matrix,12);
//		System.out.println(nbSols);
		testModels(matrix, 0);
	}

	@Test(groups = "1m")
	public static void test3() {
		int[] sizes = new int[]{6, 10};
		int[] seeds = new int[]{0, 10, 42};
		double[] densities = new double[]{0.2, 0.4};
		boolean[][] matrix;
		for (int n : sizes) {
			for (double d : densities) {
				for (int s : seeds) {
//					System.out.println("n:" + n + " d:" + d + " s:" + s);
					GraphGenerator gg = new GraphGenerator(n, s, GraphGenerator.InitialProperty.HamiltonianCircuit);
					matrix = gg.arcBasedGenerator(d);
//					System.out.println("graph generated");
					testModels(matrix, s);
				}
			}
		}
	}

	@Test(groups = "1m")
	public static void test4() {
		int[] sizes = new int[]{8};
		double[] densities = new double[]{0.1, 0.2, 0.5, 0.7};
		boolean[][] matrix;
		for (int n : sizes) {
			for (double d : densities) {
				for (int ks = 0; ks < 10; ks++) {
					long s = System.currentTimeMillis();
//					System.out.println("n:" + n + " d:" + d + " s:" + s);
					GraphGenerator gg = new GraphGenerator(n, s, GraphGenerator.InitialProperty.HamiltonianCircuit);
					matrix = gg.arcBasedGenerator(d);
//					System.out.println("graph generated");
					testModels(matrix, s);
				}
			}
		}
	}

	private static void testModels(boolean[][] m, long seed) {
		Random rd = new Random(seed);
		for(int i=0;i<m.length;i++){
			m[i][i] = rd.nextBoolean();
		}
		long nbSols = referencemodel(m,0);
		if (nbSols == -1) {
			throw new UnsupportedOperationException();
		}
		assert nbSols == referencemodel(m,12);
//		System.out.println(nbSols + " sols expected");
		boolean[] vls = new boolean[]{false, true};
		gt = SetType.SWAP_ARRAY;
		for (int i = 0; i < 4; i++) {
			for (boolean p : vls) {
				SubCircuitProblem hcp = new SubCircuitProblem();
				hcp.set(m, seed);
				hcp.execute();
				Assert.assertEquals(nbSols, hcp.solver.getMeasures().getSolutionCount(), "nb sol incorrect " + i + " ; " + p + " ; " + gt);
			}
		}
		System.gc();
		System.gc();
		System.gc();
	}

	private static long referencemodel(boolean[][] matrix, int offset) {
		int n = matrix.length;
		Solver solver = new Solver();
		IntVar[] vars = VariableFactory.enumeratedArray("", n, offset, n - 1 + offset, solver);
		IntVar length = VariableFactory.bounded("length",0,n,solver);
		try {
			for (int i = 0; i < n; i++) {
				for (int j = 0; j < n; j++) {
					if (!matrix[i][j]) {
						vars[i].removeValue(j+offset, Cause.Null);
					}
				}
			}
		} catch (ContradictionException e) {
			e.printStackTrace();
		}
		solver.post(IntConstraintFactory.subcircuit(vars, offset, length));
		long nbsol = solver.findAllSolutions();
		if (nbsol == 0) {
			return -1;
		}
		return solver.getMeasures().getSolutionCount();
	}
}
