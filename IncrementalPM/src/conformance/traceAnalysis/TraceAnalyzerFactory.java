package conformance.traceAnalysis;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.model.XLog;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;

import conformance.IncrementalConformanceChecker;
import conformance.traceAnalysis.deviation.ApproxDeviationAnalyzer_NONALIGNING;
import conformance.traceAnalysis.deviation.ApproxDeviationAnalyzer_PREFIXSUFFIX;
import conformance.traceAnalysis.deviation.DeviationAnalyzer;
import conformance.traceAnalysis.fitness.ApproxFitnessAnalyzer;
import conformance.traceAnalysis.fitness.FitnessAnalyzer;
import conformance.traceAnalysis.resourceDeviation.ApproxResourceDeviationAnalyzer_NONALIGNING;
import conformance.traceAnalysis.resourceDeviation.ResourceDeviationAnalyzer;
import resourcedeviations.ResourceAssignment;
import ressources.IccParameter;


public class TraceAnalyzerFactory {
	/**
	 * creates and returns an instance of a traceAnalyzer class depending on the specifics in the iccparameters
	 * @param parameter
	 * @param replayer
	 * @param mapping
	 * @param log
	 * @param net
	 * @param resourceAssignment
	 * @return
	 */
	public static IncrementalTraceAnalyzer<?> createTraceAnalyzer(IccParameter parameter, TransEvClassMapping mapping, XEventClassifier classifier, XLog log, PetrinetGraph net, ResourceAssignment resourceAssignment) {
		if (parameter.getGoal().equals(IncrementalConformanceChecker.Goals.FITNESS)   && !parameter.isApproximate()) {
			return new FitnessAnalyzer(parameter, net, mapping, classifier);
		}
		if (parameter.getGoal().equals(IncrementalConformanceChecker.Goals.FITNESS)   &&  parameter.isApproximate()) {
			return new ApproxFitnessAnalyzer(parameter, log, net, mapping, classifier);
		}
		if (parameter.getGoal().equals(IncrementalConformanceChecker.Goals.DEVIATIONS) && !parameter.isApproximate()) {
			return new DeviationAnalyzer(parameter, mapping, net, classifier);
		}
		if (parameter.getGoal().equals(IncrementalConformanceChecker.Goals.DEVIATIONS) &&  parameter.isApproximate()) {
			if(parameter.getApproximationHeuristic().equals(IncrementalConformanceChecker.Heuristics.PREFIXSUFFIX))
				return new ApproxDeviationAnalyzer_PREFIXSUFFIX(parameter, mapping, net, classifier);
			else if(parameter.getApproximationHeuristic().equals(IncrementalConformanceChecker.Heuristics.NONALIGNING_ALL))
				return new ApproxDeviationAnalyzer_NONALIGNING(parameter, mapping, net, classifier);		
			else if(parameter.getApproximationHeuristic().equals(IncrementalConformanceChecker.Heuristics.NONALIGNING_KNOWN))
				return new ApproxDeviationAnalyzer_NONALIGNING(parameter, mapping, net, classifier);		
		}
		if (parameter.getGoal().equals(IncrementalConformanceChecker.Goals.RESOURCES) && !parameter.isApproximate()) {
			return new ResourceDeviationAnalyzer(parameter, resourceAssignment, net, mapping, classifier);
		}
		if (parameter.getGoal().equals(IncrementalConformanceChecker.Goals.RESOURCES) &&  parameter.isApproximate()) {
			if(parameter.getApproximationHeuristic().equals(IncrementalConformanceChecker.Heuristics.NONALIGNING_ALL))
				return new ApproxResourceDeviationAnalyzer_NONALIGNING(parameter, resourceAssignment, net, mapping, classifier);
			else if(parameter.getApproximationHeuristic().equals(IncrementalConformanceChecker.Heuristics.NONALIGNING_KNOWN))
				return new ApproxResourceDeviationAnalyzer_NONALIGNING(parameter, resourceAssignment, net, mapping, classifier);
		}
		return null;
	}
}
