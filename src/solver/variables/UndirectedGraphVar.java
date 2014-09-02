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

package solver.variables;

import solver.ICause;
import solver.Solver;
import solver.exception.ContradictionException;
import solver.variables.delta.IGraphDelta;
import util.objects.graphs.UndirectedGraph;
import util.objects.setDataStructures.SetType;

/**
 * Created by IntelliJ IDEA.
 * User: chameau, Jean-Guillaume Fages
 * Date: 7 feb. 2011
 */
public class UndirectedGraphVar extends GraphVar<UndirectedGraph> {

    //////////////////////////////// GRAPH PART /////////////////////////////////////////

    //***********************************************************************************
    // CONSTRUCTORS
    //***********************************************************************************

    public UndirectedGraphVar(String name, Solver solver, int nbNodes,
                              SetType typeEnv, SetType typeKer, boolean allNodes) {
        super(name, solver);
        envelop = new UndirectedGraph(environment, nbNodes, typeEnv, allNodes);
        kernel = new UndirectedGraph(environment, nbNodes, typeKer, allNodes);
    }

    public UndirectedGraphVar(String name, Solver solver, int nbNodes, boolean allNodes) {
        this(name, solver, nbNodes, SetType.ENVELOPE_BEST, SetType.KERNEL_BEST, allNodes);
    }

    //***********************************************************************************
    // METHODS
    //***********************************************************************************

	@Override
    public boolean removeArc(int x, int y, ICause cause) throws ContradictionException {
        assert cause != null;
        if (kernel.edgeExists(x, y)) {
            this.contradiction(cause, EventType.REMOVEARC, "remove mandatory arc");
            return false;
        }
        if (envelop.removeEdge(x, y)) {
            if (reactOnModification) {
                delta.add(x, IGraphDelta.AR_tail, cause);
                delta.add(y, IGraphDelta.AR_head, cause);
            }
            EventType e = EventType.REMOVEARC;
            notifyPropagators(e, cause);
            return true;
        }
        return false;
    }

	@Override
    public boolean enforceArc(int x, int y, ICause cause) throws ContradictionException {
        assert cause != null;
        enforceNode(x, cause);
        enforceNode(y, cause);
        if (envelop.edgeExists(x, y)) {
            if (kernel.addEdge(x, y)) {
                if (reactOnModification) {
                    delta.add(x, IGraphDelta.AE_tail, cause);
                    delta.add(y, IGraphDelta.AE_head, cause);
                }
                EventType e = EventType.ENFORCEARC;
                notifyPropagators(e, cause);
                return true;
            }
            return false;
        }
        this.contradiction(cause, EventType.ENFORCEARC, "enforce arc which is not in the domain");
        return false;
    }

    //***********************************************************************************
    // ACCESSORS
    //***********************************************************************************

    @Override
    public boolean isDirected() {
        return false;
    }

	/**
	 * Checks whether or not the edge (u,v) may belong to a solution or not
	 * (i.e. if it is present in the envelop graph)
	 * @param u the id of a node
	 * @param v the id of a node
	 * @return true iff edge (u,v) belongs to the envelop graph
	 */
	public boolean isEdgePossible(int u, int v){
		return envelop.edgeExists(u,v);
	}

	/**
	 * Checks whether or not the edge (u,v) must belong to every solution or not
	 * (i.e. if it is present in the kernel graph)
	 * @param u the id of a node
	 * @param v the id of a node
	 * @return true iff edge (u,v) belongs to the kernel graph
	 */
	public boolean isEdgeMandatory(int u, int v){
		return kernel.edgeExists(u,v);
	}
}
