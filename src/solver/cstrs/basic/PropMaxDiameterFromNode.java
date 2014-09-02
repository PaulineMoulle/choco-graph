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

package solver.cstrs.basic;

import gnu.trove.list.array.TIntArrayList;
import solver.constraints.Propagator;
import solver.constraints.PropagatorPriority;
import solver.exception.ContradictionException;
import solver.variables.EventType;
import solver.variables.UndirectedGraphVar;
import util.ESat;
import util.objects.setDataStructures.ISet;

import java.util.BitSet;

public class PropMaxDiameterFromNode extends Propagator<UndirectedGraphVar> {

    //***********************************************************************************
    // VARIABLES
    //***********************************************************************************

    private UndirectedGraphVar g;
    private int maxDiam, node, n;
    private BitSet visited;
    private TIntArrayList set, nextSet;


    //***********************************************************************************
    // CONSTRUCTORS
    //***********************************************************************************

    public PropMaxDiameterFromNode(UndirectedGraphVar graph, int maxDiam, int rootNode) {
        super(new UndirectedGraphVar[]{graph}, PropagatorPriority.LINEAR, true);
        this.g = vars[0];
        this.node = rootNode;
        this.maxDiam = maxDiam;
        this.n = g.getEnvelopGraph().getNbNodes();
        this.visited = new BitSet(n);
        this.set = new TIntArrayList();
        this.nextSet = new TIntArrayList();
    }

    //***********************************************************************************
    // PROPAGATIONS
    //***********************************************************************************

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        g.enforceNode(node, aCause);
        if (BFS() >= maxDiam) {
            for (int i = visited.nextClearBit(0); i < n; i = visited.nextClearBit(i + 1)) {
                g.removeNode(i, aCause);
            }
        }
    }

    public int BFS() {
        int i = node;
        nextSet.clear();
        set.clear();
        visited.clear();
        set.add(i);
        visited.set(i);
        ISet nei;
        int depth = 0;
        while (!set.isEmpty() && depth < maxDiam) {
            for (i = set.size() - 1; i >= 0; i--) {
                nei = g.getEnvelopGraph().getNeighborsOf(set.get(i));
                for (int j = nei.getFirstElement(); j >= 0; j = nei.getNextElement()) {
                    if (!visited.get(j)) {
                        visited.set(j);
                        nextSet.add(j);
                    }
                }
            }
            depth++;
            TIntArrayList tmp = nextSet;
            nextSet = set;
            set = tmp;
            nextSet.clear();
        }
        return depth;
    }

    @Override
    public void propagate(int idxVarInProp, int mask) throws ContradictionException {
        propagate(0);
    }

    //***********************************************************************************
    // INFO
    //***********************************************************************************

    @Override
    public int getPropagationConditions(int vIdx) {
        return EventType.REMOVEARC.mask + EventType.ENFORCENODE.mask;
    }

    @Override
    public ESat isEntailed() {
        ISet nodes = g.getKernelGraph().getActiveNodes();
        if (!nodes.contain(node)) {
            return ESat.FALSE;
        }
        int k = BFS();
        if (k >= maxDiam) {
            for (int i = visited.nextClearBit(0); i < n; i = visited.nextClearBit(i + 1)) {
                if (nodes.contain(i)) {
                    return ESat.FALSE;
                }
            }
        }
        if (g.isInstantiated()) {
            return ESat.TRUE;
        }
        return ESat.UNDEFINED;
    }
}
