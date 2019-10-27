package lklee.stats;

import core.graph.ChordalGraph;
import core.graph.CliqueGraphEdge;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import java.util.*;

public class EdgeAddingOracle {
  ChordalGraph graph;

  public EdgeAddingOracle(ChordalGraph graph) {
    this.graph = graph;
  }

  public BitSet getCa(Integer a, Integer b) {
    CliqueGraphEdge eligibleEdge = graph.getEligibleEdge(a, b);
    boolean aIs1 = eligibleEdge.getClique1().get(a);
    return aIs1 ? eligibleEdge.getClique1() : eligibleEdge.getClique2();
  }

  public BitSet getCb(Integer a, Integer b) {
    CliqueGraphEdge eligibleEdge = graph.getEligibleEdge(a, b);
    boolean aIs1 = eligibleEdge.getClique1().get(a);
    return aIs1 ? eligibleEdge.getClique2() : eligibleEdge.getClique1();
  }

  public BitSet newCliqueIfAdd(Integer a, Integer b) {
  	BitSet Cab = this.graph.getSeparator(a, b);
  	Cab.set(a);
    Cab.set(b);
  	return Cab;
  }

  public List<BitSet> removedCliquesIfAdd(Integer a, Integer b) {
    BitSet Cab = this.newCliqueIfAdd(a, b);
    CliqueGraphEdge eligibleEdge = graph.getEligibleEdge(a, b);
    BitSet Ca = eligibleEdge.getClique1();
    BitSet Cb = eligibleEdge.getClique2();

    List<BitSet> removedCliques = new ArrayList<>();
    if (containsAll(Cab, Ca)) removedCliques.add(Ca);
    if (containsAll(Cab, Cb)) removedCliques.add(Cb);
    return removedCliques;
  }

  public List<BitSet> cliquesIfAdding(Integer a, Integer b) {
    BitSet Cab = this.newCliqueIfAdd(a, b);

    CliqueGraphEdge eligibleEdge = graph.getEligibleEdge(a, b);
    boolean aIs1 = eligibleEdge.getClique1().get(a);
    BitSet Ca = aIs1 ? eligibleEdge.getClique1() : eligibleEdge.getClique2();
    BitSet Cb = aIs1 ? eligibleEdge.getClique2() : eligibleEdge.getClique1();

    List<BitSet> cliques = new ArrayList<>(graph.getCliquesBFS());
    cliques.add(Cab);
    if (containsAll(Cab, Ca)) cliques.remove(Ca);
    if (containsAll(Cab, Cb)) cliques.remove(Cb);
    return cliques;
  }

  public List<BitSet> newSeparatorsIfAdd(Integer a, Integer b) {
    BitSet Cab = this.newCliqueIfAdd(a, b);
    CliqueGraphEdge eligibleEdge = graph.getEligibleEdge(a, b);
    BitSet Ca = eligibleEdge.getClique1();
    BitSet Cb = eligibleEdge.getClique2();
    List<BitSet> newSeparators = new ArrayList<>();
    if (!containsAll(Cab, Ca)) {
      Ca.and(Cab);
      newSeparators.add(Ca);
    }
    if (!containsAll(Cab, Cb)) {
      Cb.and(Cab);
      newSeparators.add(Cb);
    }
    return newSeparators;
  }

  public BitSet removedSeparatorIfAdd(Integer a, Integer b) {
    return graph.getSeparator(a, b);
  }

  public List<BitSet> separatorsIfAdding(Integer a, Integer b) {
    BitSet Sab = this.removedSeparatorIfAdd(a, b);

    BitSet Cab = this.newCliqueIfAdd(a, b);

    CliqueGraphEdge eligibleEdge = graph.getEligibleEdge(a, b);
    boolean aIs1 = eligibleEdge.getClique1().get(a);
    BitSet Ca = aIs1 ? eligibleEdge.getClique1() : eligibleEdge.getClique2();
    BitSet Cb = aIs1 ? eligibleEdge.getClique2() : eligibleEdge.getClique1();

    List<BitSet> separators = new ArrayList<>(graph.getSeparatorsBFS());
    separators.remove(Sab);
    if (!containsAll(Cab, Ca)) {
      Ca.and(Cab);
      separators.add(Ca);
    }
    if (!containsAll(Cab, Cb)) {
      Cb.and(Cab);
      separators.add(Cb);
    }
    return separators;
  }

  public final List<Integer> peoIfAdding(Integer a, Integer b) {
    List<Integer> peo = new ArrayList<>();
    SimpleGraph<Integer, DefaultEdge> gElim = (ChordalGraph) graph.clone();
    TreeMap<Integer, Integer> labels = new TreeMap<>();
    for (Integer vertex : graph.vertexSet()) {
      labels.put(vertex, 0);
    }
    while (!labels.isEmpty()) {
      int maxNumber = -1;
      Integer xi = null;
      for (Map.Entry<Integer, Integer> entry : labels.entrySet()) {
        if (entry.getValue() > maxNumber) {
          xi = entry.getKey();
          maxNumber = entry.getValue();
        }
      }
      peo.add(0, xi);

      Set<DefaultEdge> edges = gElim.edgesOf(xi);
      if (xi.equals(a) && labels.containsKey(b)) {
        Integer number = labels.get(b);
        labels.put(b, number + 1);
      } else if (xi.equals(b) && labels.containsKey(a)) {
        Integer number = labels.get(a);
        labels.put(a, number + 1);
      }
      for (DefaultEdge edge : edges) {
        Integer source = gElim.getEdgeSource(edge);
        Integer target = gElim.getEdgeTarget(edge);
        if (source.equals(xi)) {
          Integer number = labels.get(target);
          labels.put(target, number + 1);
        } else {
          Integer number = labels.get(source);
          labels.put(source, number + 1);
        }
      }
      gElim.removeVertex(xi);
      labels.remove(xi);
    }
    return peo;
  }

  public boolean containsAll(BitSet s1, BitSet s2) {
    BitSet intersection = (BitSet) s1.clone();
    intersection.and(s2);
    return intersection.equals(s2);
  }
}
