package lklee.stats.scorer.VE;

import core.graph.ChordalGraph;
import core.model.DecomposableModel;
import core.model.GraphAction;
import core.model.PValueScoredGraphAction;
import core.model.ScoredGraphAction;
import core.stats.EntropyComputer;
import core.stats.scorer.GraphActionScorer;
import core.tools.ChiSquared;
import lklee.stats.computer.ChiSquareComputer;
import lklee.stats.EdgeAddingOracle;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class GraphActionScorerChiSquare extends GraphActionScorer {

    // List of factor collections. Each collection contains common variable.
    // Priority of shared variable based on elim ordering

    List<Integer> ordering;
    int nbInstances;
    ChiSquareComputer chiSquareComputer;
    EntropyComputer entropyComputer;

    public GraphActionScorerChiSquare(
            ChiSquareComputer chiSquareComputer,
            EntropyComputer entropyComputer
    ) {
        this.chiSquareComputer = chiSquareComputer;
        this.nbInstances = this.chiSquareComputer.getLattice().getNbInstances();
        this.entropyComputer = entropyComputer;
    }

    @Override
    public ScoredGraphAction scoreEdge(DecomposableModel model, GraphAction action) {
        ChordalGraph baseGraph = model.getGraph();
        EdgeAddingOracle oracle = new EdgeAddingOracle(baseGraph);

        //TODO: Chordal Graph clone method is broken
        //TODO: Chordal Graph remove edge not implemented
        this.ordering = oracle.peoIfAdding(action.getV1(), action.getV2());
        List<List<VarElimFactor>> factorList =
            this.getFactorList(baseGraph, oracle, action);
        BitSet marginalisedVars = new BitSet();
        VarElimFactor remFactor = null;
        for (int i = this.ordering.size() - 1; i >= 0; i--) {
            int varIdx = this.ordering.get(i);
            marginalisedVars.set(varIdx);
            List<VarElimFactor> factors = factorList.get(varIdx);
            if (remFactor != null) factors.add(remFactor);
            VarElimFactor currentFactor = VarElimFactor.factorProduct(factors);
            remFactor = currentFactor.marginaliseVariables(marginalisedVars);
        }
        double chiSquare = this.nbInstances * (remFactor.factorValues[0] - 1);
        long dfDiff = model.nbParametersDiffIfAdding(action.getV1(), action.getV2());
        Double diffEntropy = model.entropyDiffIfAdding(
            action.getV1(), action.getV2(), entropyComputer
        );
        if (diffEntropy == null || Double.isNaN(chiSquare) ) {
            return new PValueScoredGraphAction(
                action.getType(),action.getV1(), action.getV2(), 1.0,
                dfDiff, Double.NaN
            );
        }
        double pValue = ChiSquared.pValue(chiSquare, dfDiff);

        PValueScoredGraphAction scoredAction = new PValueScoredGraphAction(
            action.getType(),action.getV1(), action.getV2(), pValue,
            dfDiff, chiSquare / dfDiff
        );
        return scoredAction;
    }

    @Override
    public Object clone() {
        return new GraphActionScorerChiSquare(this.chiSquareComputer, this.entropyComputer);
    }

    protected List<List<VarElimFactor>> getFactorList(
        ChordalGraph gBefore, EdgeAddingOracle oracle, GraphAction action
    ) {
        List<List<VarElimFactor>> factorList = new ArrayList<>(ordering.size());
        for (int i = 0; i < ordering.size(); i++) factorList.add(new ArrayList<>());
        List<BitSet> bCliques = gBefore.getCliquesBFS();
        List<BitSet> aCliques = oracle.cliquesIfAdding(action.getV1(), action.getV2());
        List<BitSet> bSeparators = gBefore.getSeparatorsBFS();
        List<BitSet> aSeparators = oracle.separatorsIfAdding(action.getV1(), action.getV2());
        // 1 / P_C(x)
        for (BitSet bClique : bCliques) {
            int varPrioIdx = this.findHighestPrioVarIdx(bClique, this.ordering);
            VarElimFactor newFactor = new VarElimFactor(
                this.chiSquareComputer.getLattice(), bClique, (x) -> x == 0 ? 0 : 1/x
            );
            factorList.get(varPrioIdx).add(newFactor);
        }
        // P_S(x)
        for (BitSet bSep: bSeparators) {
            int varPrioIdx = this.findHighestPrioVarIdx(bSep, this.ordering);
            VarElimFactor newFactor = new VarElimFactor(
                this.chiSquareComputer.getLattice(), bSep, (x) -> x
            );
            factorList.get(varPrioIdx).add(newFactor);
        }
        // Q_C(x)^2
        for (BitSet aClique: aCliques) {
            int varPrioIdx = this.findHighestPrioVarIdx(aClique, this.ordering);
            VarElimFactor newFactor = new VarElimFactor(
                this.chiSquareComputer.getLattice(), aClique, (x) -> Math.pow(x, 2)
            );
            factorList.get(varPrioIdx).add(newFactor);
        }
        // 1 / Q_S(x)^2
        for (BitSet aSep: aSeparators) {
            int varPrioIdx = this.findHighestPrioVarIdx(aSep, this.ordering);
            VarElimFactor newFactor = new VarElimFactor(
                this.chiSquareComputer.getLattice(), aSep, (x) -> x == 0 ? 0 : 1 / Math.pow(x, 2)
            );
            factorList.get(varPrioIdx).add(newFactor);
        }
        return factorList;
    }

    protected int findHighestPrioVarIdx(BitSet vars, List<Integer> ordering) {
        for (int i = ordering.size() - 1; i >= 0; i--) {
            int idx = ordering.get(i);
            if (vars.get(idx)) return idx;
        }
        throw new Error("No variable set to true in given bitset");
    }
}
