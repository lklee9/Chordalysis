package lklee.stats.computer;

import core.lattice.Lattice;

public class ChiSquareComputer {
    Lattice lattice;
    int nbInstances;

    public ChiSquareComputer(Lattice lattice) {
        this.lattice = lattice;
        this.nbInstances = this.lattice.getNbInstances();
    }

    public Lattice getLattice() {
        return this.lattice;
    }
}
