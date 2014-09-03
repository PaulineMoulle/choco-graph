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

package solver.cstrs.channeling;

import solver.constraints.Propagator;
import solver.constraints.PropagatorPriority;
import solver.cstrs.channeling.relations.GraphRelation;
import solver.exception.ContradictionException;
import solver.variables.EventType;
import solver.variables.IGraphVar;
import solver.variables.Variable;
import util.ESat;
import util.objects.setDataStructures.ISet;

/**
 * Propagator channeling a graph and an array of variables
 *
 * @author Jean-Guillaume Fages
 */
public class PropRelationGraph extends Propagator {

    //***********************************************************************************
    // VARIABLES
    //***********************************************************************************

    private IGraphVar g;
    private int n;
    private Variable[] nodeVars;
    private GraphRelation relation;

    //***********************************************************************************
    // CONSTRUCTOR
    //***********************************************************************************

    public PropRelationGraph(Variable[] vars, IGraphVar graph, GraphRelation relation) {
        super(vars, PropagatorPriority.LINEAR, true);
        this.g = graph;
        this.nodeVars = vars;
        this.n = nodeVars.length;
        this.relation = relation;
    }

    //***********************************************************************************
    // PROPAGATIONS
    //***********************************************************************************

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        for (int i = 0; i < n; i++) {
            checkVar(i);
        }
    }

    @Override
    public void propagate(int idxVarInProp, int mask) throws ContradictionException {
        checkVar(idxVarInProp);
    }

    //***********************************************************************************
    // INFO
    //***********************************************************************************

    @Override
    public int getPropagationConditions(int vIdx) {
        return EventType.ALL_FINE_EVENTS.mask;
    }

    @Override
    public ESat isEntailed() {
        ISet nei;
        for (int i = 0; i < n; i++) {
            nei = g.getEnvelopGraph().getSuccsOrNeigh(i);
            for (int j = nei.getFirstElement(); j >= 0; j = nei.getNextElement()) {
                if (g.getKernelGraph().isArcOrEdge(i, j) && relation.isEntail(i, j) == ESat.FALSE) {
                    return ESat.FALSE;
                }
            }
            for (int j = nei.getFirstElement(); j >= 0; j = nei.getNextElement()) {
                if (relation.isEntail(i, j) == ESat.UNDEFINED && !g.getKernelGraph().isArcOrEdge(i, j)) {
                    return ESat.UNDEFINED;
                }
            }
        }
        return ESat.TRUE;
    }

    //***********************************************************************************
    // PROCEDURE
    //***********************************************************************************

    private void checkVar(int i) throws ContradictionException {
        ISet ker = g.getMandatoryNodes();
        ISet nei = g.getEnvelopGraph().getSuccsOrNeigh(i);
        for (int j = nei.getFirstElement(); j >= 0; j = nei.getNextElement()) {
            switch (relation.isEntail(i, j)) {
                case TRUE:
                    if (ker.contain(i) && ker.contain(j)) {
                        g.enforceArc(i, j, aCause);
                    }
                    break;
                case FALSE:
                    g.removeArc(i, j, aCause);
                    break;
            }
        }
    }
}
