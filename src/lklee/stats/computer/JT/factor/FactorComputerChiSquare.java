package lklee.stats.computer.JT.factor;

import core.lattice.Lattice;

import java.util.BitSet;

public class FactorComputerChiSquare extends FactorComputer {

	public FactorComputerChiSquare(Lattice lattice) {
		super(lattice);
	}

	@Override
	public double[] oldCliqueFactor(BitSet vars) {
		return this.funcOfProbabilities(vars, (x) -> x == 0 ? 0 : 1/x);
	}

	@Override
	public double[] oldSepFactor(BitSet vars) {
		return this.funcOfProbabilities(vars, (x) -> x);
	}

	@Override
	public double[] newCliqueFactor(BitSet vars) {
		return this.funcOfProbabilities(vars, (x) -> Math.pow(x, 2));
	}

	@Override
	public double[] newSepFactor(BitSet vars) {
		return this.funcOfProbabilities(vars, (x) -> x == 0 ? 0 : 1 / Math.pow(x, 2));
	}

}
