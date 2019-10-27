package lklee.stats.computer.JT.type;


import java.util.BitSet;

public class CliquePair {
	public final BitSet c1;
	public final BitSet c2;
	public final BitSet union;

	public CliquePair(BitSet c1, BitSet c2) {
		this.c1 = c1;
		this.c2 = c2;
		this.union = (BitSet) c1.clone();
		this.union.or(c2);
	}

	@Override
	public int hashCode() {
		return this.union.hashCode();
	}
}

