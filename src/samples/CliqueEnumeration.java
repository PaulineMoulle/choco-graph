/**
 *  Copyright (c) 1999-2011, Ecole des Mines de Nantes
 *  All rights reserved.
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *      * Redistributions of source code must retain the above copyright
 *        notice, this list of conditions and the following disclaimer.
 *      * Redistributions in binary form must reproduce the above copyright
 *        notice, this list of conditions and the following disclaimer in the
 *        documentation and/or other materials provided with the distribution.
 *      * Neither the name of the Ecole des Mines de Nantes nor the
 *        names of its contributors may be used to endorse or promote products
 *        derived from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND ANY
 *  EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
 *  DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package samples;

import solver.Solver;
import solver.cstrs.GraphConstraintFactory;
import solver.search.loop.monitors.IMonitorSolution;
import solver.search.GraphStrategyFactory;
import solver.variables.GraphVarFactory;
import solver.variables.VariableFactory;
import solver.variables.IUndirectedGraphVar;
import util.objects.graphs.UndirectedGraph;
import util.objects.setDataStructures.SetType;

/**
 * This sample illustrates how to use a graph variable to
 * enumerate all cliques that respect certain conditions
 * In this example, we enumerates cliques which contain edge (1,2)
 * by using a graph variable and a clique partitioning constraint
 *
 * @author Jean-Guillaume Fages
 */
public class CliqueEnumeration extends AbstractProblem {

	// graph variable
	private IUndirectedGraphVar graphvar;
	// five nodes are involved
	private int n = 5;

	public static void main(String[] args) {
		new CliqueEnumeration().execute(args);
	}

	@Override
	public void createSolver() {
		solver = new Solver("clique enumeration");
	}

	@Override
	public void buildModel() {
		// input data
		boolean[][] link = new boolean[n][n];
		link[1][2] = true;
		link[2][3] = true;
		link[2][4] = true;
		link[1][3] = true;
		link[1][4] = true;
		link[3][4] = true;
		// graph variable domain
		UndirectedGraph GLB = new UndirectedGraph(solver.getEnvironment(),n, SetType.BITSET,false);
		UndirectedGraph GUB = new UndirectedGraph(solver.getEnvironment(),n, SetType.BITSET,false);
		for (int i = 0; i < n; i++) {
			GUB.activateNode(i);			// potential node
			GUB.addEdge(i, i);				// potential loop
			for (int j = i + 1; j < n; j++) {
				if (link[i][j]) {
					GUB.addEdge(i, j);		// potential edge
				}
			}
		}
		GLB.activateNode(1);				// 1 and 2 must belong to the solution
		GLB.activateNode(2);
		GLB.addEdge(1,2);					// 1 and 2 must belong to the same clique
		// graph variable
		graphvar = GraphVarFactory.undirectedGraph("G", GLB, GUB, solver);
		// constraint : the graph must be a clique
		solver.post(GraphConstraintFactory.nCliques(graphvar, VariableFactory.fixed(1, solver)));
	}

	@Override
	public void configureSearch() {
		// search strategy (lexicographic)
		solver.set(GraphStrategyFactory.graphLexico(graphvar));
		// log
		solver.plugMonitor(new IMonitorSolution() {
			public void onSolution() {
				System.out.println("solution found : " + graphvar.getPotNeighOf(1));
			}
		});
	}

	@Override
	public void solve() {
		// enumeration
		solver.findAllSolutions();
	}

	@Override
	public void prettyOut() {
	}
}
