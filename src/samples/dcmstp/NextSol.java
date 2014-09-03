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

package samples.dcmstp;

import solver.cstrs.IGraphRelaxation;
import solver.search.strategy.ArcStrategy;
import solver.variables.IUndirectedGraphVar;
import util.objects.setDataStructures.ISet;

/**
 * Heuristic for failing soon to prove optimality
 * Uses edges outside the lagrangian relaxation
 * and tends to create structure by connecting nodes of low degrees
 * <p/>
 * Created by IntelliJ IDEA.
 *
 * @author Jean-Guillaume Fages
 * @since 21/02/13
 */
public class NextSol extends ArcStrategy<IUndirectedGraphVar> {

    //***********************************************************************************
    // VARIABLES
    //***********************************************************************************

    private int n;
    private int[] dMax;
    private IGraphRelaxation relax;

    //***********************************************************************************
    // CONSTRUCTORS
    //***********************************************************************************

    public NextSol(IUndirectedGraphVar g, int[] maxDeg, IGraphRelaxation relaxation) {
        super(g);
        n = maxDeg.length;
        dMax = maxDeg;
        relax = relaxation;
    }

    //***********************************************************************************
    // METHODS
    //***********************************************************************************

    @Override
    public boolean computeNextArc() {
        if (from != -1 && g.getPotNeighOf(from).getSize() != g.getMandNeighOf(from).getSize()) {
            to = -1;
            int i = from;
            ISet nei = g.getPotNeighOf(i);
            for (int j = nei.getFirstElement(); j >= 0; j = nei.getNextElement()) {
                if (!g.getMandNeighOf(i).contain(j)) {
                    if (!relax.contains(i, j))
                        if (dMax[i] == 1 && dMax[j] == 2) {
                            from = i;
                            to = j;
                            return true;
                        }
                }
            }
            for (int j = nei.getFirstElement(); j >= 0; j = nei.getNextElement()) {
                if (!g.getMandNeighOf(i).contain(j)) {
                    if (!relax.contains(i, j))
                        if (dMax[i] == 1 || dMax[j] == 1) {
                            from = i;
                            to = j;
                            return true;
                        }
                }
            }
            for (int j = nei.getFirstElement(); j >= 0; j = nei.getNextElement()) {
                if (!g.getMandNeighOf(i).contain(j)) {
                    if (!relax.contains(i, j))
                        if (dMax[i] == 2 && dMax[j] == 2) {
                            from = i;
                            to = j;
                            return true;
                        }
                }
            }
            for (int j = nei.getFirstElement(); j >= 0; j = nei.getNextElement()) {
                if (!g.getMandNeighOf(i).contain(j)) {
                    if (!relax.contains(i, j))
                        if (dMax[i] == 2 || dMax[j] == 2) {
                            from = i;
                            to = j;
                            return true;
                        }
                }
            }
            for (int j = nei.getFirstElement(); j >= 0; j = nei.getNextElement()) {
                if (!g.getMandNeighOf(i).contain(j)) {
                    if (!relax.contains(i, j)) {
                        from = i;
                        to = j;
                        return true;
                    }
                }
            }
        }
        from = -1;
        to = -1;
        for (int i = 0; i < n; i++) {
            ISet nei = g.getPotNeighOf(i);
            for (int j = nei.getFirstElement(); j >= 0; j = nei.getNextElement()) {
                if (!g.getMandNeighOf(i).contain(j)) {
                    if (!relax.contains(i, j))
                        if (dMax[i] == 1 && dMax[j] == 2) {
                            from = i;
                            to = j;
                            return true;
                        }
                }
            }
        }
        for (int i = 0; i < n; i++) {
            ISet nei = g.getPotNeighOf(i);
            for (int j = nei.getFirstElement(); j >= 0; j = nei.getNextElement()) {
                if (!g.getMandNeighOf(i).contain(j)) {
                    if (!relax.contains(i, j))
                        if (dMax[i] == 1 || dMax[j] == 1) {
                            from = i;
                            to = j;
                            return true;
                        }
                }
            }
        }
        for (int i = 0; i < n; i++) {
            ISet nei = g.getPotNeighOf(i);
            for (int j = nei.getFirstElement(); j >= 0; j = nei.getNextElement()) {
                if (!g.getMandNeighOf(i).contain(j)) {
                    if (!relax.contains(i, j))
                        if (dMax[i] == 2 && dMax[j] == 2) {
                            from = i;
                            to = j;
                            return true;
                        }
                }
            }
        }
        for (int i = 0; i < n; i++) {
            ISet nei = g.getPotNeighOf(i);
            for (int j = nei.getFirstElement(); j >= 0; j = nei.getNextElement()) {
                if (!g.getMandNeighOf(i).contain(j)) {
                    if (!relax.contains(i, j))
                        if (dMax[i] == 2 || dMax[j] == 2) {
                            from = i;
                            to = j;
                            return true;
                        }
                }
            }
        }
        for (int i = 0; i < n; i++) {
            ISet nei = g.getPotNeighOf(i);
            for (int j = nei.getFirstElement(); j >= 0; j = nei.getNextElement()) {
                if (!g.getMandNeighOf(i).contain(j)) {
                    if (!relax.contains(i, j)) {
                        from = i;
                        to = j;
                        return true;
                    }
                }
            }
        }
        return false;
    }
}