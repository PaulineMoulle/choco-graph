/*
 * Copyright (c) 1999-2014, Ecole des Mines de Nantes
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

package org.chocosolver.graphsolver.search.strategy;

import org.chocosolver.graphsolver.search.GraphAssignment;
import org.chocosolver.graphsolver.search.GraphDecision;
import org.chocosolver.graphsolver.search.strategy.arcs.LexArc;
import org.chocosolver.graphsolver.search.strategy.arcs.RandomArc;
import org.chocosolver.graphsolver.search.strategy.nodes.LexNode;
import org.chocosolver.graphsolver.search.strategy.nodes.RandomNode;
import org.chocosolver.graphsolver.variables.IGraphVar;
import org.chocosolver.solver.search.strategy.strategy.AbstractStrategy;
import org.chocosolver.util.PoolManager;

/**
 * <br/>
 *
 * @author Jean-Guillaume Fages
 * @since 1 April 2011
 */
public class GraphStrategy extends AbstractStrategy<IGraphVar> {

    //***********************************************************************************
    // VARIABLES
    //***********************************************************************************

    protected IGraphVar g;
    protected NodeStrategy nodeStrategy;
    protected ArcStrategy arcStrategy;
    protected NodeArcPriority priority;
    protected PoolManager<GraphDecision> pool;

    public enum NodeArcPriority {
        NODES_THEN_ARCS,
        ARCS
    }

    //***********************************************************************************
    // CONSTRUCTORS
    //***********************************************************************************

    /**
     * Dedicated graph branching strategy.
     *
     * @param g   a graph variable to branch on
     * @param ns strategy over nodes
     * @param as  strategy over arcs/edges
     * @param priority   enables to mention if it should first branch on nodes
     */
    public GraphStrategy(IGraphVar g, NodeStrategy ns, ArcStrategy as, NodeArcPriority priority) {
        super(g);
        this.g = g;
        this.nodeStrategy = ns;
        this.arcStrategy = as;
        this.priority = priority;
        pool = new PoolManager<>();
    }

    /**
     * Lexicographic graph branching strategy.
     * Branch on nodes then arcs/edges.
     * <br>
     * <br> node branching:
     * Let i be the first node such that
     * i in envelope(g) and i not in kernel(g).
     * The decision adds i to the kernel of g.
     * It is fails, then i is removed from the envelope of g.
     * <br>
     * arc/edge branching:
     * <br> node branching:
     * Let (i,j) be the first arc/edge such that
     * (i,j) in envelope(g) and (i,j) not in kernel(g).
     * The decision adds (i,j) to the kernel of g.
     * It is fails, then (i,j) is removed from the envelope of g
     *
     * @param g a graph variable to branch on
     */
    public GraphStrategy(IGraphVar g) {
        this(g, new LexNode(g), new LexArc(g), NodeArcPriority.NODES_THEN_ARCS);
    }

    /**
     * Random graph branching strategy.
     * Alternate randomly node and arc/edge decisions.
     * <br>
     * <br> node branching:
     * Let i be a randomly selected node such that
     * i in envelope(g) and i not in kernel(g).
     * The decision adds i to the kernel of g.
     * It is fails, then i is removed from the envelope of g.
     * <br>
     * arc/edge branching:
     * <br> node branching:
     * Let (i,j) be a randomly selected arc/edge arc/edge such that
     * (i,j) in envelope(g) and (i,j) not in kernel(g).
     * The decision adds (i,j) to the kernel of g.
     * It is fails, then (i,j) is removed from the envelope of g
     *
     * @param g a graph variable to branch on
     * @param seed     randomness seed
     */
    public GraphStrategy(IGraphVar g, long seed) {
        this(g, new RandomNode(g,seed), new RandomArc(g,seed), NodeArcPriority.NODES_THEN_ARCS);
    }

    //***********************************************************************************
    // METHODS
    //***********************************************************************************

    @Override
    public boolean init() {
        return true;
    }

    @Override
    public GraphDecision getDecision() {
        if (g.isInstantiated()) {
            return null;
        }
        GraphDecision dec = pool.getE();
        if (dec == null) {
            dec = new GraphDecision(pool);
        }
        switch (priority) {
            case NODES_THEN_ARCS:
                int node = nextNode();
                if (node != -1) {
                    dec.setNode(g, node, GraphAssignment.graph_enforcer);
                } else {
					if(arcStrategy==null){
						return null;
					}
                    nextArc();
                    dec.setArc(g, arcStrategy.getFrom(), arcStrategy.getTo(), GraphAssignment.graph_enforcer);
                }
                break;
            case ARCS:
            default:
                if(!nextArc()){
					return null;
				}
                dec.setArc(g, arcStrategy.getFrom(), arcStrategy.getTo(), GraphAssignment.graph_enforcer);
                break;
        }
        return dec;
    }

    public int nextNode() {
        return nodeStrategy.nextNode();
    }

    public boolean nextArc() {
        return arcStrategy.computeNextArc();
    }
}
