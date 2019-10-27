package lklee.stats.computer.JT.type;

import java.util.Arrays;
import java.util.BitSet;

public class JTComputerVertex {
	private final BitSet clique;
	private double[] factor;

	public JTComputerVertex(BitSet clique, double[] factor) {
		this.clique = clique;
		this.factor = factor;
	}

	public JTComputerVertex(BitSet clique) {
		this.clique = clique;
		this.factor = null;
	}

	@Override
	public int hashCode() {
		return this.clique.hashCode();
	}

	@Override
	public String toString() {
		return this.clique.toString() + ": " + Arrays.toString(this.factor);
	}

	@Override
	public boolean equals(Object o) {
		return (o instanceof JTComputerVertex) && (hashCode() == o.hashCode());
	}

	public double[] getFactor() {
		return factor;
	}

	public void setFactor(double[] factor) {
		this.factor = factor;
	}

}
