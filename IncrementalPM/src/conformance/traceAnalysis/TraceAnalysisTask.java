package conformance.traceAnalysis;

import java.util.concurrent.Callable;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XTrace;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;

import qualitychecking.QualityCheckManager;
import ressources.IccParameter;



public class TraceAnalysisTask implements Callable<Triple<Boolean,XTrace, Integer>>{

	IncrementalTraceAnalyzer<?> analyzer;
	XTrace trace;
	PetrinetGraph net;
	XAttributeMap logAttributes;
	IccParameter parameter;
	Integer index;
	
	public TraceAnalysisTask(XTrace trace, Integer index, IncrementalTraceAnalyzer<?> analyzer, IccParameter parameter, PetrinetGraph net, XAttributeMap logAttributes) {
		this.trace = trace;
		this.analyzer = analyzer;
		this.net = net;
		this.parameter = parameter;
		this.logAttributes = logAttributes;
		this.index=index;
		
	}
	
	public ImmutableTriple<Boolean, XTrace, Integer> call(){
		boolean result = this.analyzer.analyzeTrace(trace, logAttributes);
		if(this.parameter.isCheckInternalQuality() && this.parameter.getInternalQualityCheckManager().getDistributions().size()>0) {
			boolean qualityResult = internalQualityCheck(trace, parameter, result);
			if (qualityResult && !result) this.parameter.getInternalQualityCheckManager().wasUsed();
			result = result || qualityResult;
		}
		return new ImmutableTriple<Boolean, XTrace, Integer>(result,trace, this.index);
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
