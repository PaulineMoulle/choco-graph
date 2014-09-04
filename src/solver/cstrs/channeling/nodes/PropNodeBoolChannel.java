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

/**
 * Created by IntelliJ IDEA.
 * User: Jean-Guillaume Fages
 * Date: 14/01/13
 * Time: 16:36
 */

package solver.cstrs.channeling.nodes;

import solver.constraints.Propagator;
import solver.constraints.PropagatorPriority;
import solver.exception.ContradictionException;
import solver.variables.BoolVar;
import solver.variables.EventType;
import solver.variables.IGraphVar;
import solver.variables.Variable;
import solver.variables.delta.IGraphDeltaMonitor;
import util.ESat;
import util.procedure.IntProcedure;
import util.tools.ArrayUtils;

/**
 * Channeling between a graph variable and set variables
 * representing either node neighbors or node successors
 *
 * @author Jean-Guillaume Fages
 */
public class PropNodeBoolChannel extends Propagator<Variable> {

	//***********************************************************************************
	// VARIABLES
	//***********************************************************************************

	private BoolVar bool;
	private int vertex;
	private IGraphVar g;

	//***********************************************************************************
	// CONSTRUCTORS
	//***********************************************************************************

	public PropNodeBoolChannel(BoolVar isIn, int vertex, IGraphVar gV) {
		super(new Variable[]{isIn,gV}, PropagatorPriority.UNARY, false);
		this.bool = isIn;
		this.vertex = vertex;
		this.g = gV;
	}

	//***********************************************************************************
	// METHODS
	//***********************************************************************************

	@Override
	public int getPropagationConditions(int vIdx) {
		if (vIdx == 1) {
			return EventType.ENFORCENODE.mask + EventType.REMOVENODE.mask;
		}else{
			return EventType.INT_ALL_MASK();
		}
	}

	@Override
	public void propagate(int evtmask) throws ContradictionException {
		if(vertex<0 || vertex>=g.getNbMaxNodes() || !g.getPotentialNodes().contain(vertex)){
			bool.setToFalse(aCause);
		}else if(g.getMandatoryNodes().contain(vertex)){
			bool.setToTrue(aCause);
		}else if(bool.getLB()==1){
			g.enforceNode(vertex,aCause);
		}else if(bool.getUB()==0){
			g.removeNode(vertex,aCause);
		}
	}

	@Override
	public ESat isEntailed() {
		if((vertex<0 || vertex>=g.getNbMaxNodes())
				|| (bool.getLB()==1 && !g.getPotentialNodes().contain(vertex))
				|| (bool.getUB()==0 && g.getMandatoryNodes().contain(vertex))
				){
			return ESat.FALSE;
		};
		if(bool.isInstantiated()
				&& g.getMandatoryNodes().contain(vertex)==g.getPotentialNodes().contain(vertex)){
			return ESat.TRUE;
		}
		return ESat.UNDEFINED;
	}
}