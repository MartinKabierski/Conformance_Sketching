package conformance.traceAnalysis.fitness;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XTrace;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;

import conformance.traceAnalysis.IncrementalTraceAnalyzer;
import conformance.traceAnalysis.TraceEditDistance;
import ressources.IccParameter;
import ressources.TraceAnalysisResult;

public class FitnessAnalyzer extends IncrementalTraceAnalyzer<Double> {
	
	public FitnessAnalyzer(IccParameter parameters, PetrinetGraph net) {
		super(parameters, net);
		this.conformanceCalculator = new FitnessConformanceCalculator();
	}
	
	
	protected Double retrieveConformanceInformation(XTrace trace, PNRepResult replayResult) {
		double rawFitness= (double) replayResult.getInfo().get("Raw Fitness Cost");
		double fitness= 1-rawFitness/ (trace.size()+(new Double(replayResult.getInfo().get("Model move cost empty trace").toString())));
		return fitness;
	}


	protected List<Double> determineConformanceApproximations(XTrace trace,
			Pair<TraceAnalysisResult<Double>, TraceEditDistance> referenceTraceInformation,
			XAttributeMap logAttributes) {
		// TODO Auto-generated method stub
		return null;
	}	
}