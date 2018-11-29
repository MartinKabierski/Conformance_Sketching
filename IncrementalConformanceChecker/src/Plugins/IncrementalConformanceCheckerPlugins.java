package Plugins;

import java.io.PrintWriter;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;
import org.processmining.plugins.petrinet.manifestreplayer.PNManifestReplayer;
import org.processmining.plugins.petrinet.manifestreplayer.PNManifestReplayerParameter;
import org.processmining.plugins.petrinet.manifestreplayer.algorithms.IPNManifestReplayAlgorithm;
import org.processmining.plugins.petrinet.replayer.ui.PNReplayerUI;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.plugins.replayer.replayresult.SyncReplayResult;

import com.google.common.collect.Multiset;
import com.google.common.collect.TreeMultiset;

import Algorithm.IncrementalConformanceChecker;
import Replayer.AlignmentReplayer;
import Replayer.ApproxAlignmentReplayer;
import Replayer.ApproxFitnessReplayer;
import Replayer.FitnessReplayer;
import Replayer.IncrementalReplayer;
import Ressources.AlignmentReplayResult;
import Ressources.IccParameters;
import Ressources.IccResult;
import nl.tue.alignment.Progress;
import nl.tue.alignment.Replayer;
import nl.tue.alignment.ReplayerParameters;
import nl.tue.alignment.TraceReplayTask;
import nl.tue.alignment.algorithms.ReplayAlgorithm.Debug;
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
		double k=0.6;
		int initialSize=20;
		String goal="fitness";
		boolean approximate=false;
		IccParameters iccParameters=new IccParameters(delta, alpha, epsilon, k, initialSize, goal, approximate);

		IncrementalReplayer replayer = null;
		if (goal.equals("fitness")&& !iccParameters.isApproximate()) {
			replayer=new FitnessReplayer(iccParameters);
		}
		if (goal.equals("fitness")&&iccParameters.isApproximate()) {
			replayer=new ApproxFitnessReplayer(iccParameters);
		}
		if (iccParameters.getGoal().equals("alignment")&& !iccParameters.isApproximate()) replayer=new AlignmentReplayer(iccParameters);
		if (iccParameters.getGoal().equals("alignment") && iccParameters.isApproximate()) {
			replayer= new ApproxAlignmentReplayer(iccParameters);
			//replayer.init(context, net, log);
		}
				
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
		Replayer traceReplayer = new Replayer(parameters, (Petrinet) net, initialMarking, finalMarking, classes, mapping, false);
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
		Replayer traceReplayer = new Replayer(parameters, (Petrinet) net, initialMarking, finalMarking, classes, mapping, false);
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
	
	public AlignmentReplayResult calculateAlignmentWithOrig(final UIPluginContext context, PetrinetGraph net, XLog log, TransEvClassMapping mapping) {
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
		Replayer replayer = new Replayer(parameters, (Petrinet) net, initialMarking, finalMarking, classes, mapping, false);

		// timeout per trace in milliseconds
		int timeoutMilliseconds = 10 * 1000;
		// preprocessing time to be added to the statistics if necessary
		long preProcessTimeNanoseconds = 0;
		
		ExecutorService service = Executors.newFixedThreadPool(parameters.nThreads);
		
		@SuppressWarnings("unchecked")
		Future<TraceReplayTask>[] futures = new Future[log.size()];

		for (int i = 0; i < log.size(); i++) {
			// Setup the trace replay task
			TraceReplayTask task = new TraceReplayTask(replayer, parameters, log.get(i), i, timeoutMilliseconds,
					parameters.maximumNumberOfStates, preProcessTimeNanoseconds);

			// submit for execution
			futures[i] = service.submit(task);
		}
		// initiate shutdown and wait for termination of all submitted tasks.
		service.shutdown();
		
		// obtain the results one by one.
		for (int i = 0; i < log.size(); i++) {

			TraceReplayTask result;
			try {
				result = futures[i].get();
			} catch (Exception e) {
				// execution os the service has terminated.
				assert false;
				throw new RuntimeException("Error while executing replayer in ExecutorService. Interrupted maybe?", e);
			}
			SyncReplayResult replayResult = result.getSuccesfulResult();
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
		
		System.out.println("Executing one run of "
				+ "the Original Replayer(Fitness) for Distance Measurement.");
//		single execution of original replayer for distribution distance measurement
		AlignmentReplayResult origResult = calculateFitnessWithOrig(context, net, log, mapping);
		//Map<String, Double> origDistrib = origResult.getAsynchMovesRel();
		//TODO single execution of original replayer to get true distribution for comparison
		//Replayers are missing for proper usage
		PrintWriter approxAlignment_out =new PrintWriter("ICC_alignment_approx"+"_"+System.currentTimeMillis());
		PrintWriter Alignment_out = new PrintWriter("ICC_alignment_"+System.currentTimeMillis());
		PrintWriter approxFitness_out = new PrintWriter("ICC_fitness_approx"+"_"+System.currentTimeMillis());
		PrintWriter Fitness_out = new PrintWriter("ICC_fitness_"+System.currentTimeMillis());
		
		approxAlignment_out.write("delta; alpha; epsilon; k; initSize; fitness; time; logSize; totalNoAsynchMoves; asynchMovesAbs; asynchMovesRel; distToOriginal\n");
		Alignment_out.write("delta; alpha; epsilon; k; initSize; fitness; time; logSize; totalNoAsynchMoves; asynchMovesAbs; asynchMovesRel; distToOriginal\n");
		approxFitness_out.write("delta; alpha; epsilon; k; initSize; fitness; time; logSize; totalNoAsynchMoves; asynchMovesAbs; asynchMovesRel; distToOriginal\n");
		Fitness_out.write("delta; alpha; epsilon; k; initSize; fitness; time; logSize; totalNoAsynchMoves; asynchMovesAbs; asynchMovesRel; distToOriginal\n");
	
		int repetitions=10;
		double[] deltas= {0.05, 0.01};
		double[] alphas= {0.99, 0.95};
		double[] epsilons= {0.01, 0.03, 0.05};
		double[] ks= {1.0, 0.8};//irrelevant for non approximating alignment calculation
		int[] initSizes= {10, 20, 30};//irrelevant for alignment
		//double[] deltas= {0.05};
		//double[] alphas= {0.99};
		//double[] epsilons= {0.05};
		//double[] ks= {0.8};//irrelevant for non approximating alignment calculation
		//int[] initSizes= {1};//irrelevant for alignment
				
		//first fitness without approximation
		IncrementalReplayer replayer = null;
		String goal="fitness";
		boolean approximate=false;
		int totalRepetitions=repetitions*deltas.length*alphas.length*epsilons.length;
		int repetition=0;
		for(double delta : deltas) {
			for(double alpha : alphas) {
				for(double epsilon: epsilons) {
					//for(double k: ks) {
						//for(int initSize : initSizes) {
							for(int i=0;i<repetitions;i++) {
								XLog copyLog=(XLog) log.clone();
								System.gc();
System.out.println("(FitnessReplayer)Repetition "+(++repetition)+" of "+totalRepetitions);
								String toOut=delta+"; "+alpha+"; "+epsilon+"; "+-1+"; "+-1+"; ";
								
								IccParameters iccParameters=new IccParameters(delta, alpha, epsilon, 0, 0, goal, approximate);	
		replayer=new FitnessReplayer(iccParameters);
								AlignmentReplayResult result=calculateAlignmentWithICC(context,replayer, net,  copyLog, iccParameters, mapping);
								
								//get distance to orig distr
								double difference=Math.abs(origResult.getFitness()-result.getFitness());
								toOut=toOut+result.toString()+" ;"+difference;
								Fitness_out.write(toOut+"\n");
							}
						//}
					//}
				}
			}
		}
		Fitness_out.close();
		
		goal="fitness";
		approximate=true;
		totalRepetitions=repetitions*deltas.length*alphas.length*epsilons.length*ks.length*initSizes.length;
		repetition=0;
		for(double delta : deltas) {
			for(double alpha : alphas) {
				for(double epsilon: epsilons) {
					for(double k: ks) {
						for(int initSize : initSizes) {
							for(int i=0;i<repetitions;i++) {
								XLog copyLog=(XLog) log.clone();
								System.gc();
	System.out.println("(ApproxFitnessReplayer)Repetition "+(++repetition)+" of "+totalRepetitions);
								String toOut=delta+"; "+alpha+"; "+epsilon+"; "+k+"; "+initSize+"; ";
								
								IccParameters iccParameters=new IccParameters(delta, alpha, epsilon, k, initSize, goal, approximate);

	replayer=new ApproxFitnessReplayer(iccParameters);
								AlignmentReplayResult result=calculateAlignmentWithICC(context,replayer, net,  copyLog, iccParameters, mapping);
								
								//get distance to orig distr
								double difference=Math.abs(origResult.getFitness()-result.getFitness());
								
								toOut=toOut+result.toString()+" ;"+difference;
								approxFitness_out.write(toOut+"\n");
							}
						}
					}
				}
			}
		}
		approxFitness_out.close();
		
		
		System.out.println("Executing one run of "
				+ "the Original Replayer(Alignment) for Distance Measurement.");
		origResult = calculateAlignmentWithOrig(context, net, log, mapping);
		Map<String, Double> origDistrib = origResult.getAsynchMovesRel();
		
		goal="alignment";
		approximate=false;
		totalRepetitions=repetitions*deltas.length*alphas.length*epsilons.length;
		repetition=0;
		for(double delta : deltas) {
			for(double alpha : alphas) {
				for(double epsilon: epsilons) {
					//for(double k: ks) {
						//for(int initSize : initSizes) {
							for(int i=0;i<repetitions;i++) {
								XLog copyLog=(XLog) log.clone();
								System.gc();
System.out.println("(AlignmentReplayer)Repetition "+(++repetition)+" of "+totalRepetitions);
								String toOut=delta+"; "+alpha+"; "+epsilon+"; "+-1+"; "+-1+"; ";
								
								IccParameters iccParameters=new IccParameters(delta, alpha, epsilon, 0, 0, goal, approximate);

replayer=new AlignmentReplayer(iccParameters);
								if (iccParameters.getGoal().equals("alignment") && iccParameters.isApproximate()) {
									replayer= new ApproxAlignmentReplayer(iccParameters);
									//replayer.init(context, net, log);
								}
								
								AlignmentReplayResult result=calculateAlignmentWithICC(context,replayer, net,  copyLog, iccParameters, mapping);
								
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
									difference+=Math.abs(d1-d2);
								}
								difference=difference/origDistrib.size();
								
								toOut=toOut+result.toString()+" ;"+difference;
								Alignment_out.write(toOut+"\n");
							}
						//}
					//}
				}
			}
		}
		Alignment_out.close();
		
		goal="alignment";
		approximate=true;
		totalRepetitions=repetitions*deltas.length*alphas.length*epsilons.length;
		repetition=0;
		for(double delta : deltas) {
			for(double alpha : alphas) {
				for(double epsilon: epsilons) {
					//for(double k: ks) {
						//for(int initSize : initSizes) {
							for(int i=0;i<repetitions;i++) {
								XLog copyLog=(XLog) log.clone();
								System.gc();
System.out.println("(ApproxAlignmentReplayer)Repetition "+(++repetition)+" of "+totalRepetitions);
								String toOut=delta+"; "+alpha+"; "+epsilon+"; "+-1+"; "+-1+"; ";
								
								IccParameters iccParameters=new IccParameters(delta, alpha, epsilon, 0, 0, goal, approximate);
								
replayer= new ApproxAlignmentReplayer(iccParameters);

								
								AlignmentReplayResult result=calculateAlignmentWithICC(context,replayer, net,  copyLog, iccParameters, mapping);
								
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
									difference+=Math.abs(d1-d2);
								}
								difference=difference/origDistrib.size();
								
								toOut=toOut+result.toString()+" ;"+difference;
								approxAlignment_out.write(toOut+"\n");
							}
						//}
					//}
				}
			}
		}
		approxAlignment_out.close();
		return "Evaluation complete";
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
			AlignmentReplayResult result=calculateAlignmentWithOrig(context,net, log, mapping);
			toOut=toOut+result.toString();
			out.write(toOut+"\n");
		}
		out.close();
		return "Evaluation complete";
	}
	
	private TransEvClassMapping constructTransEvMapping(UIPluginContext context, XLog log, PetrinetGraph net){
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
}