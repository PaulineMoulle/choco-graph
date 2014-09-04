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
package solver.cstrs.path;

import solver.Solver;
import solver.constraints.Constraint;
import solver.constraints.Propagator;
import solver.constraints.PropagatorPriority;
import solver.exception.ContradictionException;
import solver.variables.EventType;
import solver.variables.delta.IGraphDeltaMonitor;
import solver.variables.IDirectedGraphVar;
import util.ESat;
import util.graphOperations.connectivity.StrongConnectivityFinder;
import util.objects.graphs.DirectedGraph;
import util.objects.setDataStructures.ISet;
import util.objects.setDataStructures.SetType;
import util.procedure.PairProcedure;

import java.util.BitSet;

/**
 * Propagator for AllDifferent AC constraint for graphs
 * directed or undirected
 * <p/>
 * Uses Regin algorithm
 * Runs in O(m.n) worst case time for the initial propagation and then in O(n+m) time
 * per arc removed from the support
 * <p/>
 * Runs incrementally for maintaining a matching
 * <p/>
 *
 * @author Jean-Guillaume Fages
 */
public class PropAllDiffGraphIncremental extends Propagator<IDirectedGraphVar> {

    //***********************************************************************************
    // VARIABLES
    //***********************************************************************************

    private int n, n2;
    private IDirectedGraphVar g;
    private IGraphDeltaMonitor gdm;
    private DirectedGraph digraph;
    private int[] matching;
    private int[] nodeSCC;
    private BitSet free;
    private PairProcedure remProc;
    int matchingCardinality;
    private StrongConnectivityFinder SCCfinder;
    // for augmenting matching
    int[] father;
    int[] fifo;
    BitSet in;

    //***********************************************************************************
    // CONSTRUCTORS
    //***********************************************************************************

    /**
     * AllDifferent constraint for a graph
     * enables to control the cardinality of the matching
     *
     * @param graph
     * @param matchingCardinality
     */
    public PropAllDiffGraphIncremental(IDirectedGraphVar graph, int matchingCardinality) {
        super(new IDirectedGraphVar[]{graph}, PropagatorPriority.QUADRATIC, true);
        n = graph.getNbMaxNodes();
        n2 = 2 * n;
        g = graph;
        gdm = g.monitorDelta(this);
        this.matchingCardinality = matchingCardinality;
        matching = new int[n2];
        nodeSCC = new int[n2];
        digraph = new DirectedGraph(solver, n2, SetType.LINKED_LIST, false);
        free = new BitSet(n2);
        if (g.isDirected()) {
            remProc = new DirectedRemProc();
        } else {
            remProc = new UndirectedRemProc();
        }
        father = new int[n2];
        in = new BitSet(n2);
        fifo = new int[n2];
        SCCfinder = new StrongConnectivityFinder(digraph);
    }

    /**
     * AllDifferent constraint for a graph
     * suppose that a perfect matching is exepcted
     *
     * @param graph
     * @param sol
     * @param constraint
     */
    public PropAllDiffGraphIncremental(IDirectedGraphVar graph, Solver sol, Constraint constraint) {
        this(graph, graph.getNbMaxNodes());
    }

    //***********************************************************************************
    // Initialization
    //***********************************************************************************

    private void buildDigraph() {
        free.set(0, n2);
        int j;
        ISet nei;
        for (int i = 0; i < n; i++) {
            nei = g.getPotSuccOf(i);
            for (j = nei.getFirstElement(); j >= 0; j = nei.getNextElement()) {
                j += n;
                if (free.get(i) && free.get(j)) {
                    digraph.addArc(j, i);
                    free.clear(i);
                    free.clear(j);
                } else {
                    digraph.addArc(i, j);
                }
            }
        }
    }

    //***********************************************************************************
    // MATCHING
    //***********************************************************************************

    private void repairMatching() throws ContradictionException {
        for (int i = free.nextSetBit(0); i >= 0 && i < n; i = free.nextSetBit(i + 1)) {
            tryToMatch(i);
        }
        int p;
        int cardinality = 0;
        for (int i = 0; i < n; i++) {
            p = digraph.getPredecessorsOf(i).getFirstElement();
            if (p != -1) {
                cardinality++;
                matching[p] = i;
            }
            matching[i] = p;
        }
        if (cardinality < matchingCardinality) {
            contradiction(g, "");
        }
    }

    private void tryToMatch(int i) throws ContradictionException {
        int mate = augmentPath_BFS(i);
        if (mate != -1) {
            free.clear(mate);
            free.clear(i);
            int tmp = mate;
            while (tmp != i) {
                digraph.removeArc(father[tmp], tmp);
                digraph.addArc(tmp, father[tmp]);
                tmp = father[tmp];
            }
        }
    }

    private int augmentPath_BFS(int root) {
        in.clear();
        int firstIdx = 0;
        int lastIdx = 0;
        fifo[lastIdx++] = root;
        int x, y;
        ISet succs;
        while (firstIdx < lastIdx) {
            x = fifo[firstIdx++];
            succs = digraph.getSuccessorsOf(x);
            for (y = succs.getFirstElement(); y >= 0; y = succs.getNextElement()) {
                if (!in.get(y)) {
                    father[y] = x;
                    fifo[lastIdx++] = y;
                    in.set(y);
                    if (free.get(y)) {
                        return y;
                    }
                }
            }
        }
        return -1;
    }

    //***********************************************************************************
    // PRUNING
    //***********************************************************************************

    private void filter() throws ContradictionException {
        SCCfinder.findAllSCC();
        nodeSCC = SCCfinder.getNodesSCC();
        ISet succ;
        int j;
        for (int node = 0; node < n; node++) {
            succ = g.getPotSuccOf(node);
            for (j = succ.getFirstElement(); j >= 0; j = succ.getNextElement()) {
                if (nodeSCC[node] != nodeSCC[j + n]) {
                    if (matching[node] == j + n && matching[j + n] == node) {
                        g.enforceArc(node, j, aCause);
                    } else {
                        g.removeArc(node, j, aCause);
                        digraph.removeArc(node, j + n);
                    }
                }
            }
        }
    }

    //***********************************************************************************
    // PROPAGATION
    //***********************************************************************************

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        buildDigraph();
        repairMatching();
        filter();
        gdm.unfreeze();
    }

    @Override
    public void propagate(int idxVarInProp, int mask) throws ContradictionException {
        free.clear();
        gdm.freeze();
        gdm.forEachArc(remProc, EventType.REMOVEARC);
        gdm.unfreeze();
        repairMatching();
        filter();
    }

    //***********************************************************************************
    // INFO
    //***********************************************************************************

    @Override
    public int getPropagationConditions(int vIdx) {
        return EventType.REMOVEARC.mask;
    }

    @Override
    public ESat isEntailed() {
        if (!g.isInstantiated()) {
            return ESat.UNDEFINED;
        }
        BitSet b = new BitSet(n);
        int next;
        for (int i = 0; i < n; i++) {
            next = g.getPotSuccOf(i).getFirstElement();
            if (next != -1) {
                if (b.get(next)) {
                    return ESat.FALSE;
                } else {
                    b.set(next);
                }
            }
        }
        return ESat.TRUE;
    }

    private class DirectedRemProc implements PairProcedure {
        public void execute(int from, int to) throws ContradictionException {
            to += n;
            if (digraph.arcExists(to, from)) {
                free.set(to);
                free.set(from);
                digraph.removeArc(to, from);
            }
            if (digraph.arcExists(from, to)) {
                digraph.removeArc(from, to);
            }
        }
    }

    private class UndirectedRemProc implements PairProcedure {
        public void execute(int from, int to) throws ContradictionException {
            check(from, to + n);
            check(to, from + n);
        }

        private void check(int from, int to) {
            if (digraph.arcExists(to, from)) {
                free.set(to);
                free.set(from);
                digraph.removeArc(to, from);
            }
            if (digraph.arcExists(from, to)) {
                digraph.removeArc(from, to);
            }
        }
    }
}
