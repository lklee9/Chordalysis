package lklee.explorer;

import core.explorer.ChordalysisModeller;
import core.model.Couple;
import core.model.ScoredGraphAction;
import core.stats.EntropyComputer;
import core.stats.scorer.GraphActionScorer;
import lklee.stats.computer.ChiSquareComputer;
import lklee.stats.scorer.VE.GraphActionScorerChiSquare;

import java.util.BitSet;

public class ChordalysisModellingChiSquared extends ChordalysisModeller {
    double pValueThreshold;

    public ChordalysisModellingChiSquared(Data data, double pValueThreshold) {
        super(data);
        this.pValueThreshold = pValueThreshold;
    }

    @Override
    protected GraphActionScorer initScorer() {
        EntropyComputer entropyComputer = new EntropyComputer(this.lattice);
        ChiSquareComputer chiSquareComputer = new ChiSquareComputer(this.lattice);
        return new GraphActionScorerChiSquare(chiSquareComputer, entropyComputer);
    }

    @Override
    public void explore() {
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
            bestModel.performAction(todo, bestModel, pq);
            for (Couple<Integer> edge : pq.getActionEdges()) {
                pq.updateEdge(edge.getV1(), edge.getV2());
            }
            pq.processStoredModifications();
            step++;
        }
    }
}
