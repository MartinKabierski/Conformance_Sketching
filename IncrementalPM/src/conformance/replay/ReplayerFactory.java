package conformance.replay;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.impl.XLogImpl;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;

import nl.tue.alignment.Replayer;
import nl.tue.alignment.ReplayerParameters;
import nl.tue.alignment.algorithms.ReplayAlgorithm.Debug;

/**
 *Creates an instance of the Petrinet Replayer for further use during sampling
 *Currently only implements the IterativeAStrar-based approach of the ALignment-Package
 * @author Martin Bauer
 *
 */
public class ReplayerFactory {
	static int THREADS_SAMPLING =1;
	static int THREADS_BASELINE =10;
	
	/**
	 * instantiates and returns an instance of the replayer class used for the replay-based construction of trace-petrinet alignments
	 * Number of threads is specified through THREADS_SAMPLING and THREADS_BASELINE
	 * @param net
	 * @param log
	 * @param mapping
	 * @param calculateIncrementally
	 * @return
	 */
	public static Replayer createReplayer(PetrinetGraph net, XLog log, TransEvClassMapping mapping, XEventClassifier classifier, boolean calculateIncrementally, Marking initialMarking, Marking finalMarking) {
		XLogInfo summary = XLogInfoFactory.createLogInfo(log, classifier);
		XEventClasses classes = summary.getEventClasses();
		ReplayerParameters parameters;
		if (calculateIncrementally) parameters = new ReplayerParameters.Default(THREADS_SAMPLING, Debug.NONE);
		else if (!calculateIncrementally) parameters = new ReplayerParameters.Default(THREADS_BASELINE, Debug.NONE);
		else parameters = new ReplayerParameters.Default();
		return new Replayer(parameters, (Petrinet) net, initialMarking, finalMarking, classes, mapping, true);
	}
	
	/**
	 * instantiates and returns an instance of the replayer class used for the replay-based construction of trace-petrinet alignments
	 * Number of threads is specified as parameter
	 * @param net
	 * @param log
	 * @param mapping
	 * @param calculateIncrementally
	 * @return
	 */
	public static Replayer createReplayer(PetrinetGraph net, XLog log, TransEvClassMapping mapping, XEventClassifier classifier, int noThreads, Marking initialMarking, Marking finalMarking) {
		XLogInfo summary = XLogInfoFactory.createLogInfo(log, classifier);
		XEventClasses classes = summary.getEventClasses();
		//System.out.println(Runtime.getRuntime().availableProcessors() / 4);
		ReplayerParameters parameters = new ReplayerParameters.Default(noThreads, Debug.NONE);
		return new Replayer(parameters, (Petrinet) net, initialMarking, finalMarking, classes, mapping, true);
	}
	
	public static Replayer createReplayer(PetrinetGraph net, XLog log, TransEvClassMapping mapping, XEventClassifier classifier, boolean calculateIncrementally) {
		XLogInfo summary = XLogInfoFactory.createLogInfo(log, classifier);
		XEventClasses classes = summary.getEventClasses();
		ReplayerParameters parameters;
		if (calculateIncrementally) parameters = new ReplayerParameters.Default(THREADS_SAMPLING, Debug.NONE);
		else  parameters = new ReplayerParameters.Default(THREADS_BASELINE, Debug.NONE);
		//else parameters = new ReplayerParameters.Default();
		return new Replayer(parameters, (Petrinet) net, getInitialMarking(net), getFinalMarking(net), classes, mapping, true);
	}
	
	/**
	 * retrieve final marking from net
	 * @param net
	 * @return
	 */
	private static Marking getFinalMarking(PetrinetGraph net) {
		Marking finalMarking = new Marking();
	
		for (Place p : net.getPlaces()) {
			if (net.getOutEdges(p).isEmpty())
				finalMarking.add(p);
		}
	
		return finalMarking;
	}
	
	/**
	 * retrieve initial marking from net
	 * @param net
	 * @return
	 */
	private static Marking getInitialMarking(PetrinetGraph net) {
		Marking initMarking = new Marking();
	
		for (Place p : net.getPlaces()) {
			if (net.getInEdges(p).isEmpty())
				initMarking.add(p);
		}
	
		return initMarking;
	}
}
