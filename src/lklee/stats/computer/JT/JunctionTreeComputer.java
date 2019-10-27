package lklee.stats.computer.JT;

import core.graph.CliqueGraphEdge;
import core.model.DecomposableModel;
import lklee.stats.computer.JT.type.CliquePair;
import lklee.stats.computer.JT.type.JTComputerEdge;
import lklee.stats.computer.JT.type.SimpleFactor;
import lklee.stats.computer.JT.factor.FactorComputer;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;
import org.jgrapht.graph.SimpleGraph;

import java.util.*;
import java.util.stream.Collectors;

public class JunctionTreeComputer {
	// param vars
	protected FactorComputer computer;

	// Internal vars
	protected int[] nbValsForAtt;
	protected int nbInstances;
	protected BitSet rootNode;
	// Store only messages in Junction Tree
	protected SimpleGraph<BitSet, CliqueGraphEdge> JT;
	protected SimpleDirectedWeightedGraph<BitSet, JTComputerEdge> computationJT;
	protected Map<BitSet, Integer> cliqueLevel;
	protected Map<BitSet, SimpleFactor> cliqueBelief;
	protected Map<CliquePair, SimpleFactor> cliquePairBelief;

	public JunctionTreeComputer(FactorComputer computer) {
		// Get junction tree starting from root node
		// Create list of cliques based on level in tree
		// Belief propagation and storage
		this.computer = computer;
	}

	public JunctionTreeComputer(
			FactorComputer computer,
			SimpleGraph<BitSet, CliqueGraphEdge> junctionTree
	) {
		this.computer = computer;
		this.updateTree(junctionTree);
	}

	public void updateGraph(DecomposableModel model) {
		// Update this.graph
		// Propagate changes
		// DO NOT recreate whole computation graph
		// Don't need to recompute msgs towards clique changes
		this.updateTree(model.graph.getJunctionTree());
	}

	public void updateTree(SimpleGraph<BitSet, CliqueGraphEdge> junctionTree) {
		this.JT = junctionTree;
		this.computationJT = new SimpleDirectedWeightedGraph<>(JTComputerEdge.class);
		this.rootNode = this.findBestRoot(JT);
		this.nbValsForAtt = this.computer.getNbValuesForAttribute();
		cliqueLevel = new HashMap<>();
		cliqueBelief = new HashMap<>();
		cliquePairBelief = new HashMap<>();
		this.propagateUpThenDown(this.rootNode);
	}

	public SimpleFactor getCliqueBelief
			(BitSet clique) {
		return this.cliqueBelief.get(clique);
	}

	private BitSet findBestRoot(SimpleGraph<BitSet, CliqueGraphEdge> jt) {
		// TODO: Determine if there are any benefits to select a non random root
		// TODO: ie. if a balanced tree is faster
		return jt.vertexSet().stream().findFirst().orElseThrow(Error::new);
	}


	private List<BitSet> findLeaves() {
		return this.JT.vertexSet().stream()
				.filter( c -> this.JT.degreeOf(c) == 1)
				.collect(Collectors.toList());
	}

	private BitSet parentOf(BitSet clique) {
		return this.computationJT.incomingEdgesOf(clique).stream()
				.map(e -> e.parent)
				.findAny()
				.orElseThrow(() -> new Error("No parent"));
	}

	private List<BitSet> getNextClique(BitSet parent, BitSet current) {
		return this.JT.edgesOf(current).stream()
				.map( e ->
						e.getClique1().equals(current) ? e.getClique2() : e.getClique1()
				)
				.filter( c -> !c.equals(parent) )
				.collect(Collectors.toList());
	}

	private void propagateUpThenDown(BitSet rootClique) {
		this.computationJT.addVertex(rootClique);
		this.cliqueBelief.put(rootClique,
				new SimpleFactor(
						this.nbValsForAtt,
						this.computer.staticFactorClique(rootClique),
						rootClique
				)
		);
		this.cliqueLevel.put(rootClique, 0);
		List<BitSet> nextCliques = this.getNextClique(null, rootClique);
		for (BitSet clique : nextCliques) {
			this.propagateUp(clique, rootClique);
		}

		double sum = Arrays.stream(this.cliqueBelief.get(rootClique).getFactor()).sum();
		for (BitSet child : nextCliques) {
			this.propagateDown(rootClique, child);
		}
		if (this.computationJT.vertexSet().size() != this.JT.vertexSet().size()) {
			throw new Error("Clique Tree and JT not consistent");
		}
	}

	private void propagateUp(BitSet current, BitSet parent) {
		this.computationJT.addVertex(current);
		this.cliqueBelief.put(current,
				new SimpleFactor(
						this.nbValsForAtt, this.computer.staticFactorClique(current), current
				)
		);
		this.cliqueLevel.put(current, this.cliqueLevel.get(parent) + 1);
		this.computationJT.addEdge(parent, current, new JTComputerEdge(parent, current));
		// Explore down
		List<BitSet> nextCliques = this.getNextClique(parent, current);
		if (nextCliques.size() > 0) {
			for (BitSet child : nextCliques) {
				this.propagateUp(child, current);
			}
		}
		this.propagate(current, parent);
		double sum = Arrays.stream(this.cliqueBelief.get(parent).getFactor()).sum();
		sum = Arrays.stream(this.cliqueBelief.get(parent).getFactor()).sum();
		return;
	}

	private void propagateDown(BitSet parent, BitSet current) {
		propagate(parent, current);
		double sum = Arrays.stream(this.cliqueBelief.get(current).getFactor()).sum();
		List<BitSet> nextCliques = this.getNextClique(parent, current);
		if (nextCliques.size() >= 1) {
			for (BitSet child : nextCliques) {
				propagateDown(current, child);
			}
		}
	}

	private void propagate(BitSet from, BitSet to) {
		JTComputerEdge edge = this.getEdge(from, to);
		// Get msg out from "from" clique
		SimpleFactor fromBelief = this.cliqueBelief.get(from).clone();
		fromBelief.multiplyByFactor(
				this.computer.staticFactorSep(edge.getSeparator()), edge.getSeparator()
		);
		double[] msg = fromBelief.marginaliseVariables(edge.getSeparator());

		// Multiply msg into "to" clique
		SimpleFactor toBelief = this.cliqueBelief.get(to);
		toBelief.multiplyByFactor(msg, edge.getSeparator());
		if (edge.getFactor() != null) {
			toBelief.divideByFactor(edge.getFactor(), edge.getSeparator());
		}
		// Update separator belief
		edge.setFactor(msg);
	}

	private JTComputerEdge getEdge(BitSet clique1, BitSet clique2) {
		return this.computationJT.containsEdge(clique1, clique2) ?
				this.computationJT.getEdge(clique1, clique2) :
				this.computationJT.getEdge(clique2, clique1);
	}

	private List<BitSet> getPathInTree(BitSet clique1, BitSet clique2) {
		List<BitSet> clique1ToRoot = new ArrayList<>();
		List<BitSet> clique2ToRoot = new ArrayList<>();

		int level1 = this.cliqueLevel.get(clique1);
		int level2 = this.cliqueLevel.get(clique2);
		while(level1 != level2) {
			if (level1 > level2) {
				clique1ToRoot.add(clique1);
				clique1 = this.parentOf(clique1);
				level1 -= 1;
			} else {
				clique2ToRoot.add(clique2);
				clique2 = this.parentOf(clique2);
				level2 -= 1;
			}
		}
		while (!clique1.equals(clique2)) {
			clique1ToRoot.add(clique1);
			clique1 = this.parentOf(clique1);
			clique2ToRoot.add(clique2);
			clique2 = this.parentOf(clique2);
		}
		if (!clique1.equals(clique2)) {
			throw new Error("No common ancestor found");
		}
		clique1ToRoot.add(clique1);
		for (int i = clique2ToRoot.size() - 1; i >= 0; i--) {
			clique1ToRoot.add(clique2ToRoot.get(i));
		}
		return clique1ToRoot;
	}

	// TODO: Write proof based on division removing branches of info
	public SimpleFactor jointFactor(BitSet clique1, BitSet clique2) {
		List<BitSet> path = this.getPathInTree(clique1, clique2);
		SimpleFactor jointFactor = this.getJointFactorBetweenEnds(path);
		return jointFactor;
	}

	private SimpleFactor getJointFactorBetweenEnds(List<BitSet> path) {
		if (path.size() <= 1) {
			throw new Error("Path is not a path");
		}
		BitSet Ci = path.get(0);
		BitSet Cj = path.get(path.size() - 1);
		BitSet Cl = path.get(path.size() - 2);
		CliquePair pair = new CliquePair(Ci, Cj);
		if (this.cliquePairBelief.containsKey(pair)) {
			return this.cliquePairBelief.get(pair).clone();
		}
		if (path.size() == 2) {
			JTComputerEdge CiToCj = this.getEdge(Ci, Cj);
			SimpleFactor cjBelief = this.cliqueBelief.get(Cj).clone();
			SimpleFactor jointFactor = SimpleFactor.multiply2Factors(
					cjBelief.divideByFactor(CiToCj.getFactor(), CiToCj.getSeparator()),
					this.cliqueBelief.get(Ci)
			);
			// No Separator information left, so multiply
			jointFactor.multiplyByFactor(
					this.computer.staticFactorSep(CiToCj.getSeparator()),
					CiToCj.getSeparator()
			);
			this.cliquePairBelief.put(pair, jointFactor);
		} else {
			SimpleFactor CjUnionCl = this.getJointFactorBetweenEnds(
					path.subList(path.size() - 2, path.size())
			);
			SimpleFactor CjGivenCl = CjUnionCl.clone().divideByFactor(this.cliqueBelief.get(Cl));
			SimpleFactor CiUnionCl = this.getJointFactorBetweenEnds(
					path.subList(0, path.size() - 1)
			);
			SimpleFactor CiCjCl = SimpleFactor.multiply2Factors(
					CiUnionCl, CjGivenCl
			);
			double[] jointFactor = CiCjCl.marginaliseVariables(pair.union);
			double sum = Arrays.stream(jointFactor).sum();
			//if (Math.abs(1.0 - sum) > 0.000001) throw new Error("Factor does not sum to 1");
			this.cliquePairBelief.put(pair,
					new SimpleFactor(this.nbValsForAtt, jointFactor, pair.union)
			);
		}
		return this.cliquePairBelief.get(pair).clone();
	}

	public double sumBelief() {
		return this.cliqueBelief
				.get(this.rootNode)
				.marginaliseVariables(new BitSet())[0];
	}

}
