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

import solver.constraints.Propagator;
import solver.constraints.PropagatorPriority;
import solver.exception.ContradictionException;
import solver.variables.EventType;
import solver.variables.delta.GraphDeltaMonitor;
import solver.variables.GraphVar;
import util.ESat;
import util.objects.setDataStructures.ISet;
import util.procedure.IntProcedure;
import util.procedure.PairProcedure;

/**
 * Propagator that ensures that each node of the given subset of nodes has a loop
 *
 * @author Jean-Guillaume Fages
 */
public class PropEachNodeHasLoop extends Propagator<GraphVar> {

    //***********************************************************************************
    // VARIABLES
    //***********************************************************************************

    private GraphVar g;
    GraphDeltaMonitor gdm;
    private IntProcedure enfNode;
    private PairProcedure remArc;
    private ISet concernedNodes;

    //***********************************************************************************
    // CONSTRUCTORS
    //***********************************************************************************

    public PropEachNodeHasLoop(GraphVar graph, ISet concernedNodes) {
        super(new GraphVar[]{graph}, PropagatorPriority.UNARY, true);
        this.g = vars[0];
        gdm = (GraphDeltaMonitor) g.monitorDelta(this);
        this.enfNode = new NodeEnf();
        this.remArc = new ArcRem();
        this.concernedNodes = concernedNodes;
    }

    public PropEachNodeHasLoop(GraphVar graph) {
        this(graph, graph.getEnvelopGraph().getActiveNodes());
    }

    //***********************************************************************************
    // PROPAGATIONS
    //***********************************************************************************

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        ISet env = g.getEnvelopGraph().getActiveNodes();
        for (int i = env.getFirstElement(); i >= 0; i = env.getNextElement()) {
            if (concernedNodes.contain(i)) {
                if (g.getEnvelopGraph().isArcOrEdge(i, i)) {
                    if (g.getKernelGraph().getActiveNodes().contain(i)) {
                        g.enforceArc(i, i, aCause);
                    }
                } else {
                    g.removeNode(i, aCause);
                }
            }
        }
        gdm.unfreeze();
    }

    @Override
    public void propagate(int idxVarInProp, int mask) throws ContradictionException {
        gdm.freeze();
        if ((mask & EventType.REMOVEARC.mask) != 0) {
            gdm.forEachArc(remArc, EventType.REMOVEARC);
        }
        if ((mask & EventType.ENFORCENODE.mask) != 0) {
            gdm.forEachNode(enfNode, EventType.ENFORCENODE);
        }
        gdm.unfreeze();
    }

    //***********************************************************************************
    // INFO
    //***********************************************************************************

    @Override
    public int getPropagationConditions(int vIdx) {
        return EventType.ENFORCENODE.mask + EventType.REMOVEARC.mask;
    }

    @Override
    public ESat isEntailed() {
        ISet ker = g.getKernelGraph().getActiveNodes();
        for (int i = ker.getFirstElement(); i >= 0; i = ker.getNextElement()) {
            if (concernedNodes.contain(i) && !g.getKernelGraph().getSuccsOrNeigh(i).contain(i)) {
                return ESat.FALSE;
            }
        }
        if (g.getEnvelopOrder() != g.getKernelOrder()) {
            return ESat.UNDEFINED;
        }
        return ESat.TRUE;
    }

    //***********************************************************************************
    // PROCEDURE
    //***********************************************************************************

    private class NodeEnf implements IntProcedure {
        @Override
        public void execute(int i) throws ContradictionException {
            if (concernedNodes.contain(i)) {
                g.enforceArc(i, i, aCause);
            }
        }
    }

    private class ArcRem implements PairProcedure {
        @Override
        public void execute(int from, int to) throws ContradictionException {
            if (from == to && concernedNodes.contain(to)) {
                g.removeNode(from, aCause);
            }
        }
    }
}
