package lklee.stats.computer.JT.type;

import java.util.Arrays;
import java.util.BitSet;

public class SimpleFactor {

	private double[] factor;
	public final int[] nbValsForAtt;
	public final BitSet vars;
	public final int[] varIndices;
	public final int[] varBases;
	public final int targetFactorLength;

	public SimpleFactor(int[] nbValsForAtt, double[] factor, BitSet vars) {
		this.nbValsForAtt = nbValsForAtt;
		if (factor != null) {
			this.factor = factor.clone();
		}
		this.factor = factor;
		this.vars = vars;
		this.varIndices = vars.stream().toArray();
		this.varBases = getVarBases(vars, nbValsForAtt);
		this.targetFactorLength = getTargetFactorLength(vars, nbValsForAtt);
	}

	public String toString() {
		return Arrays.toString(this.factor);
	}

	public SimpleFactor clone() {
		return new SimpleFactor(this.nbValsForAtt, this.factor.clone(), this.vars);
	}

	private static int[] getVarBases(BitSet vars, int[] nbValsForAtt) {
		int[] bases = new int[vars.size()];
		int varIdx = vars.previousSetBit(vars.length());
		int base = 1;
		while (varIdx >= 0) {
			bases[varIdx] = base;
			base = base * nbValsForAtt[varIdx];
			varIdx = vars.previousSetBit(varIdx - 1);
		}
		return bases;
	}

	private static int getTargetFactorLength(BitSet vars, int[] nbValsForAtt) {
		int length = 1;
		int varIdx = vars.previousSetBit(vars.length());
		while (varIdx >= 0) {
			length *= nbValsForAtt[varIdx];
			varIdx = vars.previousSetBit(varIdx - 1);
		}
		return length;
	}

	public double[] getFactor() {
		return factor;
	}

	private void updateFactor(double[] factor) {
		if (factor.length != this.targetFactorLength) {
			throw new Error("Factor length is not correct");
		}
		this.factor = factor.clone();
	}

	public BitSet getVars() {
		return vars;
	}

	public SimpleFactor multiplyByFactor(SimpleFactor newSimpleFactor) {
		double[] newFactor = newSimpleFactor.getFactor();
		BitSet newVars = newSimpleFactor.getVars();
		this.multiplyByFactor(newFactor, newVars);
		return this;
	}

	/**
	 * @param newFactor Factor to multiply into accFactor
	 * @param newVars   variables in newFactor that is subset of accVars
	 * @return accFactor * newFactor with the domain accVars
	 */
	public SimpleFactor multiplyByFactor(
		double[] newFactor, BitSet newVars
	) {
		int[] radixAcc = new int[this.vars.size()];
		int idxCombNew = 0;
		for (int idxCombAcc = 0; idxCombAcc < this.factor.length; idxCombAcc++) {
			// multiply factors
			this.factor[idxCombAcc] *= newFactor[idxCombNew];
			// increment radixAcc
			idxCombNew = this.incSubDomIndex(radixAcc, idxCombNew, newVars);
			this.incRadix(radixAcc);
		}
		return this;
	}

	public static SimpleFactor multiply2Factors(SimpleFactor factor1, SimpleFactor factor2) {
		BitSet finalDomain = (BitSet) factor1.vars.clone();
		finalDomain.or(factor2.vars);
		SimpleFactor finalFactor = new SimpleFactor(
				factor1.nbValsForAtt, null, finalDomain
		);
		double[] factor = new double[finalFactor.targetFactorLength];
		int[] finalRadix = new int[finalDomain.size()];
		int idxF1 = 0;
		int idxF2 = 0;
		for (int idxFinal = 0; idxFinal < factor.length; idxFinal++) {
			factor[idxFinal] = factor1.factor[idxF1] * factor2.factor[idxF2];
			idxF1 = finalFactor.incSubDomIndex(finalRadix, idxF1, factor1.vars);
			idxF2 = finalFactor.incSubDomIndex(finalRadix, idxF2, factor2.vars);
			finalFactor.incRadix(finalRadix);
		}
		/*
		SimpleFactor finalFactor = extendFactorDomain(factor1, factor2.vars);
		finalFactor.multiplyByFactor(factor2);
		 */
		finalFactor.updateFactor(factor);
		return finalFactor;
	}

	public static SimpleFactor extendFactorDomain(SimpleFactor oldFactor, BitSet addDomain) {
		// get New Domain
		BitSet newDomain = (BitSet) oldFactor.vars.clone();
		newDomain.or(addDomain);
		// get Vars in addDomain not in oldFactor
		BitSet additionalVars = (BitSet) addDomain.clone();
		additionalVars.andNot(oldFactor.vars);
		// get number of additional values per value in oldFactor
		int nAdditionalVal = getTargetFactorLength(additionalVars, oldFactor.nbValsForAtt);
		// create factor with domain of newDomain that marginalises to oldFactor
		SimpleFactor newFactor = new SimpleFactor(oldFactor.nbValsForAtt, null, newDomain);
		double[] newDist = new double[newFactor.targetFactorLength];
		int[] newRadix = new int[newDomain.size()];
		int oldFactorIdx = 0;
		for (int newFactorIdx = 0; newFactorIdx < newDist.length; newFactorIdx++) {
			newDist[newFactorIdx] = oldFactor.factor[oldFactorIdx];
			oldFactorIdx = newFactor.incSubDomIndex(newRadix, oldFactorIdx, oldFactor.vars);
			newFactor.incRadix(newRadix);
		}
		newFactor.updateFactor(newDist);
		return newFactor;
	}

	public SimpleFactor divideByFactor(SimpleFactor newSimpleFactor) {
		double[] newFactor = newSimpleFactor.getFactor();
		BitSet newVars = newSimpleFactor.getVars();
		this.divideByFactor(newFactor, newVars);
		return this;
	}

	/**
	 * @param newFactor Factor to divide into accFactor
	 * @param newVars   variables in newFactor that is subset of accVars
	 * @return accFactor * newFactor with the domain accVars
	 */
	public SimpleFactor divideByFactor(
		double[] newFactor, BitSet newVars
	) {
		double[] newFactorDiv = new double[newFactor.length];
		for (int i = 0; i < newFactor.length; i++) {
			newFactorDiv[i] = newFactor[i] == 0 ? 0 : 1 / newFactor[i];
		}
		return multiplyByFactor(newFactorDiv, newVars);
	}

	// TODO: this method needs more testing on different positions of the 1 att margin in 3 att
	public double[] marginaliseVariables(BitSet marginVars) {
		BitSet mVarsAnd = (BitSet) this.vars.clone();
		mVarsAnd.and(marginVars);
		if (!mVarsAnd.equals(marginVars)) {
			throw new Error("Margin is not a subset of variables in factor");
		}
		int nMarginVal = marginVars.stream()
			.map( idx -> this.nbValsForAtt[idx] )
			.reduce( 1, (acc, nbVal) -> acc*nbVal );
		double[] newFactorVals = new double[nMarginVal];

		int[] radixAcc = new int[this.vars.size()];
		int newIdx = 0;
		for (int i = 0; i < this.factor.length; i++) {
			newFactorVals[newIdx] += factor[i];
			// increment radix number by 1
			newIdx = this.incSubDomIndex(radixAcc, newIdx, marginVars);
			this.incRadix(radixAcc);
		}
		// Sanity Check
		double sumA = 0.0;
		double sumB = 0.0;
		for (double val : this.factor) sumA += val;
		for (double val : newFactorVals) sumB += val;
		if (Math.abs(sumA - sumB) > 0.001) {
			//System.out.println("Sum not consistent");
			throw new Error("Sum not consistent");
		}
		return newFactorVals;
	}

	/**
	 * @param radix Radix of the current SimpleFactor
	 * @return
	 */
	private int[] incRadix(int[] radix) {
		for (int j = this.varIndices.length - 1; j >= 0; j--) {
			int varIdx = this.varIndices[j];
			int varVal = radix[varIdx];
			int maxVal = this.nbValsForAtt[varIdx] - 1;
			if (varVal + 1 > maxVal) {
				radix[varIdx] = 0;
			} else {
				radix[varIdx] += 1;
				break;
			}
		}
		return radix;
	}

	private int incSubDomIndex(int[] radix, int subDomIdx, BitSet subDom) {
		int base = 1;
		for (int j = this.varIndices.length - 1; j >= 0; j--) {
			int varIdx = this.varIndices[j];
			int varVal = radix[varIdx];
			int maxVal = this.nbValsForAtt[varIdx] - 1;
			if (varVal + 1 > maxVal) {
				if (subDom.get(varIdx)) {
					subDomIdx -= varVal * base;
				}
			} else {
				if (subDom.get(varIdx)) {
					subDomIdx += base;
				}
				break;
			}
			if (subDom.get(varIdx)) {
				base = base * this.nbValsForAtt[varIdx];
			}
		}
		return subDomIdx;
	}
}
