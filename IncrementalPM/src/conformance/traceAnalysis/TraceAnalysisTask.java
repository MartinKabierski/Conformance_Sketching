package conformance.traceAnalysis;

import java.util.concurrent.Callable;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XTrace;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;

import qualitychecking.QualityCheckManager;
import ressources.IccParameter;



public class TraceAnalysisTask implements Callable<Pair<Boolean,XTrace>>{

	IncrementalTraceAnalyzer<?> analyzer;
	XTrace trace;
	PetrinetGraph net;
	XAttributeMap logAttributes;
	TransEvClassMapping mapping;
	IccParameter parameter;
	
	public TraceAnalysisTask(XTrace trace, IncrementalTraceAnalyzer<?> analyzer, IccParameter parameter, PetrinetGraph net, XAttributeMap logAttributes, TransEvClassMapping mapping) {
		this.trace = trace;
		this.analyzer = analyzer;
		this.net = net;
		this.logAttributes = logAttributes;
		this.mapping = mapping;
		this.parameter = parameter;
		
	}
	
	public ImmutablePair<Boolean, XTrace> call(){
		boolean result = this.analyzer.analyzeTrace(trace, net, this.mapping, logAttributes);
		if(this.parameter.isCheckInternalQuality()) {
			boolean qualityResult = internalQualityCheck(trace, parameter, result);
			if (qualityResult && !result) this.parameter.getInternalQualityCheckManager().wasUsed();
			result = result || qualityResult;
		}
		return new ImmutablePair<Boolean, XTrace>(result,trace);
	}
	
	public boolean internalQualityCheck(XTrace trace, IccParameter parameter, boolean significantChange) {
	//System.out.println(System.currentTimeMillis());
	QualityCheckManager qualityChecker = this.parameter.getInternalQualityCheckManager();
	// if already novel conformance information, distributions must still be updated, but no need to check attributes
	if (significantChange) {
		qualityChecker.addTraceToDistributions(trace);
		return true;
	} else {
		return qualityChecker.addTraceToDistributionsAndCheckPredicate(trace, this.parameter.getEpsilon());
		}
	}
}
