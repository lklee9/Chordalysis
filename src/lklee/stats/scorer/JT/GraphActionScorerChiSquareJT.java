package lklee.stats.scorer.JT;

import core.model.DecomposableModel;
import core.model.GraphAction;
import core.model.PValueScoredGraphAction;
import core.model.ScoredGraphAction;
import core.stats.EntropyComputer;
import core.stats.scorer.GraphActionScorer;
import core.tools.ChiSquared;
import lklee.stats.computer.JT.JunctionForestComputer;

public class GraphActionScorerChiSquareJT extends GraphActionScorer {

	JunctionForestComputer jfComputer;
	EntropyComputer entropyComputer;

	// TODO: move chi square specific functions here
	public GraphActionScorerChiSquareJT(
			JunctionForestComputer jfComputer, EntropyComputer entropyComputer
	) {
		this.jfComputer = jfComputer;
		this.entropyComputer = entropyComputer;
	}

	@Override
	public ScoredGraphAction scoreEdge(DecomposableModel model, GraphAction action) {
		// TODO: Different subclasses for different p-value test (left tail, right tail, both tail, raw score)
		double chiSquare = this.jfComputer.scoreEdgeAddition(action.getV1(), action.getV2());
		chiSquare = this.entropyComputer.getNbInstances() * (chiSquare - 1);
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

}
