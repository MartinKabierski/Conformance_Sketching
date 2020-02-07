package Plugins;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.in.XParser;
import org.deckfour.xes.in.XesXmlParser;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.impl.XAttributeLiteralImpl;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.models.connections.GraphLayoutConnection;
import org.processmining.models.connections.petrinets.behavioral.InitialMarkingConnection;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.graphbased.directed.petrinet.impl.PetrinetFactory;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.connectionfactories.logpetrinet.EvClassLogPetrinetConnectionFactoryUI;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;
import org.processmining.plugins.petrinet.manifestreplayer.PNManifestReplayer;
import org.processmining.plugins.petrinet.manifestreplayer.PNManifestReplayerParameter;
import org.processmining.plugins.petrinet.manifestreplayer.algorithms.IPNManifestReplayAlgorithm;
import org.processmining.plugins.petrinet.replayer.ui.PNReplayerUI;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.plugins.pnml.base.Pnml;
import org.processmining.plugins.pnml.importing.PnmlImportUtils;
import org.processmining.plugins.replayer.replayresult.SyncReplayResult;

import com.google.common.collect.Multiset;
import com.google.common.collect.TreeMultiset;

import Algorithm.IncrementalConformanceChecker;
import Replayer.AlignmentReplayer;
import Replayer.ApproxAlignmentReplayer;
import Replayer.ApproxFitnessReplayer;
import Replayer.ApproxResourceDeviationReplayer;
import Replayer.FitnessReplayer;
import Replayer.IncrementalReplayer;
import Replayer.ResourceDeviationReplayer;
import Ressources.AlignmentReplayResult;
import Ressources.IccParameters;
import Ressources.IccResult;
import nl.tue.alignment.Progress;
import nl.tue.alignment.Replayer;
import nl.tue.alignment.ReplayerParameters;
import nl.tue.alignment.algorithms.ReplayAlgorithm.Debug;
import qualitychecking.QualityCheckManager;
import resourcedeviations.ResourceAssignment;
import resourcedeviations.ResourceAssignmentComputer;
//TODO own functions for parameter windows, reduce number of plugins, refactor whole structure
//TODO use function overloading - with and without parameter as arguments

/**
 * @author MartinBauer
 */
	public class IncrementalConformanceCheckerPlugins{
/**
 * 
 * These are the three end user plugins for single execution and control of correct execution for originally used replayer
 */
		//TODO multiple runs of conformance checking reduce size of log due to  trace getting deleted, build a copy log, at start
	@Plugin(name = "Check for Conformance with Incremental Conformance Checker", returnLabels = { "Conformance Information" }, returnTypes = { String.class }, parameterLabels = {}, userAccessible = true)
	@UITopiaVariant(affiliation = "Humboldt-University Berlin", author = "Martin Bauer", email = "bauermax@hu-berlin.de", uiLabel = UITopiaVariant.USEPLUGIN)
	@PluginVariant(variantLabel = "Calculate Fitness/Optimal Alignment with the Incremental Conformance Checker", requiredParameterLabels = {0,1})
	//get input parameters if needed, create conformance checker and let it compute results
	//ui output stuff also to here
	public String CheckForConformanceWithICCWithUI(final UIPluginContext context, PetrinetGraph net, XLog log) throws Exception {
		TransEvClassMapping mapping = constructTransEvMapping(context, log, net);
		double delta=0.01;
		double alpha=0.99;
		double epsilon=0.01;
		double k=0.1;
		int initialSize=1;
		String goal="alignment";
		boolean approximate=true;
		IccParameters iccParameters=new IccParameters(delta, alpha, epsilon, k, initialSize, goal, approximate);

		IncrementalReplayer replayer = selectReplayer(goal, iccParameters);
				
		//make own parameter function for alignment/fitness
		AlignmentReplayResult result=calculateAlignmentWithICC(context, replayer, net, log, iccParameters, mapping);

		System.out.println("Fitness         : "+result.getFitness());
		System.out.println("Time(ms)        : "+result.getTime());
		System.out.println("Log Size        : "+result.getLogSize());
		System.out.println("No AsynchMoves  : "+result.getTotalNoAsynchMoves());
		System.out.println("AsynchMoves abs : "+result.getAsynchMovesAbs().toString());
		System.out.println("AsynchMoves rel : "+result.getAsynchMovesRel().toString());
		return result.toString();
	}
	
	@Plugin(name = "0000 Han's test bed", returnLabels = { "Conformance Information" }, returnTypes = { String.class }, parameterLabels = {}, userAccessible = true)
	@UITopiaVariant(affiliation = "Humboldt-University Berlin", author = "Martin Bauer", email = "bauermax@hu-berlin.de", uiLabel = UITopiaVariant.USEPLUGIN)
	@PluginVariant(variantLabel = "Calculate Fitness/Optimal Alignment with the Incremental Conformance Checker", requiredParameterLabels = {})
	public String CheckForConformanceWithICCWithoutUI(final UIPluginContext context) throws Exception {
		String NET_PATH = "input/traffic.pnml";
		String LOG_PATH = "input/traffic.xes";
		
		XLog log = loadLog(LOG_PATH);
		PetrinetGraph net = importPNML(NET_PATH, context);
		System.out.println("Loaded log and net");
		
		TransEvClassMapping mapping = computeTransEventMapping(log, net);
		double delta=0.01;
		double alpha=0.99;
		double epsilon=0.01;
		double k=0.3;
		int initialSize=20;
		String goal="resource";
		boolean approximate=false;
		double resAssignmentThreshold = 0.2;
		
		ResourceAssignmentComputer resComputer = new ResourceAssignmentComputer(resAssignmentThreshold);
		ResourceAssignment assignment = resComputer.createResourceAssignment(log);
		
		XLog copyLog=(XLog) log.clone();
		IccParameters iccParameters=new IccParameters(delta, alpha, epsilon, k, initialSize, goal, approximate);		
		IncrementalReplayer replayer = new ResourceDeviationReplayer(iccParameters, assignment, copyLog);
//		IncrementalReplayer replayer = new ApproxResourceDeviationReplayer(iccParameters, assignment, copyLog);
		
		//make own parameter function for alignment/fitness
		AlignmentReplayResult result =calculateAlignmentWithICC(context, replayer, net, copyLog, iccParameters, mapping);

		System.out.println("Fitness         : "+result.getFitness());
		System.out.println("Time(ms)        : "+result.getTime());
		System.out.println("Log Size        : "+result.getLogSize());
		System.out.println("No AsynchMoves  : "+result.getTotalNoAsynchMoves());
		System.out.println("AsynchMoves abs : "+result.getAsynchMovesAbs().toString());
		System.out.println("AsynchMoves rel : "+result.getAsynchMovesRel().toString());
		
		//make own parameter function for alignment/fitness
		iccParameters= new IccParameters(delta, alpha, epsilon, k, initialSize, goal, approximate);
//		iccParameters.setCheckInternalQuality(true);
		replayer = new ApproxResourceDeviationReplayer(iccParameters, assignment, copyLog);
		copyLog=(XLog) log.clone();
		System.gc();
		AlignmentReplayResult result2 =calculateAlignmentWithICC(context, replayer, net, copyLog, iccParameters, mapping);

		
		System.out.println("Fitness         : "+result2.getFitness());
		System.out.println("Time(ms)        : "+result2.getTime());
		System.out.println("Log Size        : "+result2.getLogSize());
		System.out.println("No AsynchMoves  : "+result2.getTotalNoAsynchMoves());
		System.out.println("AsynchMoves abs : "+result2.getAsynchMovesAbs().toString());
		System.out.println("AsynchMoves rel : "+result2.getAsynchMovesRel().toString());
		
		//make own parameter function for alignment/fitness
//		iccParameters= new IccParameters(delta, alpha, epsilon, k, initialSize, goal, approximate);
//		iccParameters.setCheckExternalQuality(true);
//		iccParameters.getExternalQualityCheckContainer().addDirectlyFollowsChecking();
//		replayer = selectReplayer(goal, iccParameters);
//		copyLog=(XLog) log.clone();
//		System.gc();
//		
//		AlignmentReplayResult result3 =calculateAlignmentWithICC(context, replayer, net, copyLog, iccParameters, mapping);
//		System.out.println("Fitness         : "+result3.getFitness());
//		System.out.println("Time(ms)        : "+result3.getTime());
//		System.out.println("Log Size        : "+result3.getLogSize());
//		System.out.println("No AsynchMoves  : "+result3.getTotalNoAsynchMoves());
//		System.out.println("AsynchMoves abs : "+result3.getAsynchMovesAbs().toString());
//		System.out.println("AsynchMoves rel : "+result3.getAsynchMovesRel().toString());
//				

		
		return "done";
//		return result.toString();
	}
	
	

	@Plugin(name = "Calculate optimal alignments with Original replayer", returnLabels = { "Fitness" }, returnTypes = { String.class }, parameterLabels = {}, userAccessible = true)
	@UITopiaVariant(affiliation = "Humboldt-University Berlin", author = "Martin Bauer", email = "bauermax@informatik.hu-berlin.de", uiLabel = UITopiaVariant.USEPLUGIN)
	@PluginVariant(variantLabel = "Calculate All-opt alignments with original replayer", requiredParameterLabels = {0,1})
	//get input parameters if needed, create conformance checker and let it compute results
	//ui output stuff also to here
	public String calculateAlignmentWithOrigWithUI(final UIPluginContext context, PetrinetGraph net, XLog log) throws Exception {
		TransEvClassMapping mapping = constructTransEvMapping(context, log, net);
		AlignmentReplayResult result = calculateAlignmentWithOrig(context, net, log, mapping);
		System.out.println("Fitness         : "+result.getFitness());
		System.out.println("Time(ms)        : "+result.getTime());
		System.out.println("Log Size        : "+result.getLogSize());
		System.out.println("No AsynchMoves  : "+result.getTotalNoAsynchMoves());
		System.out.println("AsynchMoves abs : "+result.getAsynchMovesAbs().toString());
		System.out.println("AsynchMoves rel : "+result.getAsynchMovesRel().toString());
		return result.toString();
	}
	
	@Plugin(name = "Calculate fitness with original replayer", returnLabels = { "Fitness" }, returnTypes = { String.class }, parameterLabels = {}, userAccessible = true)
	@UITopiaVariant(affiliation = "Humboldt-University Berlin", author = "Martin Bauer", email = "bauermax@informatik.hu-berlin.de", uiLabel = UITopiaVariant.USEPLUGIN)
	@PluginVariant(variantLabel = "Calculate Fitness with the Incremental Conformance Checker", requiredParameterLabels = {0,1})
	//get input parameters if needed, create conformance checker and let it compute results
	//ui output stuff also to here
	//TODO call fitness calc function instead of calculating everything here
	public String calculateFitnessWithOrigWithUI(final UIPluginContext context, PetrinetGraph net, XLog log) throws Exception {
		TransEvClassMapping mapping = constructTransEvMapping(context, log, net);
		int nThreads = 2;
		int costUpperBound = Integer.MAX_VALUE;
		
		XEventClassifier eventClassifier=XLogInfoImpl.STANDARD_CLASSIFIER;
		XLogInfo summary = XLogInfoFactory.createLogInfo(log, eventClassifier);
		XEventClasses classes = summary.getEventClasses();
		
		Marking initialMarking = getInitialMarking(net);
		Marking finalMarking = getFinalMarking(net);
		
		
		ReplayerParameters parameters = new ReplayerParameters.Default(nThreads, costUpperBound, Debug.NONE);
		Replayer traceReplayer = new Replayer(parameters, (Petrinet) net, initialMarking, finalMarking, classes, mapping, true);
		PNRepResult result;
		double fitness=0;
		long startingTimeInMillis=System.currentTimeMillis();
		try {
			result = traceReplayer.computePNRepResult(Progress.INVISIBLE, log);
			fitness = (Double) result.getInfo().get("Trace Fitness");
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		long endingTimeInMillis=System.currentTimeMillis();
		long totalTimeinMillis=endingTimeInMillis-startingTimeInMillis;
		System.out.println("Fitness: "+fitness+", Traces: "+log.size()+", Time (ms): "+totalTimeinMillis);
		return fitness+", "+log.size()+", "+totalTimeinMillis;
	}
	
	public AlignmentReplayResult calculateFitnessWithOrig(final UIPluginContext context, PetrinetGraph net, XLog log, TransEvClassMapping mapping) {
		int nThreads = 2;
		int costUpperBound = Integer.MAX_VALUE;
		
		XEventClassifier eventClassifier=XLogInfoImpl.STANDARD_CLASSIFIER;
		XLogInfo summary = XLogInfoFactory.createLogInfo(log, eventClassifier);
		XEventClasses classes = summary.getEventClasses();
		
		Marking initialMarking = getInitialMarking(net);
		Marking finalMarking = getFinalMarking(net);
		
		
		ReplayerParameters parameters = new ReplayerParameters.Default(nThreads, costUpperBound, Debug.NONE);
		Replayer traceReplayer = new Replayer(parameters, (Petrinet) net, initialMarking, finalMarking, classes, mapping, true);
		PNRepResult result;
		double fitness=0;
		long startingTimeInMillis=System.currentTimeMillis();
		try {
			result = traceReplayer.computePNRepResult(Progress.INVISIBLE, log);
			fitness = (Double) result.getInfo().get("Trace Fitness");
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		long endingTimeInMillis=System.currentTimeMillis();
		long totalTimeinMillis=endingTimeInMillis-startingTimeInMillis;
		return new AlignmentReplayResult(fitness, totalTimeinMillis, log.size(), 0, null, null);
	}

	/**
	 *Two alignment functions (ICC and original) without parameter setting ui.
	 *Use these for automated evaluation.
	 */
	
	
	public AlignmentReplayResult calculateAlignmentWithICC(final UIPluginContext context, IncrementalReplayer replayer, PetrinetGraph net, XLog log, IccParameters parameters, TransEvClassMapping mapping) 
	{
		IncrementalConformanceChecker icc =new IncrementalConformanceChecker(context, replayer, parameters, log, net);
		IccResult iccresult = icc.apply(context, log, net, mapping);
		Map<String, Integer> asynchMoveAbs=new TreeMap<String, Integer>();
		Map<String, Double> asynchMoveRel=new TreeMap<String, Double>();
		
		if(parameters.getGoal().equals("alignment")) {
			int asynchMovesSize=iccresult.getAlignmentContainer().getAsynchMoves().size();
	
			for (String key:iccresult.getAlignmentContainer().getAsynchMoves().elementSet()) {
				int absValue=iccresult.getAlignmentContainer().getAsynchMoves().count(key);
				double relValue=(double)absValue/(double)asynchMovesSize;
				asynchMoveAbs.put(key, absValue);
				asynchMoveRel.put(key, relValue);
			}
			AlignmentReplayResult result = new AlignmentReplayResult(iccresult.getFitness(), iccresult.getTime(), iccresult.getTraces(), asynchMovesSize, asynchMoveAbs, asynchMoveRel);
			return result;
		}
		else {
			AlignmentReplayResult result = new AlignmentReplayResult(iccresult.getFitness(), iccresult.getTime(), iccresult.getTraces(), -1, asynchMoveAbs, asynchMoveRel);
			return result;		
		}
	}
	
	//TODO differentiate between success and duplicate traces upon evaluation
	public AlignmentReplayResult calculateAlignmentWithOrig(final UIPluginContext context, PetrinetGraph net, XLog log, TransEvClassMapping mapping) throws InterruptedException, ExecutionException {
		// init multiset for result
		long startingtime =System.currentTimeMillis();
		Multiset<String> asynchronousMoveBag=TreeMultiset.create();
		
		int nThreads = 2;
		int costUpperBound = Integer.MAX_VALUE;
		
		XEventClassifier eventClassifier=XLogInfoImpl.STANDARD_CLASSIFIER;
		XLogInfo summary = XLogInfoFactory.createLogInfo(log, eventClassifier);
		XEventClasses classes = summary.getEventClasses();
		
		Marking initialMarking = getInitialMarking(net);
		Marking finalMarking = getFinalMarking(net);
		
		ReplayerParameters parameters = new ReplayerParameters.Default(nThreads, costUpperBound, Debug.NONE);
		Replayer replayer = new Replayer(parameters, (Petrinet) net, initialMarking, finalMarking, classes, mapping, true);
		
		PNRepResult ReplayResult;

		ReplayResult = replayer.computePNRepResult(Progress.INVISIBLE, log);

		// obtain the results one by one.
		for(SyncReplayResult replayResult : ReplayResult) {

			for (int j=0;j<replayResult.getStepTypes().size();j++) {
				if(replayResult.getStepTypes().get(j).toString().equals("Log move") || replayResult.getStepTypes().get(j).toString().equals("Model move")) {
					//System.out.println(replayResult.getNodeInstance().get(j).toString());
					if(replayResult.getStepTypes().get(j).toString().equals("Model move")) {
						if(mapping.containsKey(replayResult.getNodeInstance().get(j))) {
							asynchronousMoveBag.add(mapping.get(replayResult.getNodeInstance().get(j)).toString());
						}
						else {
							asynchronousMoveBag.add((replayResult.getNodeInstance().get(j)).toString());
						}
					}
					if(replayResult.getStepTypes().get(j).toString().equals("Log move")) {
						asynchronousMoveBag.add((replayResult.getNodeInstance().get(j)).toString());
						
						//asynchronousMoveBag.add(nodeInstance.split("\\+")[0]);
					}
				}
				//System.out.println();
			}
		}
		
		//build result data sets
		Map<String, Integer> asynchMoveAbs=new TreeMap<String, Integer>();
		Map<String, Double> asynchMoveRel=new TreeMap<String, Double>();
		for (String key:asynchronousMoveBag.elementSet()) {
			int absValue=asynchronousMoveBag.count(key);
			double relValue=(double)absValue/(double)asynchronousMoveBag.size();
			asynchMoveAbs.put(key, absValue);
			asynchMoveRel.put(key, relValue);
		}
		
		long endingtime = System.currentTimeMillis();
		long totalTimeinMillis=endingtime-startingtime;
		
		AlignmentReplayResult result=new AlignmentReplayResult(-1.0, totalTimeinMillis, log.size(), asynchronousMoveBag.size(), asynchMoveAbs, asynchMoveRel);
		return result;
	}
	
	
	
	
	
	/**
	 * Evaluation plugin. Uses above four functions for multiple consecutive 
	 * evaluation runs with differing parameters and writes the results to disk.
	 */
	@Plugin(name = "Evaluate ICC", returnLabels = { "Evaluation Results" }, returnTypes = { String.class }, parameterLabels = {}, userAccessible = true)
	@UITopiaVariant(affiliation = "Humboldt-University Berlin", author = "Martin Bauer", email = "bauermax@informatik.hu-berlin.de", uiLabel = UITopiaVariant.USEPLUGIN)
	@PluginVariant(variantLabel = "Evaluate ICC", requiredParameterLabels = {0,1})
	public String evaluateIccAlignment(final UIPluginContext context, PetrinetGraph net, XLog log) throws Exception {
		TransEvClassMapping mapping = constructTransEvMapping(context, log, net);
		
		System.out.println("Executing five runs of "
				+ "the Original Replayer(Fitness) for Distance Measurement.");
//		single execution of original replayer for distribution distance measurement
		PrintWriter orig_out=new PrintWriter("orig_fitness_times"+"_"+System.currentTimeMillis());

		
		//AlignmentReplayResult origResult = calculateFitnessWithOrig(context, net, log, mapping);
		//orig_out.write((int) origResult.getTime()+"\n");
		//origResult = calculateFitnessWithOrig(context, net, log, mapping);
		//orig_out.write((int) origResult.getTime()+"\n");
		//origResult = calculateFitnessWithOrig(context, net, log, mapping);
		//orig_out.write((int) origResult.getTime()+"\n");
		//origResult = calculateFitnessWithOrig(context, net, log, mapping);
		//orig_out.write((int) origResult.getTime()+"\n");
		//origResult = calculateFitnessWithOrig(context, net, log, mapping);
		//orig_out.write((int) origResult.getTime()+"\n");
		orig_out.close();
		//Map<String, Double> origDistrib = origResult.getAsynchMovesRel();
		//TODO single execution of original replayer to get true distribution for comparison
		//Replayers are missing for proper usage
		PrintWriter approxAlignment_out =new PrintWriter("ICC_alignment_approx"+"_"+System.currentTimeMillis());
		PrintWriter Alignment_out = new PrintWriter("ICC_alignment_"+System.currentTimeMillis());
		PrintWriter approxFitness_out = new PrintWriter("ICC_fitness_approx"+"_"+System.currentTimeMillis());
		PrintWriter Fitness_out = new PrintWriter("ICC_fitness_"+System.currentTimeMillis());
		
		approxAlignment_out.write("delta; alpha; epsilon; k; initSize; fitness; time; logSize; totalNoAsynchMoves; asynchMovesAbs; asynchMovesRel; approximationMode; distToOriginal\n");
		Alignment_out.write("delta; alpha; epsilon; k; initSize; fitness; time; logSize; totalNoAsynchMoves; asynchMovesAbs; asynchMovesRel; approximationMode; distToOriginal\n");
		approxFitness_out.write("delta; alpha; epsilon; k; initSize; fitness; time; logSize; totalNoAsynchMoves; asynchMovesAbs; asynchMovesRel; approximationMode; distToOriginal\n");
		Fitness_out.write("delta; alpha; epsilon; k; initSize; fitness; time; logSize; totalNoAsynchMoves; asynchMovesAbs; asynchMovesRel; approximationMode; distToOriginal\n");
	
		int repetitions=5;
		double[] deltas= {0.01, 0.05, 0.1};
		double[] alphas= {0.9, 0.95, 0.99};
		double[] epsilons= {0.01, 0.05, 0.1};
		double[] ks= {1.0/3.0, 2.0/3.0};//irrelevant for non approximating alignment calculation
		int[] initSizes= {1};//irrelevant for alignment
		String[] approximationModes= {IccParameters.NONALIGNING, IccParameters.PREFIXSUFFIX};

		ArrayList<IccParameters> list = new ArrayList<IccParameters>();		//initialsize of 0 removes parameter from framework
		//new IccParameters(delta, alpha, epsilon, 0, 0, goal, approximate);	
		list.add(new IccParameters(0.01, 0.99, 0.01, 1.0/3.0 ,1, "fitness", false));
		list.add(new IccParameters(0.05, 0.99, 0.01, 1.0/3.0 ,1, "fitness", false));
		list.add(new IccParameters(0.1, 0.99, 0.01, 1.0/3.0 ,1, "fitness", false));
		list.add(new IccParameters(0.01, 0.9, 0.01, 1.0/3.0 ,1, "fitness", false));
		list.add(new IccParameters(0.01, 0.95, 0.01, 1.0/3.0 ,1, "fitness", false));
		//list.add(new IccParameters(0.01, 0.99, 0.01, 1.0/3.0 ,1, "fitness", false));
		//list.add(new IccParameters(0.01, 0.99, 0.01, 1.0/3.0 ,1, "fitness", false));
		list.add(new IccParameters(0.01, 0.99, 0.05, 1.0/3.0 ,1, "fitness", false));
		list.add(new IccParameters(0.01, 0.99, 0.1, 1.0/3.0 ,1, "fitness", false));
		//list.add(new IccParameters(0.01, 0.99, 0.01, 1.0/3.0 ,1, "fitness", false));
		list.add(new IccParameters(0.01, 0.99, 0.01, 2.0/3.0 ,1, "fitness", false));
		list.add(new IccParameters(0.01, 0.99, 0.01, 3.0/3.0 ,1, "fitness", false));

		
		
		//double[] deltas= {0.05};
		//double[] alphas= {0.99};
		//double[] epsilons= {0.05};
		//double[] ks= {0.8};//irrelevant for non approximating alignment calculation
		//int[] initSizes= {1};//irrelevant for alignment
				
		//first fitness without approximation
		//TODO rewrite using guavas Cartesian Product
		IncrementalReplayer replayer = null;
		String goal="fitness";
		boolean approximate=false;
		int totalRepetitions=list.size()*repetitions;
		int repetition=0;
		//for(double delta : deltas) {
			//for(double alpha : alphas) {
				//for(double epsilon: epsilons) {
					//for(double k: ks) {
						//for(int initSize : initSizes) {
						/*for(IccParameters params : list) {
							for(int i=0;i<repetitions;i++) {
								XLog copyLog=(XLog) log.clone();
								System.gc();
								System.out.println("(FitnessReplayer)Repetition "+(++repetition)+" of "+totalRepetitions);
								String toOut=params.getDelta()+"; "+params.getAlpha()+"; "+params.getEpsilon()+"; "+params.getK()+"; "+params.getInitialSize()+"; ";
								
								//IccParameters iccParameters=new IccParameters(delta, alpha, epsilon, 0, 0, goal, approximate);	
								replayer=selectReplayer(goal, params);
								//new FitnessReplayer(iccParameters);
								AlignmentReplayResult result=calculateAlignmentWithICC(context,replayer, net,  copyLog, params, mapping);
								
								//get distance to orig distr
								double difference=Math.abs(origResult.getFitness()-result.getFitness());
								toOut=toOut+result.toString()+"; -;"+difference;
								Fitness_out.write(toOut+"\n");
							}
						}*/
						//}
					//}
				//}
			//}
		//}
		Fitness_out.close();
		
		
		
		list = new ArrayList<IccParameters>();		//initialsize of 0 removes parameter from framework
		//new IccParameters(delta, alpha, epsilon, 0, 0, goal, approximate);	
		list.add(new IccParameters(0.01, 0.99, 0.01, 1.0/3.0 ,1, "fitness", true));
		list.add(new IccParameters(0.05, 0.99, 0.01, 1.0/3.0 ,1, "fitness", true));
		list.add(new IccParameters(0.1, 0.99, 0.01, 1.0/3.0 ,1, "fitness", true));
		list.add(new IccParameters(0.01, 0.9, 0.01, 1.0/3.0 ,1, "fitness", true));
		list.add(new IccParameters(0.01, 0.95, 0.01, 1.0/3.0 ,1, "fitness", true));
		//list.add(new IccParameters(0.01, 0.99, 0.01, 1.0/3.0 ,1, "fitness", true));
		//list.add(new IccParameters(0.01, 0.99, 0.01, 1.0/3.0 ,1, "fitness", true));
		list.add(new IccParameters(0.01, 0.99, 0.05, 1.0/3.0 ,1, "fitness", true));
		list.add(new IccParameters(0.01, 0.99, 0.1, 1.0/3.0 ,1, "fitness", true));
		//list.add(new IccParameters(0.01, 0.99, 0.01, 1.0/3.0 ,1, "fitness", true));
		list.add(new IccParameters(0.01, 0.99, 0.01, 2.0/3.0 ,1, "fitness", true));
		list.add(new IccParameters(0.01, 0.99, 0.01, 3.0/3.0 ,1, "fitness", true));
		
		goal="fitness";
		approximate=true;
		totalRepetitions=list.size()*repetitions;
		repetition=0;
		//for(double delta : deltas) {
			//for(double alpha : alphas) {
				//for(double epsilon: epsilons) {
					//for(double k: ks) {
						//for(int initSize : initSizes) {
						/*for(IccParameters params : list) {
							for(int i=0;i<repetitions;i++) {
								XLog copyLog=(XLog) log.clone();
								System.gc();
								System.out.println("(ApproxFitnessReplayer)Repetition "+(++repetition)+" of "+totalRepetitions);
								String toOut=params.getDelta()+"; "+params.getAlpha()+"; "+params.getEpsilon()+"; "+params.getK()+"; "+params.getInitialSize()+"; ";
								
								//IccParameters iccParameters=new IccParameters(delta, alpha, epsilon, k, initSize, goal, approximate);

								replayer=selectReplayer(goal, params);
								AlignmentReplayResult result=calculateAlignmentWithICC(context,replayer, net,  copyLog, params, mapping);
								
								//get distance to orig distr
								double difference=Math.abs(origResult.getFitness()-result.getFitness());
								
								toOut=toOut+result.toString()+" ; -;"+difference;
								approxFitness_out.write(toOut+"\n");
							}
						}*/
						//}
					//}
				//}
			//}
		//}
		approxFitness_out.close();
		
		
		System.out.println("Executing five runs of "
				+ "the Original Replayer(Alignment) for Distance Measurement.");
		AlignmentReplayResult origResult = calculateAlignmentWithOrig(context, net, log, mapping);
		Map<String, Double> origDistrib = origResult.getAsynchMovesRel();
		
		PrintWriter orig_alignment_out=new PrintWriter("orig_alignment_times"+"_"+System.currentTimeMillis());

		
		//origResult = calculateAlignmentWithOrig(context, net, log, mapping);
		//orig_alignment_out.write((int) origResult.getTime()+"\n");
		//origResult = calculateAlignmentWithOrig(context, net, log, mapping);
		//orig_alignment_out.write((int) origResult.getTime()+"\n");
		//origResult = calculateAlignmentWithOrig(context, net, log, mapping);
		//orig_alignment_out.write((int) origResult.getTime()+"\n");
		//origResult = calculateAlignmentWithOrig(context, net, log, mapping);
		//orig_alignment_out.write((int) origResult.getTime()+"\n");
		//origResult = calculateAlignmentWithOrig(context, net, log, mapping);
		//orig_alignment_out.write((int) origResult.getTime()+"\n");
		//orig_alignment_out.close();
		
		list = new ArrayList<IccParameters>();		//initialsize of 0 removes parameter from framework
		//new IccParameters(delta, alpha, epsilon, 0, 0, goal, approximate);	
		list.add(new IccParameters(0.01, 0.99, 0.01, 0.1 ,1, "alignment", false));
		list.add(new IccParameters(0.05, 0.99, 0.01, 0.1 ,1, "alignment", false));
		list.add(new IccParameters(0.1, 0.99, 0.01, 0.1 ,1, "alignment", false));
		list.add(new IccParameters(0.01, 0.9, 0.01, 0.1 ,1, "alignment", false));
		list.add(new IccParameters(0.01, 0.95, 0.01, 0.1 ,1, "alignment", false));
		//list.add(new IccParameters(0.01, 0.99, 0.01, 1.0/3.0 ,1, "alignment", false));
		//list.add(new IccParameters(0.01, 0.99, 0.01, 1.0/3.0 ,1, "alignment", false));
		list.add(new IccParameters(0.01, 0.99, 0.05, 0.1 ,1, "alignment", false));
		list.add(new IccParameters(0.01, 0.99, 0.1, 0.1 ,1, "alignment", false));
		//list.add(new IccParameters(0.01, 0.99, 0.01, 1.0/3.0 ,1, "alignment", false));
		list.add(new IccParameters(0.01, 0.99, 0.01, 0.2 ,1, "alignment", false));
		list.add(new IccParameters(0.01, 0.99, 0.01, 0.3 ,1, "alignment", false));
		
		
		goal="alignment";
		approximate=false;
		totalRepetitions=list.size()*repetitions;
		repetition=0;
		//for(double delta : deltas) {
			//for(double alpha : alphas) {
				//for(double epsilon: epsilons) {
					//for(double k: ks) {
						//for(int initSize : initSizes) {
						/*for(IccParameters params : list) {
							for(int i=0;i<repetitions;i++) {
								XLog copyLog=(XLog) log.clone();
								System.gc();
								System.out.println("(AlignmentReplayer)Repetition "+(++repetition)+" of "+totalRepetitions);
								String toOut=params.getDelta()+"; "+params.getAlpha()+"; "+params.getEpsilon()+"; "+params.getK()+"; "+params.getInitialSize()+"; ";
								
								//IccParameters iccParameters=new IccParameters(delta, alpha, epsilon, 0, 0, goal, approximate);

								replayer=selectReplayer(goal, params);
								
								AlignmentReplayResult result=calculateAlignmentWithICC(context,replayer, net,  copyLog, params, mapping);
								
								//get distance to orig distr
								Map<String, Double> iccDistr=result.getAsynchMovesRel();
								double difference=0.0;
								double d1=0.0;
								double d2=0.0;
								for(String activity :  origDistrib.keySet()) {
									d1=origDistrib.get(activity);
									if (!iccDistr.containsKey(activity)) {
										d2=0.0;
									}
									else d2=iccDistr.get(activity);
									double dif=Math.pow(d1-d2,2);
									difference+=dif;
								}
								difference=Math.sqrt(difference);
								
								toOut=toOut+result.toString()+" ; -;"+difference;
								Alignment_out.write(toOut+"\n");
							}
						}*/
						//}
					//}
				//}
			//}
		//}
		Alignment_out.close();

		
		list = new ArrayList<IccParameters>();		//initialsize of 0 removes parameter from framework
		//new IccParameters(delta, alpha, epsilon, 0, 0, goal, approximate);	
		list.add(new IccParameters(0.01, 0.99, 0.01, 0.2 ,1, "alignment", true, IccParameters.NONALIGNING));
		list.add(new IccParameters(0.05, 0.99, 0.01, 0.2 ,1, "alignment", true, IccParameters.NONALIGNING));
		list.add(new IccParameters(0.1, 0.99, 0.01, 0.2 ,1, "alignment", true, IccParameters.NONALIGNING));
		list.add(new IccParameters(0.01, 0.9, 0.01, 0.2 ,1, "alignment", true, IccParameters.NONALIGNING));
		list.add(new IccParameters(0.01, 0.95, 0.01, 0.2 ,1, "alignment", true, IccParameters.NONALIGNING));
		//list.add(new IccParameters(0.01, 0.99, 0.01, 1.0/3.0 ,1, "alignment", true, IccParameters.NONALIGNING));
		//list.add(new IccParameters(0.01, 0.99, 0.01, 1.0/3.0 ,1, "alignment", true, IccParameters.NONALIGNING));
		list.add(new IccParameters(0.01, 0.99, 0.05, 0.2 ,1, "alignment", true, IccParameters.NONALIGNING));
		list.add(new IccParameters(0.01, 0.99, 0.1, 0.2 ,1, "alignment", true, IccParameters.NONALIGNING));
		list.add(new IccParameters(0.01, 0.99, 0.01, 0.1 ,1, "alignment", true, IccParameters.NONALIGNING));
		//list.add(new IccParameters(0.01, 0.99, 0.01, 0.2 ,1, "alignment", true, IccParameters.NONALIGNING));
		list.add(new IccParameters(0.01, 0.99, 0.01, 0.3 ,1, "alignment", true, IccParameters.NONALIGNING));	
		/*
		list.add(new IccParameters(0.01, 0.99, 0.01, 1.0/3.0 ,1, "alignment", true, IccParameters.PREFIXSUFFIX));
		list.add(new IccParameters(0.05, 0.99, 0.01, 1.0/3.0 ,1, "alignment", true, IccParameters.PREFIXSUFFIX));
		list.add(new IccParameters(0.1, 0.99, 0.01, 1.0/3.0 ,1, "alignment", true, IccParameters.PREFIXSUFFIX));
		list.add(new IccParameters(0.01, 0.9, 0.01, 1.0/3.0 ,1, "alignment", true, IccParameters.PREFIXSUFFIX));
		list.add(new IccParameters(0.01, 0.95, 0.01, 1.0/3.0 ,1, "alignment", true, IccParameters.PREFIXSUFFIX));
		//list.add(new IccParameters(0.01, 0.99, 0.01, 1.0/3.0 ,1, "alignment", true, IccParameters.PREFIXSUFFIX));
		//list.add(new IccParameters(0.01, 0.99, 0.01, 1.0/3.0 ,1, "alignment", true, IccParameters.PREFIXSUFFIX));
		list.add(new IccParameters(0.01, 0.99, 0.05, 1.0/3.0 ,1, "alignment", true, IccParameters.PREFIXSUFFIX));
		list.add(new IccParameters(0.01, 0.99, 0.1, 1.0/3.0 ,1, "alignment", true, IccParameters.PREFIXSUFFIX));
		//list.add(new IccParameters(0.01, 0.99, 0.01, 1.0/3.0 ,1, "alignment", true, IccParameters.PREFIXSUFFIX));
		list.add(new IccParameters(0.01, 0.99, 0.01, 2.0/3.0 ,1, "alignment", true, IccParameters.PREFIXSUFFIX));
		list.add(new IccParameters(0.01, 0.99, 0.01, 3.0/3.0 ,1, "alignment", true, IccParameters.PREFIXSUFFIX));
		*/
		
		goal="alignment";
		approximate=true;
		totalRepetitions=list.size()*repetitions;
		repetition=0;
		//for(double delta : deltas) {
			//for(double alpha : alphas) {
				//for(double epsilon: epsilons) {
					//for(double k: ks) {
						//for(int initSize : initSizes) {
							//for(String approximationMode : approximationModes) {
							for(IccParameters params : list) {
								for(int i=0;i<repetitions;i++) {
									XLog copyLog=(XLog) log.clone();
									System.gc();
									System.out.println("(ApproxAlignmentReplayer)Repetition "+(++repetition)+" of "+totalRepetitions);
									String toOut=params.getDelta()+"; "+params.getAlpha()+"; "+params.getEpsilon()+"; "+params.getK()+"; "+params.getInitialSize()+"; ";
									
									//IccParameters iccParameters=new IccParameters(delta, alpha, epsilon, 0, 0, goal, approximate, approximationMode);
									
									replayer=selectReplayer(goal, params);
	
									
									AlignmentReplayResult result=calculateAlignmentWithICC(context,replayer, net,  copyLog, params, mapping);
									
									//get distance to orig distr
									Map<String, Double> iccDistr=result.getAsynchMovesRel();
									double difference=0.0;
									double d1=0.0;
									double d2=0.0;
									for(String activity :  origDistrib.keySet()) {
										d1=origDistrib.get(activity);
										if (!iccDistr.containsKey(activity)) {
											d2=0.0;
										}
										else d2=iccDistr.get(activity);
										double dif=Math.pow(d1-d2,2);
										difference+=dif;
									}
									difference=Math.sqrt(difference);
									
									toOut=toOut+result.toString()+" ;"+params.getApproximationMode()+"; "+difference;
									approxAlignment_out.write(toOut+"\n");
								}
							}
							//}
						//}
					//}
				//}
			//}
		//}
		approxAlignment_out.close();
		return "Evaluation complete";
	}
	
	/**
	 * Evaluation plugin. Uses above four functions for multiple consecutive 
	 * evaluation runs with differing parameters and writes the results to disk.
	 */
	@Plugin(name = "000 Evaluate quality checking", returnLabels = { "Evaluation Results" }, returnTypes = { String.class }, parameterLabels = {}, userAccessible = true)
	@UITopiaVariant(affiliation = "Humboldt-University Berlin", author = "Martin Bauer", email = "bauermax@informatik.hu-berlin.de", uiLabel = UITopiaVariant.USEPLUGIN)
	@PluginVariant(variantLabel = "Evaluate quality checking", requiredParameterLabels = {})
	public String evaluateIccQualityChecking(final UIPluginContext context) throws Exception {
		String out_file = "output/quality_check_results" + System.currentTimeMillis() + ".csv";
		PrintWriter writer = new PrintWriter(out_file);
		writer.write("log; alpha; epsilon; quality checker; dfDistrib; attrib; times triggered; fitness; time; logSize; totalNoAsynchMoves; asynchMovesAbs; asynchMovesRel; approx;  distToOriginal; original\n");
		
		int repetitions = 5;
		double alpha = 0.99;
		double epsilon = 0.10;
		
		String[] logs = new String[]{"traffic", "bpi12"};
//		String[] logs = new String[]{"traffic"};
		for (String log : logs) {
			evaluateIccQualityChecking(context, log, writer, repetitions, alpha, epsilon);
		}
		writer.close();
		return "evaluation completed";
	}
	
	
	private void evaluateIccQualityChecking(final UIPluginContext context, String name, PrintWriter writer,
			int repetitions, double alpha, double epsilon) throws Exception {

		String NET_PATH = "input/" + name + ".pnml";
		String LOG_PATH = "input/" + name + ".xes";
		
		XLog log = loadLog(LOG_PATH);
		PetrinetGraph net = importPNML(NET_PATH, context);
		System.out.println("Loaded log and net");
		TransEvClassMapping mapping = computeTransEventMapping(log, net);

		double origFitness = 0;
		if (name.equals("bpi14")) {
			origFitness = 0.9008334329271833; 
		}
		if (name.equals("traffic")) {
			origFitness = 0.9823429057173879;
		}
		if (name.equals("bpi12")) {
			origFitness = 0.7310599598129743;
		}

		ArrayList<IccParameters> list = new ArrayList<IccParameters>();
		IccParameters setting;
//		setting =  new IccParameters(0.01, alpha, epsilon, 1.0/3.0 ,1, "fitness", false);
//		list.add(setting);
//		setting =  new IccParameters(0.01, alpha, epsilon, 1.0/3.0 ,1, "fitness", false);
//		setting.setCheckInternalQuality(true);
//		setting.getInternalQualityCheckContainer().addDirectlyFollowsChecking();
//		list.add(setting);
//		setting =  new IccParameters(0.01, alpha, epsilon, 1.0/3.0 ,1, "fitness", false);
//		setting.setCheckInternalQuality(true);
//		addDataAttribute(setting, name, true);
//		list.add(setting);
//		setting =  new IccParameters(0.01, alpha, epsilon, 1.0/3.0 ,1, "fitness", false);
//		setting.setCheckInternalQuality(true);
//		setting.getInternalQualityCheckContainer().addDirectlyFollowsChecking();
//		addDataAttribute(setting, name, true);
//		list.add(setting);
		
		// external quality check
		setting =  new IccParameters(0.01, alpha, epsilon, 1.0/3.0 ,1, "fitness", false);
		setting.setCheckExternalQuality(true);
		setting.getExternalQualityCheckContainer().addDirectlyFollowsChecking();
		list.add(setting);
		setting =  new IccParameters(0.01,  alpha, epsilon, 1.0/3.0 ,1, "fitness", false);
		setting.setCheckExternalQuality(true);
		addDataAttribute(setting, name, false);
		list.add(setting);
		setting =  new IccParameters(0.01,  alpha, epsilon, 1.0/3.0 ,1, "fitness", false);
		setting.setCheckExternalQuality(true);
		setting.getExternalQualityCheckContainer().addDirectlyFollowsChecking();
		addDataAttribute(setting, name, false);
		list.add(setting);
		
		IncrementalReplayer replayer = null;
		String goal="fitness";
		int totalRepetitions=list.size()*repetitions;
		int repetition=0;
		for(IccParameters params : list) {
			for(int i=0;i<repetitions;i++) {
				XLog copyLog=(XLog) log.clone();
				System.gc();
				System.out.println("(FitnessReplayer)Repetition "+(++repetition)+" of "+totalRepetitions + " for log: " + name);

				replayer=selectReplayer(goal, params);
				AlignmentReplayResult result=calculateAlignmentWithICC(context,replayer, net,  copyLog, params, mapping);

				String toOut = name + "; " + alpha + "; " + epsilon + "; "; 
				if (params.isCheckInternalQuality()) {
					QualityCheckManager checker = params.getInternalQualityCheckContainer();
					toOut = toOut + "internal; " + checker.checkDirectlyFollows() + "; " + checker.getCheckedAttributes() +"; " + checker.timesTriggered() + "; ";
					checker.resetAll();
				}
				else if (params.isCheckExternalQuality()) {
					QualityCheckManager checker = params.getExternalQualityCheckContainer();
					toOut = toOut + "external; " + checker.checkDirectlyFollows() + "; "+ checker.getCheckedAttributes()+"; " + checker.timesTriggered() + "; ";
					checker.resetAll();
				}
				else {
					toOut = toOut + "no quality check; ; ; ;";
				}
				
				//get distance to orig distr
				double difference=Math.abs(origFitness-result.getFitness());
				toOut=toOut+result.toString()+"; -;"+difference + ";" + origFitness;
				toOut = toOut.replace('.', ',');
				writer.write(toOut+"\n");
				System.gc();
				
			}
		}
	}
	
	private void addDataAttribute(IccParameters parameters, String logname, boolean internal) {
		QualityCheckManager qualityCheckManager;
		if (internal) {
			qualityCheckManager = parameters.getInternalQualityCheckContainer();
		} else {
			qualityCheckManager = parameters.getExternalQualityCheckContainer();
		}
		if (logname.equals("traffic")) {
			qualityCheckManager.addNumericEventAttribute("Create Fine", "amount");
		}
		if (logname.equals("bpi12")) {
			qualityCheckManager.addNominalEventAttribute("W_Completeren aanvraag", "org:resource");
		}
		if (logname.equals("bpi14")) {
			qualityCheckManager.addNominalEventAttribute("Closed", "org:resource");
		}
	}


	
	
	

	@Plugin(name = "Evaluate Orig", returnLabels = { "Evaluation Results" }, returnTypes = { String.class }, parameterLabels = {}, userAccessible = true)
	@UITopiaVariant(affiliation = "Humboldt-University Berlin", author = "Martin Bauer", email = "bauermax@informatik.hu-berlin.de", uiLabel = UITopiaVariant.USEPLUGIN)
	@PluginVariant(variantLabel = "Evaluate ICC", requiredParameterLabels = {0,1})
	public String evaluateOrigAlignment(final UIPluginContext context, PetrinetGraph net, XLog log) throws Exception {
		String goal="alignment";
		
		TransEvClassMapping mapping = constructTransEvMapping(context, log, net);
		PNManifestReplayer replayer=new PNManifestReplayer();
		IPNManifestReplayAlgorithm alg;
		PNManifestReplayerParameter parameter;
		Object[] obj = replayer.chooseAlgorithmAndParam(context, net, log);
		alg = (IPNManifestReplayAlgorithm) obj[0];
		parameter = (PNManifestReplayerParameter) obj[1];
		PrintWriter out = new PrintWriter("alignment_Orig_"+System.currentTimeMillis());
		out.write("fitness; time; logSize; totalNoAsynchMoves; asynchMovesAbs; asynchMovesRel\n");
		int repetitions=5;
		int repetition=0;
		for(int i=0;i<repetitions;i++) {
			System.out.println("Repetition "+(++repetition)+" of "+repetitions);
			String toOut="";
			AlignmentReplayResult result=calculateFitnessWithOrig(context,net, log, mapping);
			toOut=toOut+result.getTime();
			out.write(toOut+"\n");
		}
		out.close();
		return "Evaluation complete";
	}
	
	public TransEvClassMapping constructTransEvMapping(UIPluginContext context, XLog log, PetrinetGraph net){
		PNReplayerUI pnReplayerUI = new PNReplayerUI();
		Object[] resultConfiguration = pnReplayerUI.getConfiguration(context, net, log);
		if (resultConfiguration == null) {
			context.getFutureResult(0).cancel(true);
			System.out.println("Problem while constructing Mapping");
			return null;
		}
		return (TransEvClassMapping) resultConfiguration[PNReplayerUI.MAPPING];
	}
	
	
	private static Marking getFinalMarking(PetrinetGraph net) {
		Marking finalMarking = new Marking();

		for (Place p : net.getPlaces()) {
			if (net.getOutEdges(p).isEmpty())
				finalMarking.add(p);
		}

		return finalMarking;
	}

	private static Marking getInitialMarking(PetrinetGraph net) {
		Marking initMarking = new Marking();

		for (Place p : net.getPlaces()) {
			if (net.getInEdges(p).isEmpty())
				initMarking.add(p);
		}

		return initMarking;
	}
	
	public static TransEvClassMapping constructMappingBasedOnLabelEquality(PetrinetGraph net, XLog log,
			XEventClass dummyEvClass, XEventClassifier eventClassifier) {
		TransEvClassMapping mapping = new TransEvClassMapping(eventClassifier, dummyEvClass);

		XLogInfo summary = XLogInfoFactory.createLogInfo(log, eventClassifier);

		for (Transition t : net.getTransitions()) {
			boolean mapped = false;
			for (XEventClass evClass : summary.getEventClasses().getClasses()) {
				String id = evClass.getId();

				if (t.getLabel().equals(id)) {
					mapping.put(t, evClass);
					mapped = true;
					break;
				}
			}

			if (!mapped && !t.isInvisible()) {
				mapping.put(t, dummyEvClass);
			}

		}

		return mapping;
	}
	
	public IncrementalReplayer selectReplayer(String goal, IccParameters parameters) {
		if (goal.equals("fitness") && !parameters.isApproximate()) {
			return new FitnessReplayer(parameters);
		}
		if (goal.equals("fitness")&&parameters.isApproximate()) {
			return new ApproxFitnessReplayer(parameters);
		}
		if (parameters.getGoal().equals("alignment")&& !parameters.isApproximate()) {
			return new AlignmentReplayer(parameters);
		}
		if (parameters.getGoal().equals("alignment") && parameters.isApproximate()) {
			return new ApproxAlignmentReplayer(parameters);
		}
		return null;
	}
	
	public static XLog loadLog(String path) {
		File file = new File(path);
		XFactory  factory = XFactoryRegistry.instance().currentDefault();
		XParser parser = new XesXmlParser(factory);
		try {
    		XLog log = parser.parse(file).get(0);
    		XAttribute source = new XAttributeLiteralImpl("source", file.getName());
    		log.getAttributes().put("source", source);
    		System.out.println("Loaded log with: " + log.size() + " traces.");
    		return log;
		} catch (Exception e) {
			System.err.println("FAILED TO LOAD LOG FROM: " + file);
		}
		return null;
	}
	

	public static Petrinet importPNML(String fileName, PluginContext context) throws Exception {
		return (Petrinet) (importPNMLObjects(fileName, context))[0];
	}
	
	
	
	private static Object[] importPNMLObjects(String fileName, PluginContext context) throws Exception {
		File netFile = new File(fileName);
		PnmlImportUtils utils = new PnmlImportUtils();
		Pnml pnml = utils.importPnmlFromStream(context, new FileInputStream(netFile), netFile.getName(),
				netFile.length());
		//			net = (Petrinet) netImport.importFromStream(

		PetrinetGraph netGraph = PetrinetFactory.newPetrinet(pnml.getLabel() + " (imported from " + netFile.getName()
				+ ")");

		Marking marking = new Marking();
		GraphLayoutConnection layout = new GraphLayoutConnection(netGraph);
		pnml.convertToNet(netGraph, marking, layout);
		if(context!=null) {
			context.addConnection(new InitialMarkingConnection(netGraph, marking));
			context.addConnection(layout);
		}
		Petrinet net = (Petrinet) netGraph;
		
		for(org.processmining.models.graphbased.directed.petrinet.elements.Transition t : net.getTransitions())
			if(t.getLabel().equals(""))
				t.setInvisible(true);

		Object[] netObjects = new Object[]{net, marking};
		return netObjects;
	}
	
	public static  TransEvClassMapping computeTransEventMapping(XLog log, PetrinetGraph net) {
		XEventClass evClassDummy = EvClassLogPetrinetConnectionFactoryUI.DUMMY;
		TransEvClassMapping mapping = new TransEvClassMapping(XLogInfoImpl.STANDARD_CLASSIFIER, evClassDummy);
		XEventClasses ecLog = XLogInfoFactory.createLogInfo(log, XLogInfoImpl.STANDARD_CLASSIFIER).getEventClasses();
		for (Transition t : net.getTransitions()) {
			//TODO: this part is rather hacky, I'll admit.
			XEventClass eventClass = ecLog.getByIdentity(t.getLabel() + "+complete");
			if (eventClass == null) {
				eventClass = ecLog.getByIdentity(t.getLabel() + "+COMPLETE");
			}
			if (eventClass == null) {
				eventClass = ecLog.getByIdentity(t.getLabel());
			}
			
			if (eventClass != null) {
				mapping.put(t, eventClass);
			} else {
				mapping.put(t, evClassDummy);
				t.setInvisible(true);
			}
		}
		return mapping;
	}

}