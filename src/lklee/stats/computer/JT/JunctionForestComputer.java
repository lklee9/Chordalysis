package lklee.stats.computer.JT;

import core.graph.ChordalGraph;
import core.graph.CliqueGraphEdge;
import core.model.DecomposableModel;
import lklee.stats.computer.JT.type.SimpleFactor;
import lklee.stats.computer.JT.factor.FactorComputer;
import lklee.stats.EdgeAddingOracle;
import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.alg.KruskalMinimumSpanningTree;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleGraph;

import java.util.*;

public class JunctionForestComputer {
	public final FactorComputer computer;

	ChordalGraph graph;
	List<JunctionTreeComputer> forest;
	HashMap<BitSet, Integer> forestIdxCliqueIsIn;
	EdgeAddingOracle oracle;
	ConnectivityInspector<BitSet, CliqueGraphEdge> inspector;

	public JunctionForestComputer(FactorComputer computer) {
		this.computer = computer;
	}

	public void initJunctionForest(DecomposableModel model) {
		this.forest = new ArrayList<>();
		this.forestIdxCliqueIsIn = new HashMap<>();
		this.graph = (ChordalGraph)model.graph.clone();
		this.oracle = new EdgeAddingOracle(this.graph);
		// TODO: Make Inspector use graph instead of JT
		this.inspector = new ConnectivityInspector<>(this.graph.getJunctionTree());
		this.forest = this.createForest();
	}

	public void addEdgeToForest(Integer n1, Integer n2) {
		// Remove old non-existent cliques
		BitSet oldC1 = this.oracle.getCa(n1, n2);
		BitSet oldC2 = this.oracle.getCb(n1, n2);
		int oldTreeIdx1 = this.forestIdxCliqueIsIn.get(oldC1);
		int oldTreeIdx2 = this.forestIdxCliqueIsIn.get(oldC2);
		if (oldTreeIdx1 > oldTreeIdx2) {
			int tmp = oldTreeIdx1;
			oldTreeIdx1 = oldTreeIdx2;
			oldTreeIdx2 = tmp;
			oldC1 = this.oracle.getCb(n1, n2);
			oldC2 = this.oracle.getCa(n1, n2);
		}
		if (oldTreeIdx1 != oldTreeIdx2) {
			// Move cliques in tree 2 to tree 1 and delete tree 1
			Set<BitSet> oldCliquesInTree2 = this.inspector.connectedSetOf(oldC2);
			for (BitSet clique : oldCliquesInTree2) {
				this.forestIdxCliqueIsIn.put(clique, oldTreeIdx1);
			}
			this.forest.remove(oldTreeIdx2);
			for (BitSet clique : this.forestIdxCliqueIsIn.keySet()) {
				int oldIdx = this.forestIdxCliqueIsIn.get(clique);
				if (oldIdx > oldTreeIdx2) {
					this.forestIdxCliqueIsIn.put(clique, oldIdx - 1);
				}
			}
		}

		for (BitSet clique : oracle.removedCliquesIfAdd(n1, n2)) {
			this.forestIdxCliqueIsIn.remove(clique);
		}
		BitSet newClique = oracle.newCliqueIfAdd(n1, n2);
		// Update data structures
		this.graph.addSecuredEdge(n1, n2);
		SimpleGraph<BitSet, CliqueGraphEdge> newJF = this.graph.getJunctionTree();
		this.inspector = new ConnectivityInspector<>(newJF);
		// Add new clique
		this.forestIdxCliqueIsIn.put(newClique, oldTreeIdx1);
		Set<BitSet> vertices = inspector.connectedSetOf(newClique);
		//this.forest.set(oldTreeIdx1, this.createTree(newJF, vertices));
		this.forest.set(oldTreeIdx1, this.createTree(vertices));
	}

	private JunctionTreeComputer getTreeCliqueIsIn(BitSet clique) {
		return this.forest.get(this.forestIdxCliqueIsIn.get(clique));
	}

	public SimpleFactor getBeliefContainingVars(BitSet vars) {
		for (BitSet clique : this.forestIdxCliqueIsIn.keySet()) {
			if (EdgeAddingOracle.containsAll(clique, vars)) {
				JunctionTreeComputer tree = this.getTreeCliqueIsIn(clique);
				return tree.cliqueBelief.get(clique);
			}
		}
		throw new Error("Clique containing vars not found");
	}

	private List<JunctionTreeComputer> createForest() {
		List<JunctionTreeComputer> forest = new ArrayList<>();
		List<Set<BitSet>> verticesInEachTree = this.inspector.connectedSets();
		for (int i = 0; i < verticesInEachTree.size(); i++) {
			Set<BitSet> verticesInTree = verticesInEachTree.get(i);
			for (BitSet clique : verticesInTree) {
				this.forestIdxCliqueIsIn.put(clique, i);
			}
			forest.add(this.createTree(verticesInTree));
		}
		return forest;
	}

	private JunctionTreeComputer createTree(Set<BitSet> verticesInTree) {
		SimpleGraph<BitSet, DefaultWeightedEdge> wcg = new SimpleGraph<>(DefaultWeightedEdge.class);
		SimpleGraph<BitSet, CliqueGraphEdge> JT = new SimpleGraph<>(CliqueGraphEdge::new);
		List<BitSet> cliques = new ArrayList<>(verticesInTree);
		for (BitSet clique : cliques) {
			wcg.addVertex(clique);
			JT.addVertex(clique);
		}
		for (int i = 0; i < cliques.size(); i++) {
			BitSet Ci = cliques.get(i);
			for (int j = i+1; j < cliques.size(); j++) {
				BitSet Cj = cliques.get(j);
				BitSet intersect = (BitSet) Cj.clone();
				intersect.and(Ci);
				if (intersect.cardinality() > 0) {
					wcg.setEdgeWeight(wcg.addEdge(Ci, Cj), -intersect.cardinality());
				}
			}
		}
		KruskalMinimumSpanningTree<BitSet, DefaultWeightedEdge> span =
				new KruskalMinimumSpanningTree<>(wcg);
		for (DefaultWeightedEdge e : span.getEdgeSet()) {
			BitSet v1 = wcg.getEdgeSource(e);
			BitSet v2 = wcg.getEdgeTarget(e);
			JT.addEdge(v1, v2);
		}
		return new JunctionTreeComputer(this.computer, JT);
	}

	public double scoreEdgeAddition(Integer n1, Integer n2) {
		// Find tree of n1 and n2
		BitSet C1 = this.oracle.getCa(n1, n2);
		BitSet C2 = this.oracle.getCb(n1, n2);
		int treeIdx1 = this.forestIdxCliqueIsIn.get(C1);
		int treeIdx2 = this.forestIdxCliqueIsIn.get(C2);
		// if same tree, call scorer of tree and multiply by belief sum of other trees
		// if different tree product belief of (n1 clique) (n2 clique) (product of the belief sum of other trees)
		double score = treeIdx1 == treeIdx2 ?
				scoreUnchangedForTrees(treeIdx1) :
				scoreUnchangedForTrees(treeIdx1, treeIdx2);
		score *= this.replaceFactorsAfterAdd(this.oracle, n1, n2);
		return score;
	}

	public double replaceFactorsAfterAdd(EdgeAddingOracle oracle, Integer n1, Integer n2) {
		BitSet newClique = oracle.newCliqueIfAdd(n1, n2);
		List<BitSet> oldCliques = oracle.removedCliquesIfAdd(n1, n2);
		List<BitSet> newSeparator = oracle.newSeparatorsIfAdd(n1, n2);
		BitSet oldSeparator = oracle.removedSeparatorIfAdd(n1, n2);

		BitSet C1 = oracle.getCa(n1, n2);
		BitSet C2 = oracle.getCb(n1, n2);
		SimpleFactor sumProduct = this.jointFactor(C1, C2);

		double sum = Arrays.stream(sumProduct.getFactor()).sum();
		for (BitSet clique : oldCliques) {
			sumProduct.divideByFactor(this.computer.newCliqueFactor(clique), clique);
		}
		sum = Arrays.stream(sumProduct.getFactor()).sum();
		if (!oldSeparator.isEmpty()) {
			sumProduct.divideByFactor(this.computer.newSepFactor(oldSeparator), oldSeparator);
		}
		sum = Arrays.stream(sumProduct.getFactor()).sum();
		for (BitSet sep : newSeparator) {
			sumProduct.multiplyByFactor(this.computer.newSepFactor(sep), sep);
		}
		sum = Arrays.stream(sumProduct.getFactor()).sum();
		sumProduct.multiplyByFactor(this.computer.newCliqueFactor(newClique), newClique);
		// TODO: This should be > 1.0
		sum = Arrays.stream(sumProduct.getFactor()).sum();
		return sumProduct.marginaliseVariables(new BitSet())[0];
	}

	private SimpleFactor jointFactor(BitSet C1, BitSet C2) {
		int treeIdx1 = this.forestIdxCliqueIsIn.get(C1);
		int treeIdx2 = this.forestIdxCliqueIsIn.get(C2);
		SimpleFactor simpleFactor;
		if (treeIdx1 == treeIdx2) {
			simpleFactor = this.forest.get(treeIdx1).jointFactor(C1, C2);
		} else {
			SimpleFactor factor1 = this.forest.get(treeIdx1).getCliqueBelief(C1);
			SimpleFactor factor2 = this.forest.get(treeIdx2).getCliqueBelief(C2);
			simpleFactor = SimpleFactor.multiply2Factors(factor1, factor2);
		}
		double sum = Arrays.stream(simpleFactor.getFactor()).sum();
		return simpleFactor;
	}

	public double scoreUnchangedForTrees(int ... excludedTreeIdx) {
		double unchangedScore = 1.0;
		for (int i = 0; i < this.forest.size(); i++) {
			if (!Arrays.asList(excludedTreeIdx).contains(i)) {
				unchangedScore *= this.forest.get(i).sumBelief();
			}
		}
		return unchangedScore;
	}

}
