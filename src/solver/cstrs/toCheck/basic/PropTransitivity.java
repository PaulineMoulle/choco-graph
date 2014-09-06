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

package solver.cstrs.toCheck.basic;

import solver.constraints.Propagator;
import solver.constraints.PropagatorPriority;
import solver.exception.ContradictionException;
import solver.variables.EventType;
import solver.variables.IGraphVar;
import solver.variables.delta.IGraphDeltaMonitor;
import util.ESat;
import util.objects.setDataStructures.ISet;
import util.procedure.PairProcedure;

/**
 * Propagator that ensures that the relation of the graph is transitive : (a,b) + (b,c) => (a,c)
 *
 * @author Jean-Guillaume Fages
 */
public class PropTransitivity<V extends IGraphVar> extends Propagator<V> {

    //***********************************************************************************
    // VARIABLES
    //***********************************************************************************

    private V g;
    IGraphDeltaMonitor gdm;
    private PairProcedure arcEnforced;
    private PairProcedure arcRemoved;

    //***********************************************************************************
    // CONSTRUCTORS
    //***********************************************************************************

    public PropTransitivity(V graph) {
        super((V[]) new IGraphVar[]{graph}, PropagatorPriority.LINEAR, true);
        g = vars[0];
        gdm = g.monitorDelta(this);
        arcEnforced = new PairProcedure() {
            @Override
            public void execute(int from, int to) throws ContradictionException {
                enfArc(from, to);
            }
        };
        arcRemoved = new PairProcedure() {
            @Override
            public void execute(int from, int to) throws ContradictionException {
                remArc(from, to);
            }
        };
    }

    //***********************************************************************************
    // PROPAGATIONS
    //***********************************************************************************

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        int n = g.getNbMaxNodes();
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (g.getMandSuccOrNeighOf(i).contain(j)) {
                    enfArc(i, j);
                } else if (!g.getPotSuccOrNeighOf(i).contain(j)) {
                    remArc(i, j);
                }
            }
        }
        gdm.unfreeze();
    }

    @Override
    public void propagate(int idxVarInProp, int mask) throws ContradictionException {
        gdm.freeze();
        if ((mask & EventType.ENFORCEARC.mask) != 0) {
            gdm.forEachArc(arcEnforced, EventType.ENFORCEARC);
        }
        if ((mask & EventType.REMOVEARC.mask) != 0) {
            gdm.forEachArc(arcRemoved, EventType.REMOVEARC);
        }
        gdm.unfreeze();
    }

    //***********************************************************************************
    // INFO
    //***********************************************************************************

    @Override
    public int getPropagationConditions(int vIdx) {
        return EventType.REMOVEARC.mask + EventType.ENFORCEARC.mask;
    }

    @Override
    public ESat isEntailed() {
        int n = g.getNbMaxNodes();
        for (int i = 0; i < n; i++) {
            ISet succ = g.getPotSuccOrNeighOf(i);
            for (int j = succ.getFirstElement(); j >= 0; j = succ.getNextElement()) {
                for (int k = i + 1; k < n; k++) {
                    if (g.getMandSuccOrNeighOf(j).contain(k) && !g.getMandSuccOrNeighOf(i).contain(k)) {
                        return ESat.FALSE;
                    }
                }
            }
        }
        if (g.isInstantiated()) {
            return ESat.TRUE;
        }
        return ESat.UNDEFINED;
    }

    //***********************************************************************************
    // PROCEDURE
    //***********************************************************************************
    // --- Arc enforcings
    private void enfArc(int node, int succ) throws ContradictionException {
        if (node != succ) {
            ISet ker = g.getMandPredOrNeighOf(node);
            ISet env = g.getPotPredOrNeighOf(node);
            for (int i = env.getFirstElement(); i >= 0; i = env.getNextElement()) {
                if (ker.contain(i)) {
                    if (g.enforceArc(i, succ, aCause)) {
                        enfArc(i, succ);
                    }
                } else if (!g.getPotSuccOrNeighOf(i).contain(succ)) {
                    if (g.removeArc(i, node, aCause)) {
                        remArc(i, node);
                    }
                }
            }
            ker = g.getMandSuccOrNeighOf(succ);
            env = g.getPotSuccOrNeighOf(succ);
            for (int i = env.getFirstElement(); i >= 0; i = env.getNextElement()) {
                if (ker.contain(i)) {
                    if (g.enforceArc(node, i, aCause)) {
                        enfArc(node, i);
                    }
                } else if (!g.getPotSuccOrNeighOf(node).contain(i)) {
                    if (g.removeArc(succ, i, aCause)) {
                        remArc(succ, i);
                    }
                }
            }
        }
    }

    // --- Arc removals
    private void remArc(int from, int to) throws ContradictionException {
        if (from != to) {
            ISet nei = g.getMandSuccOrNeighOf(from);
            for (int i = nei.getFirstElement(); i >= 0; i = nei.getNextElement()) {
                if (g.removeArc(i, to, aCause)) {
                    remArc(i, to);
                }
            }
            nei = g.getMandPredOrNeighOf(to);
            for (int i = nei.getFirstElement(); i >= 0; i = nei.getNextElement()) {
                if (g.removeArc(from, i, aCause)) {
                    remArc(from, i);
                }
            }
        }
    }
}
