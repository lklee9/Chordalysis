package lklee.stats.computer.JT.factor;

import core.lattice.Lattice;
import core.lattice.LatticeNode;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.function.DoubleUnaryOperator;

public abstract class FactorComputer {
	protected Lattice lattice;

	abstract public double[] oldCliqueFactor(BitSet vars);
	abstract public double[] oldSepFactor(BitSet vars);
	abstract public double[] newCliqueFactor(BitSet vars);
	abstract public double[] newSepFactor(BitSet vars);

	public Map<BitSet, double[]> factorCacheClique;
	public Map<BitSet, double[]> factorCacheSep;

	public FactorComputer(Lattice lattice) {
		this.lattice = lattice;
		this.factorCacheClique = new HashMap<>();
		this.factorCacheSep = new HashMap<>();
	}

	public int[] getNbValuesForAttribute() {
		return this.lattice.getnbValuesForAttribute();
	}

	public double[] funcOfProbabilities(BitSet vars, DoubleUnaryOperator func) {
		LatticeNode node = this.lattice.getNode(vars);
		double n = this.lattice.getNbInstances();
		int nCombinations = node.getNbCells();
		double[] funcOfProb = new double[nCombinations];
		for (int i = 0; i < nCombinations; i++) {
			double prob = (double)node.getMatrixCell(i) / n;
			funcOfProb[i] = func.applyAsDouble(prob);
		}
		return funcOfProb;
	}

	public double[] funcOfFrequencies(BitSet vars, DoubleUnaryOperator func) {
		LatticeNode node = this.lattice.getNode(vars);
		int nCombinations = node.getNbCells();
		double[] funcOfFreq = new double[nCombinations];
		for (int i = 0; i < nCombinations; i++) {
			double count = node.getMatrixCell(i);
			funcOfFreq[i] = func.applyAsDouble(count);
		}
		return funcOfFreq;
	}

	public double[] staticFactorClique(BitSet clique) {
		if (this.factorCacheClique.containsKey(clique)) {
			return this.factorCacheClique.get(clique).clone();
		} else {
			double[] oldFactor = this.oldCliqueFactor(clique);
			double[] newFactor = this.newCliqueFactor(clique);
			if (oldFactor.length != newFactor.length)
				throw new Error("old and new initial factor not same length");
			for (int i = 0; i < oldFactor.length; i++) {
				oldFactor[i] = oldFactor[i] * newFactor[i];
			}
			this.factorCacheClique.put(clique, oldFactor);
			return oldFactor.clone();
		}
	}

	public double[] staticFactorSep(BitSet sep) {
		if (this.factorCacheSep.containsKey(sep)) {
			return this.factorCacheSep.get(sep).clone();
		} else {
			double[] oldFactor = this.oldSepFactor(sep);
			double[] newFactor = this.newSepFactor(sep);
			if (oldFactor.length != newFactor.length)
				throw new Error("old and new initial factor not same length");
			for (int i = 0; i < oldFactor.length; i++) {
				oldFactor[i] = oldFactor[i] * newFactor[i];
			}
			this.factorCacheSep.put(sep, oldFactor);
			return oldFactor.clone();
		}
	}

}
