package conformance.traceAnalysis.fitness;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang3.tuple.Pair;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XLogImpl;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;

import conformance.replay.ReplayerFactory;
import conformance.traceAnalysis.TraceEditDistance;
import nl.tue.alignment.Progress;
import nl.tue.alignment.Replayer;
import ressources.IccParameter;
import ressources.TraceAnalysisResult;

public class ApproxFitnessAnalyzer extends FitnessAnalyzer{
	double petrinetShortestPath;
	
	public ApproxFitnessAnalyzer(IccParameter parameters, XLog log, PetrinetGraph net, TransEvClassMapping mapping, XEventClassifier classifier) {
		super(parameters, net, mapping, classifier);
		this.conformanceCalculator = new FitnessConformanceCalculator();
		
		this.petrinetShortestPath=0;
		//dirty hack - get shortest petri net path once upon creation to circumvent repeated replay during cost approximation
		Replayer oneTimeReplayer = ReplayerFactory.createReplayer(net, log, mapping, classifier, false);
		XAttributeMap logAttributes=log.getAttributes();
		XLog testlog=new XLogImpl(logAttributes);
		testlog.add(log.get(0));
		PNRepResult pnrresult;
		try {
			pnrresult = oneTimeReplayer.computePNRepResult(Progress.INVISIBLE, testlog);
			this.petrinetShortestPath=new Double(pnrresult.getInfo().get("Model move cost empty trace").toString());
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
	}
	
	
	protected List<Double> determineConformanceApproximations(XTrace trace,
			Pair<TraceAnalysisResult<Double>, TraceEditDistance> referenceTraceInformation,
			XAttributeMap logAttributes) {
		//System.out.println("Shortest Petrinet Path: "+this.petrinetShortestPath);
		//System.out.println("Referenz Result: "+referenceTraceInformation.getLeft().getResult()+", Referenz L�nge: "+referenceTraceInformation.getLeft().getTrace().size());
		double referenceTraceCost = (1.0-referenceTraceInformation.getLeft().getResult())*(referenceTraceInformation.getLeft().getTrace().size()+this.petrinetShortestPath);
		//System.out.println("Referenz Raw Cost: "+referenceTraceCost);
		
		//System.out.println("Distanz: "+referenceTraceInformation.getRight().getDistance());
		//System.out.println("Trace Size: "+trace.size());
		double estimatedCost= Math.min(referenceTraceInformation.getRight().getDistance() + referenceTraceCost, trace.size()+petrinetShortestPath);
		////double estimatedCost = referenceTraceInformation.getRight().getDistance()+ referenceTraceCost;
		//System.out.println("Approx Cost Raw: "+estimatedCost);
		double estimatedFitness=1.0-(estimatedCost/(trace.size()+petrinetShortestPath));
		////double estimatedFitness=1.0-(estimatedCost/(referenceTraceInformation.getLeft().getTrace().size()+petrinetShortestPath+(referenceTraceInformation.getLeft().getTrace().size()+trace.size()*parameter.getK())));
		//System.out.println("Estimated Fitness: "+estimatedFitness);
		List<Double> resultList = new ArrayList<Double>();
		resultList.add(estimatedFitness);
		//System.out.println(resultList+"\n");

		return resultList;
	}
}
