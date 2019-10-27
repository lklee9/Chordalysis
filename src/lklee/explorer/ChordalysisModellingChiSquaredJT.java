package lklee.explorer;

import core.explorer.ChordalysisModeller;
import core.model.Couple;
import core.model.ScoredGraphAction;
import core.stats.EntropyComputer;
import core.stats.scorer.GraphActionScorer;
import lklee.stats.computer.JT.JunctionForestComputer;
import lklee.stats.computer.JT.factor.FactorComputerChiSquare;
import lklee.stats.scorer.JT.GraphActionScorerChiSquareJT;

import java.util.BitSet;

public class ChordalysisModellingChiSquaredJT extends ChordalysisModeller {
    double pValueThreshold;
    JunctionForestComputer jtComputer;

    public ChordalysisModellingChiSquaredJT(Data data, double pValueThreshold) {
        super(data);
        this.pValueThreshold = pValueThreshold;
    }

    @Override
    protected GraphActionScorer initScorer() {
        this.jtComputer = new JunctionForestComputer(
            new FactorComputerChiSquare(this.lattice)
        );
        return new GraphActionScorerChiSquareJT(
            this.jtComputer,
            new EntropyComputer(lattice)
        );
    }

    @Override
    public void explore() {
        this.jtComputer.initJunctionForest(bestModel);
        pq.processStoredModifications();
        double remainingBudget = pValueThreshold;
        int step=0;
        while (!pq.isEmpty()&& step<maxNSteps) {
            int nTests = pq.size();

            double correctedPValueThreshold = remainingBudget / nTests;

            // System.out.println(pq);
            ScoredGraphAction todo = pq.poll();
            int treeWidth = bestModel.getCliques().stream()
                .map(BitSet::cardinality)
                .max(Integer::compareTo)
                .orElse(-1);
            System.out.println(step + ". " + todo.toString() + "\t TreeWidth=" + treeWidth);
            // TODO: How does this p-value checker work logically
            if (todo.getScore()> correctedPValueThreshold) {
                break;
            }
            // Budget gets used quick because no todos where p-value is 0
            double usedBudget = todo.getScore()*nTests;
            remainingBudget -= usedBudget;
            //System.out.println("Remaining Budget: " + remainingBudget);
            operationsPerformed.add(todo);
            this.jtComputer.addEdgeToForest(todo.getV1(), todo.getV2());
            bestModel.performAction(todo, bestModel, pq);
            for (Couple<Integer> edge : pq.getActionEdges()) {
                pq.updateEdge(edge.getV1(), edge.getV2());
            }
            pq.processStoredModifications();
            step++;
        }
    }
}
