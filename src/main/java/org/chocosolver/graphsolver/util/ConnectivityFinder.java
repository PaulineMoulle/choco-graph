/**
 * Copyright (c) 1999-2011, Ecole des Mines de Nantes
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * <p>
 * * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * * Neither the name of the Ecole des Mines de Nantes nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 * <p>
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
 * <p>
 * Created by IntelliJ IDEA.
 * User: Jean-Guillaume Fages
 * Date: 17/04/2016
 * Time: 13:11
 */

/**
 * Created by IntelliJ IDEA.
 * User: Jean-Guillaume Fages
 * Date: 17/04/2016
 * Time: 13:11
 */

package org.chocosolver.graphsolver.util;


import gnu.trove.list.array.TIntArrayList;
import org.chocosolver.util.objects.graphs.IGraph;
import org.chocosolver.util.objects.setDataStructures.ISet;
import org.chocosolver.util.objects.setDataStructures.ISetIterator;

import java.util.Iterator;

/**
 * Class containing algorithms to find all connected components and articulation points of graph by performing one dfs
 * it uses Tarjan algorithm in a non recursive way and can be performed in O(M+N) time c.f. Gondrand Minoux
 *
 * @author Jean-Guillaume Fages
 */
public class ConnectivityFinder {

	//***********************************************************************************
	// CONNECTED COMPONENTS ONLY
	//***********************************************************************************

	private int n;
	private IGraph graph;
	private int[] CC_firstNode, CC_nextNode, node_CC, p, fifo;
	private int nbCC;
	//bonus biconnection
	private int[] numOfNode, nodeOfNum, inf;
	private Iterator<Integer>[] iterators;

	/**
	 * Create an object that can compute Connected Components (CC) of a graph g
	 * Can also quickly tell whether g is biconnected or not (only for undirected graph)
	 *
	 * @param g graph
	 */
	public ConnectivityFinder(IGraph g) {
		graph = g;
		n = g.getNbMaxNodes();
		p = new int[n];
		fifo = new int[n];
		iterators = new Iterator[n];
	}

	/**
	 * get the number of CC in g
	 * Beware you should call method findAllCC() first
	 *
	 * @return nbCC the number of CC in g
	 */
	public int getNBCC() {
		return nbCC;
	}

	public int[] getCC_firstNode() {
		return CC_firstNode;
	}

	public int[] getCC_nextNode() {
		return CC_nextNode;
	}

	public int[] getNode_CC() {
		return node_CC;
	}

	/**
	 * Find all connected components of graph by performing one dfs
	 * Complexity : O(M+N) light and fast in practice
	 */
	public void findAllCC() {
		if (node_CC == null) {
			CC_firstNode = new int[n];
			CC_nextNode = new int[n];
			node_CC = new int[n];
		}
		ISet act = graph.getNodes();
		for (int i : act) {
			p[i] = -1;
		}
		for(int i=0;i<CC_firstNode.length;i++){
			CC_firstNode[i] = -1;
		}
		int cc = 0;
		for(int i:act){
			if(p[i]==-1){
				findCC(i, cc);
				cc++;
			}
		}
		nbCC = cc;
	}

	private void findCC(int start, int cc) {
		int first= 0;
		int last = 0;
		fifo[last++] = start;
		p[start] = start;
		add(start,cc);
		while(first<last){
			int i = fifo[first++];
			for(int j:graph.getSuccOrNeighOf(i)){
				if(p[j]==-1){
					p[j] = i;
					add(j,cc);
					fifo[last++] = j;
				}
			}
			if(graph.isDirected()){
				for(int j:graph.getPredOrNeighOf(i)){
					if(p[j]==-1){
						p[j] = i;
						add(j,cc);
						fifo[last++] = j;
					}
				}
			}
		}
	}

	private void add(int node, int cc) {
		node_CC[node] = cc;
		CC_nextNode[node] = CC_firstNode[cc];
		CC_firstNode[cc] = node;
	}

	/**
	 * Test biconnectivity (i.e. connected with no articulation point and no bridge)
	 * only for undirected graphs
	 *
	 * @return true iff g is biconnected
	 */
	public boolean isBiconnected() {
		assert (!graph.isDirected());
		if (inf == null) {
			nodeOfNum = new int[n];
			numOfNode = new int[n];
			inf = new int[n];
		}
		ISet act = graph.getNodes();
		for (int i : act) {
			inf[i] = Integer.MAX_VALUE;
			p[i] = -1;
			iterators[i] = graph.getSuccOrNeighOf(i).iterator();
		}
		//algo
		int start = act.iterator().next();
		int i = start;
		int k = 0;
		numOfNode[start] = k;
		nodeOfNum[k] = start;
		p[start] = start;
		int j, q;
		int nbRootChildren = 0;
		while (true) {
			if (iterators[i].hasNext()) {
				j = iterators[i].next();
				if (p[j] == -1) {
					p[j] = i;
					if (i == start) {
						nbRootChildren++;
						if (nbRootChildren > 1) {
							return false;// ARTICULATION POINT DETECTED
						}
					}
					i = j;
					k++;
					numOfNode[i] = k;
					nodeOfNum[k] = i;
					inf[i] = numOfNode[i];
				} else if (p[i] != j) {
					inf[i] = Math.min(inf[i], numOfNode[j]);
				}
			}else {
				if (i == start) {
					return k >= act.size() - 1;
				}
				q = inf[i];
				i = p[i];
				inf[i] = Math.min(q, inf[i]);
				if (q >= numOfNode[i] && i != start) {
					return false;
				} // ARTICULATION POINT DETECTED
			}
		}
	}

	TIntArrayList articulations;

	/**
	 * Computes articulation points of the graph (must be connected)
	 * @return the list of articulation points
	 */
	public TIntArrayList getArticulationPoints(){
		if(articulations==null) articulations = new TIntArrayList();
		if (inf == null) {
			nodeOfNum = new int[n];
			numOfNode = new int[n];
			inf = new int[n];
		}
		articulations.clear();
		ISet act = graph.getNodes();
		ISetIterator iter = act.iterator();
		while(iter.hasNext()){
			int i = iter.next();
			inf[i] = Integer.MAX_VALUE;
			p[i] = -1;
			iterators[i] = graph.getSuccOrNeighOf(i).iterator();
		}
		//algo
		int start = act.iterator().next();
		int i = start;
		int k = 0;
		numOfNode[start] = k;
		nodeOfNum[k] = start;
		p[start] = start;
		int j, q;
		int nbRootChildren = 0;
		while (true) {
			if (iterators[i].hasNext()) {
				j = iterators[i].next();
				if (p[j] == -1) {
					p[j] = i;
					if (i == start) {
						nbRootChildren++;
						if (nbRootChildren > 1) {
							articulations.add(i); // ARTICULATION POINT DETECTED
						}
					}
					i = j;
					k++;
					numOfNode[i] = k;
					nodeOfNum[k] = i;
					inf[i] = numOfNode[i];
				} else if (p[i] != j) {
					inf[i] = Math.min(inf[i], numOfNode[j]);
				}
			}else {
				if (i == start) {
					if(k < act.size() - 1){
						throw new UnsupportedOperationException("disconnected graph");
					}
					return articulations;
				}
				q = inf[i];
				i = p[i];
				inf[i] = Math.min(q, inf[i]);
				if (q >= numOfNode[i] && i != start) {
					articulations.add(i); // ARTICULATION POINT DETECTED
				}
			}
		}
	}


	public TIntArrayList isthmusFrom, isthmusTo;
	private int[] ND, L, H;

	/**
	 * Only for undirected graphs
	 * @return true iff g is connected
	 */
	public boolean isConnectedAndFindIsthma() {
		assert (!graph.isDirected());
		if (numOfNode == null || CC_firstNode == null) {
			CC_firstNode = new int[n];
			CC_nextNode = new int[n];
			node_CC = new int[n];
			nodeOfNum = new int[n];
			numOfNode = new int[n];
			isthmusFrom = new TIntArrayList();
			isthmusTo = new TIntArrayList();
			ND = new int[n];
			L = new int[n];
			H = new int[n];
		}
		ISet act = graph.getNodes();
		for (int i : act) {
			p[i] = -1;
			iterators[i] = graph.getSuccOrNeighOf(i).iterator();
		}
		for(int i=0;i<CC_firstNode.length;i++){
			CC_firstNode[i] = -1;
		}
		//algo
		int start = act.iterator().next();
		int i = start;
		int k = 0;
		numOfNode[start] = k;
		nodeOfNum[k] = start;
		p[start] = start;
		int j;
		while (true) {
			if (iterators[i].hasNext()) {
				j = iterators[i].next();
				if (p[j] == -1) {
					p[j] = i;
					i = j;
					add(i, 0);
					k++;
					numOfNode[i] = k;
					nodeOfNum[k] = i;
				}
			}else {
				if (i == start) {
					if (k < act.size() - 1) {
						return false;
					} else {
						break;
					}
				}
				i = p[i];
			}
		}
		// POST ORDER PASS FOR FINDING ISTHMUS
		isthmusFrom.clear();
		isthmusTo.clear();
		int currentNode;
		for (i = k; i >= 0; i--) {
			currentNode = nodeOfNum[i];
			ND[currentNode] = 1;
			L[currentNode] = i;
			H[currentNode] = i;
			for (int s : graph.getSuccOrNeighOf(currentNode)) {
				if (p[s] == currentNode) {
					ND[currentNode] += ND[s];
					L[currentNode] = Math.min(L[currentNode], L[s]);
					H[currentNode] = Math.max(H[currentNode], H[s]);
				} else if (s != p[currentNode]) {
					L[currentNode] = Math.min(L[currentNode], numOfNode[s]);
					H[currentNode] = Math.max(H[currentNode], numOfNode[s]);
				}
				if (s != currentNode && p[s] == currentNode && L[s] >= numOfNode[s] && H[s] < numOfNode[s] + ND[s]) {
					isthmusFrom.add(currentNode);
					isthmusTo.add(s);
				}
			}
		}
		return true;
	}
}