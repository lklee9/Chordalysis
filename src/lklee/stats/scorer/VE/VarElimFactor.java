package lklee.stats.scorer.VE;

import core.lattice.Lattice;
import core.lattice.LatticeNode;

import java.util.BitSet;
import java.util.List;
import java.util.function.DoubleUnaryOperator;

public class VarElimFactor {
    public final int[] nbValuesForAttribute;
    public final BitSet variables;
    public final double[] factorValues;
    public final int[] varBase;

    public VarElimFactor(Lattice orgLattice, BitSet variables, DoubleUnaryOperator op) {
        LatticeNode node = orgLattice.getNode(variables);
        int nCombinations = node.getNbCells();
        double[] factorVals = new double[nCombinations];
        // Left most variable has largest base
        // TODO: Move smoothing and making to prob to Scorer
        double min = 1.0 / (double)orgLattice.getNbInstances();
        double debugSum = 0.0;
        for (int i = 0; i < nCombinations; i++) {
            double count = node.getMatrixCell(i);
            //double p = (1 + count) / (orgLattice.getNbInstances() + nCombinations);
            double p = count / orgLattice.getNbInstances();
            debugSum += p;
            factorVals[i] = op.applyAsDouble(p);
        }
        if (Math.abs(debugSum - 1) > 0.00001) {
            throw new Error("Not a prob vec");
        }
        this.nbValuesForAttribute = orgLattice.getnbValuesForAttribute();
        this.variables = variables;
        this.factorValues = factorVals;
        this.varBase = this.getVarBases(variables);
    }

    public VarElimFactor(Lattice orgLattice, BitSet variables, double[] factorValues) {
        this.nbValuesForAttribute = orgLattice.getnbValuesForAttribute();
        this.variables = variables;
        this.factorValues = factorValues;
        this.varBase = this.getVarBases(variables);
    }

    public VarElimFactor(int[] nbValuesForAttribute, BitSet vars, double[] factorValues) {
        this.nbValuesForAttribute = nbValuesForAttribute;
        this.variables = vars;
        this.factorValues = factorValues;
        this.varBase = this.getVarBases(vars);
    }

    private int[] getVarBases(BitSet vars) {
        int[] bases = new int[vars.size()];
        int varIdx = vars.previousSetBit(vars.length());
        int base = 1;
        while (varIdx >= 0) {
            bases[varIdx] = base;
            base = base * this.nbValuesForAttribute[varIdx];
            varIdx = this.variables.previousSetBit(varIdx - 1);
        }
        return bases;
    }

    // TODO: this method needs more testing on different positions of the 1 att margin in 3 att
    public VarElimFactor marginaliseVariables(BitSet marginVars) {
        BitSet mVarsAnd = (BitSet)this.variables.clone();
        mVarsAnd.and(marginVars);
        BitSet newVars = (BitSet)this.variables.clone();

        int varIdx = this.variables.nextSetBit(0);
        int[] radixIdxToVarIdx = new int[this.variables.cardinality()];
        int idx = 0;
        while (varIdx >= 0) {
            radixIdxToVarIdx[idx] = varIdx;
            varIdx = this.variables.nextSetBit(varIdx + 1);
            idx++;
        }
        varIdx = mVarsAnd.nextSetBit(0);
        int nMarginVal = 1;
        while (varIdx >= 0) {
            nMarginVal = nMarginVal * this.nbValuesForAttribute[varIdx];
            newVars.clear(varIdx);
            varIdx = mVarsAnd.nextSetBit(varIdx + 1);
        }
        double[] newFactorVals = new double[factorValues.length / nMarginVal];

        int[] radix = new int[this.variables.cardinality()];
        int newIdx = 0;
        for (int i = 0; i < this.factorValues.length; i++) {
            newFactorVals[newIdx] = newFactorVals[newIdx] + factorValues[i];
            // increment radix number by 1
            int base = 1;
            for (int j = radix.length - 1; j >= 0; j--) {
                int curVarIdx = radixIdxToVarIdx[j];
                int curVal = radix[j];
                int maxVal = this.nbValuesForAttribute[curVarIdx] - 1;
                if (curVal + 1 > maxVal) {
                    if (!marginVars.get(curVarIdx)) {
                        newIdx = newIdx - (curVal * base);
                    }
                    radix[j] = 0;
                } else {
                    radix[j] = radix[j] + 1;
                    if (!marginVars.get(curVarIdx)) {
                        newIdx = newIdx + base;
                    }
                    break;
                }
                if (!marginVars.get(curVarIdx)) {
                    base = base * this.nbValuesForAttribute[curVarIdx];
                }
            }
        }
        // Sanity Check
        double sumA = 0.0;
        double sumB = 0.0;
        for (double val : this.factorValues) sumA += val;
        for (double val : newFactorVals) sumB += val;
        if (Math.abs(sumA - sumB) > 0.00001) {
            //System.out.println("Sum not consistent");
            throw new Error("Sum not consistent");
        }
        return new VarElimFactor(
            this.nbValuesForAttribute,
            newVars,
            newFactorVals
        );
    }

    public static VarElimFactor factorProduct(List<VarElimFactor> factorList) {
        if (factorList.size() == 1) return factorList.get(0);
        BitSet allVars = new BitSet();
        int[] nbValuesForAttribute = factorList.get(0).nbValuesForAttribute;
        for (int i = 0; i < factorList.size(); i++) {
            VarElimFactor factor = factorList.get(i);
            allVars.or(factor.variables);
        }
        // Get radix idx to var idx mapping & n combinations
        int nComb = 1;
        int[] radixIdxToVarIdx = new int[allVars.cardinality()];
        int varIdx = allVars.nextSetBit(0);
        int i = 0;
        while (varIdx >= 0) {
            radixIdxToVarIdx[i] = varIdx;
            nComb = nComb * nbValuesForAttribute[varIdx];
            i++;
            varIdx = allVars.nextSetBit(varIdx + 1);
        }
        // Cache factor value indices for each factor
        int[] factorValueIndices = new int[factorList.size()];
        // Get factor value for each attribute value combination
        int[] varValues = new int[nbValuesForAttribute.length];
        double[] factorValuesProd = new double[nComb];
        // TODO: Check if iterating through previous factor variables
        for (i = 0; i < nComb; i++) {
            double factorValue = 1.0;
            for (int j = 0; j < factorList.size(); j++) {
                VarElimFactor factor = factorList.get(j);
                int factorValIdx = factorValueIndices[j];
                factorValue = factorValue * factor.factorValues[factorValIdx];
            }
            factorValuesProd[i] = factorValue;
            for (int j = radixIdxToVarIdx.length - 1; j >= 0; j--) {
                int curVarIdx = radixIdxToVarIdx[j];
                int curVarVal = varValues[curVarIdx];
                int maxVal = nbValuesForAttribute[curVarIdx] - 1;
                if (curVarVal + 1 > maxVal) {
                    int newVal = 0;
                    factorValueIndices = getNewFactorValIdx(
                        factorList, factorValueIndices, varValues, curVarIdx, newVal
                    );
                    varValues[curVarIdx] = newVal;
                } else {
                    int newVal = varValues[curVarIdx] + 1;
                    factorValueIndices = getNewFactorValIdx(
                        factorList, factorValueIndices, varValues, curVarIdx, newVal
                    );
                    varValues[curVarIdx] = newVal;
                    break;
                }
            }
        }
        return new VarElimFactor(
            nbValuesForAttribute,
            allVars,
            factorValuesProd
        );
    }

    private static int[] getNewFactorValIdx(
        List<VarElimFactor> factors, int[] curFactorValIndices,
        int[] curVarValues, int varIdx, int newVarVal
    ) {
        int[] newFactorValIndices = curFactorValIndices;
        int curVarVal = curVarValues[varIdx];
        for (int i = 0; i < factors.size(); i++) {
            VarElimFactor factor = factors.get(i);
            // TODO: Check if this.varBase[varIdx] = 0 if varIdx in not in factor
            int base = factor.varBase[varIdx];
            int curIdxAddition = base * curVarVal;
            int newIdxAddition = base * newVarVal;
            newFactorValIndices[i] += newIdxAddition - curIdxAddition;
        }
        return newFactorValIndices;
    }

    /*
    public double getFactorValue(int[] varValues) {
        int base = 1;
        int idx = 0;
        int varIdx = this.variables.previousSetBit(this.variables.length());
        while (varIdx >= 0) {
            int varVal = varValues[varIdx];
            if (varVal < 0) {
                throw new Error("Variable Value not set");
            }
            idx = idx + (base * varVal);
            base = base * this.nbValuesForAttribute[varIdx];
            varIdx = this.variables.previousSetBit(varIdx - 1);
        }
        return this.factorValues[idx];
    }
     */
}
