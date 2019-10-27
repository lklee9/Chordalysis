package lklee.stats.computer.JT.type;

import org.jgrapht.graph.DefaultEdge;

import java.util.BitSet;

public class JTComputerEdge extends DefaultEdge {
    private BitSet separator;
    // factor domain is the domain of separator
    private double[] factor;

    public final BitSet parent;
    public final BitSet child;

    public JTComputerEdge(BitSet parent, BitSet child, double[] factor) {
        super();
        this.parent = parent;
        this.child = child;
        this.separator = (BitSet)(this.parent).clone();
        this.separator.and(this.child);
        this.factor = factor;
    }

    public JTComputerEdge(BitSet parent, BitSet child) {
    	  super();
        this.parent = parent;
        this.child = child;
        this.separator = (BitSet)(this.parent).clone();
        this.separator.and(this.child);
        this.factor = null;
    }

    public BitSet getSeparator() {
        return separator;
    }

    public double[] getFactor() {
        return factor;
    }

    public void setFactor(double[] factor) {
        this.factor = factor;
    }
}
