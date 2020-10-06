package evaluation;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.in.XParser;
import org.deckfour.xes.in.XesXmlParser;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
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
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.graphbased.directed.petrinet.impl.PetrinetFactory;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.connectionfactories.logpetrinet.EvClassLogPetrinetConnectionFactoryUI;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;
import org.processmining.plugins.petrinet.replayer.ui.PNReplayerUI;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.plugins.petrinet.replayresult.StepTypes;
import org.processmining.plugins.pnml.base.Pnml;
import org.processmining.plugins.pnml.importing.PnmlImportUtils;
import org.processmining.plugins.replayer.replayresult.SyncReplayResult;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;

import conformance.IncrementalConformanceChecker;
import conformance.replay.ReplayerFactory;
import conformance.traceAnalysis.IncrementalTraceAnalyzer;
import conformance.traceAnalysis.TraceAnalyzerFactory;
import nl.tue.alignment.Progress;
import nl.tue.alignment.Replayer;
import nl.tue.astar.AStarException;
import qualitychecking.QualityCheckManager;
import resourcedeviations.ResourceAssignment;
import resourcedeviations.ResourceAssignmentComputer;
import ressources.GlobalConformanceResult;
import ressources.IccParameter;

/**
 * @author MartinBauer
 */
public class EvaluationTEST{
	/**
	 * Conduct evaluations of sample-and approximation based conbformance checking. 
	 * The given settings represent the settings used in the evaluation setting of TODO insert paper.
	 * The inputs and used parameters are hardcoded, thus this class is not intended for use as an end user plugin
	 * @param context
	 * @param net
	 * @param log
	 * @return
	 * @throws Exception
	 */
	//7259, 2434, 
	long[] SEEDS = {7259, 2434, 1967, 4180, 4300, 3514, 4086, 8993, 3258, 6415};

	
	
	@Plugin(name = "TEST2 - Evaluate Incremental Conformance Checker", returnLabels = { "Global Conformance Result" }, returnTypes = { GlobalConformanceResult.class }, parameterLabels = {}, userAccessible = true)
	@UITopiaVariant(affiliation = "Humboldt-University Berlin", author = "Martin Bauer", email = "bauemart@hu-berlin.de", uiLabel = UITopiaVariant.USEPLUGIN)
	@PluginVariant(variantLabel = "TEST2 - Evaluate Incremental Conformance Checker", requiredParameterLabels = {0,1})
	public GlobalConformanceResult evaluateICC2(final UIPluginContext context, PetrinetGraph net, XLog log) throws Exception {
		TransEvClassMapping mapping = constructTransEvMapping(context, log, net);
		//get ui-based stuff for all evaluation runs
		//TODO baselines using 1 and 4 cores
		System.out.println("test");
		baselineResultsEvaluation(context, net, log, mapping); //DONE for Synthetic default cores, and Real Life default cores
		//parameterEvaluation(context); //DONE
		//syntheticEvaluation(context); //DONE
		//prefixsuffixEvaluation(context);


		//test(context);
		/*
		resourceDeviationEvaluation(context);
		qualityCheckingEvaluation(context);
		sampleSizeEvaluation(context);
		*/return null;
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


	private void test(UIPluginContext context) throws Exception {
//		String[] inputNames = {"Road_Traffic_Fines_Management_Process"};
		String[] inputNames = {"BPI_Challenge_2012"};
		
		for(String input : inputNames) {
			System.out.println("	>"+input);
			String NET_PATH = "input"+File.separator+input + ".pnml";
			String LOG_PATH = "input"+File.separator+input + ".xes";
			XLog log = loadLog(LOG_PATH);
			PetrinetGraph net = importPNML(NET_PATH, context);
			System.out.println("	>Loaded log and net");
			TransEvClassMapping mapping = computeTransEventMapping(log, net);

			//get resource deviations
			List<IccParameter> settings = new ArrayList<IccParameter>();
			settings.add (new IccParameter(0.01, 0.99, 0.01, 0.2, IncrementalConformanceChecker.Goals.RESOURCES, 	false, false, false));
			
//			settings.add (new IccParameter(0.01, 0.99, 0.01, 0.2, IncrementalConformanceChecker.Goals.FITNESS, 		false, false, false));
//			settings.add (new IccParameter(0.01, 0.99, 0.01, 0.2, IncrementalConformanceChecker.Goals.FITNESS, 		false, false, false));
//			settings.add (new IccParameter(0.01, 0.99, 0.01, 0.2, IncrementalConformanceChecker.Goals.FITNESS, 		false, false, false));
//			
//			settings.add (new IccParameter(0.01, 0.99, 0.01, 0.2, IncrementalConformanceChecker.Goals.FITNESS, 		true,  false, false));
//			settings.add (new IccParameter(0.01, 0.99, 0.01, 0.2, IncrementalConformanceChecker.Goals.FITNESS, 		true,  false, false));
//			settings.add (new IccParameter(0.01, 0.99, 0.01, 0.2, IncrementalConformanceChecker.Goals.FITNESS, 		true,  false, false));
//			
//			settings.add (new IccParameter(0.01, 0.99, 0.01, 0.2, IncrementalConformanceChecker.Goals.DEVIATIONS, 	false, false, false));
//			settings.add (new IccParameter(0.01, 0.99, 0.01, 0.2, IncrementalConformanceChecker.Goals.DEVIATIONS, 	false, false, false));
//			settings.add (new IccParameter(0.01, 0.99, 0.01, 0.2, IncrementalConformanceChecker.Goals.DEVIATIONS, 	false, false, false));
//			
//			//TODO approximation with retaining seem to work worse for logs with a small number of trace variants, as critical events may be discarded
//			settings.add (new IccParameter(0.01, 0.99, 0.01, 0.2, IncrementalConformanceChecker.Goals.DEVIATIONS, 	true, IncrementalConformanceChecker.Heuristics.NONALIGNING_KNOWN, 	false, false));
//			settings.add (new IccParameter(0.01, 0.99, 0.01, 0.2, IncrementalConformanceChecker.Goals.DEVIATIONS, 	true, IncrementalConformanceChecker.Heuristics.NONALIGNING_KNOWN, 	false, false));
//			settings.add (new IccParameter(0.01, 0.99, 0.01, 0.2, IncrementalConformanceChecker.Goals.DEVIATIONS, 	true, IncrementalConformanceChecker.Heuristics.NONALIGNING_KNOWN, 	false, false));
//			
//			settings.add (new IccParameter(0.01, 0.99, 0.01, 0.2, IncrementalConformanceChecker.Goals.DEVIATIONS,   true, IncrementalConformanceChecker.Heuristics.NONALIGNING_ALL, 	false, false));
//			settings.add (new IccParameter(0.01, 0.99, 0.01, 0.2, IncrementalConformanceChecker.Goals.DEVIATIONS, 	true, IncrementalConformanceChecker.Heuristics.NONALIGNING_ALL, 	false, false));
//			settings.add (new IccParameter(0.01, 0.99, 0.01, 0.2, IncrementalConformanceChecker.Goals.DEVIATIONS, 	true, IncrementalConformanceChecker.Heuristics.NONALIGNING_ALL, 	false, false));
//			
//			settings.add (new IccParameter(0.01, 0.99, 0.01, 0.2, IncrementalConformanceChecker.Goals.RESOURCES, 	false, false, false));
//			settings.add (new IccParameter(0.01, 0.99, 0.01, 0.2, IncrementalConformanceChecker.Goals.RESOURCES, 	false, false, false));
//			settings.add (new IccParameter(0.01, 0.99, 0.01, 0.2, IncrementalConformanceChecker.Goals.RESOURCES, 	false, false, false));
//
//			settings.add (new IccParameter(0.01, 0.99, 0.01, 0.2, IncrementalConformanceChecker.Goals.RESOURCES, 	true, IncrementalConformanceChecker.Heuristics.NONALIGNING_KNOWN, 	false, false));
//			settings.add (new IccParameter(0.01, 0.99, 0.01, 0.2, IncrementalConformanceChecker.Goals.RESOURCES, 	true, IncrementalConformanceChecker.Heuristics.NONALIGNING_KNOWN, 	false, false));
//			settings.add (new IccParameter(0.01, 0.99, 0.01, 0.2, IncrementalConformanceChecker.Goals.RESOURCES, 	true, IncrementalConformanceChecker.Heuristics.NONALIGNING_KNOWN, 	false, false));			
//			
//			settings.add (new IccParameter(0.01, 0.99, 0.01, 0.2, IncrementalConformanceChecker.Goals.RESOURCES, 	true, IncrementalConformanceChecker.Heuristics.NONALIGNING_ALL, 	false, false));
//			settings.add (new IccParameter(0.01, 0.99, 0.01, 0.2, IncrementalConformanceChecker.Goals.RESOURCES, 	true, IncrementalConformanceChecker.Heuristics.NONALIGNING_ALL, 	false, false));
//			settings.add (new IccParameter(0.01, 0.99, 0.01, 0.2, IncrementalConformanceChecker.Goals.RESOURCES, 	true, IncrementalConformanceChecker.Heuristics.NONALIGNING_ALL, 	false, false));


			for(IccParameter setting : settings) {
				XLog copyLog = (XLog) log.clone();
				Replayer replayer = ReplayerFactory.createReplayer(net, copyLog, mapping, true);
				ResourceAssignment resAssignment = new ResourceAssignment();
				if (setting.getGoal() == IncrementalConformanceChecker.Goals.RESOURCES) {
					ResourceAssignmentComputer assignmentComputer = new ResourceAssignmentComputer(0.20);
					resAssignment = assignmentComputer.createResourceAssignment(copyLog);
				}
				IncrementalTraceAnalyzer<?> analyzer = TraceAnalyzerFactory.createTraceAnalyzer(setting, replayer, mapping, copyLog, net, resAssignment);
				long start = System.currentTimeMillis();
				System.out.println(setting.toString());
				GlobalConformanceResult result = checkForGlobalConformanceWithICC(context, net, copyLog, analyzer, setting, null, null);
				long end = System.currentTimeMillis();
				System.out.println(result.toString());
				System.out.println(end-start);
				System.out.println();
			}
			

		}

	}

//	/**
//	 * get results of the baseline conformance checking method for fitness, deviations and resource allocation
//	 * @param context
//	 * @throws Exception
//	 */
//	
	private void baselineResultsEvaluation(UIPluginContext context, PetrinetGraph net, XLog log, TransEvClassMapping mapping) throws Exception{

		double fitness=-1.0;
		Map<String,Double> deviationsRel=new HashMap<String,Double>();
		Map<String, Map<String,Double>> resourcesRelative = new HashMap<String, Map<String,Double>>();

		int repetitions=5;
		int cnt=1;
		for(int i=0;i<repetitions;i++) {
			System.out.println("Baseline Evaluation>BPI2014 - repetition "+cnt+"/"+repetitions);
			
			XLog copyLog = (XLog) log.clone();
			copyLog = (XLog) log.clone();
			Replayer replayer = ReplayerFactory.createReplayer(net, copyLog, mapping, false);
			
			long start = System.currentTimeMillis();
			PNRepResult result = replayer.computePNRepResult(Progress.INVISIBLE, copyLog);
			long end = System.currentTimeMillis();
			System.out.println("replay time :"+(end-start));
			

			//fitness
			fitness = (Double) result.getInfo().get("Trace Fitness");
			System.out.println("Fitness: "+fitness);
			
			//deviations
			Multiset<String> deviatingActivities = HashMultiset.create();
			for(SyncReplayResult replayResult : result) {
				for (int j=0;j<replayResult.getStepTypes().size();j++) {
					if(replayResult.getStepTypes().get(j).toString().equals("Log move") || replayResult.getStepTypes().get(j).toString().equals("Model move")) {
						//System.out.println(replayResult.getNodeInstance().get(j).toString());
						if(replayResult.getStepTypes().get(j).toString().equals("Model move")) {
								if(mapping.containsKey(replayResult.getNodeInstance().get(j))) {
								deviatingActivities.add(mapping.get(replayResult.getNodeInstance().get(j)).toString());
							}
							else {
								deviatingActivities.add((replayResult.getNodeInstance().get(j)).toString());
							}
						}
						if(replayResult.getStepTypes().get(j).toString().equals("Log move")) {
							deviatingActivities.add((replayResult.getNodeInstance().get(j)).toString());
						}
					}
				}
			}
			
			Map<String, Integer> asynchMoveAbs=new HashMap<String, Integer>();
			Map<String, Double> asynchMoveRel=new HashMap<String, Double>();
			for (String key : deviatingActivities.elementSet()) {
				int absValue=deviatingActivities.count(key);
				double relValue=(double)absValue/(double)deviatingActivities.size();
				asynchMoveAbs.put(key, absValue);
				asynchMoveRel.put(key, relValue);
			}

			
			double total =0;
			for (String x : deviatingActivities.elementSet()) {
				total=total+deviatingActivities.count(x);
			}
			double test = total;
			//deviationsAbs=Maps.asMap(deviatingActivities.elementSet(), elem -> deviatingActivities.count(elem));
			deviationsRel=Maps.asMap(deviatingActivities.elementSet(), elem -> deviatingActivities.count(elem)/test);
			
			System.out.println("Deviations (New): "+deviationsRel);
			System.out.println("Deviations (Old): "+asynchMoveRel);
			
			//resources
			Map<String, Map<String,Double>> resources = new HashMap<String, Map<String,Double>>();
			
			ResourceAssignment resAssignment = new ResourceAssignment();
			ResourceAssignmentComputer assignmentComputer = new ResourceAssignmentComputer(0.20);
			resAssignment = assignmentComputer.createResourceAssignment(copyLog);
			
			for (SyncReplayResult replayResult : result) {
				XTrace trace = copyLog.get(replayResult.getTraceIndex().first());
				List<StepTypes> stepTypes = replayResult.getStepTypes();
				Map<String, Map<String,Double>> result1 = getResourcesFromSkipSteps(trace, stepTypes);
				Map<String, Map<String,Double>> result2 = getUnauthorizedResources(trace, resAssignment);
				for (String activity : result2.keySet()) {
					if (resources.containsKey(activity)) {
						for(java.util.Map.Entry<String, Double> resource : result2.get(activity).entrySet()) {
							if(resources.get(activity).containsKey(resource.getKey())) {
								resources.get(activity).put(resource.getKey(), resources.get(activity).get(resource.getKey())+resource.getValue());
							}
							else
								resources.get(activity).put(resource.getKey(), resource.getValue());
						}
					} else {
						resources.put(activity, result2.get(activity));
					}
				}
				for (String activity : result1.keySet()) {
					if(resources.containsKey(activity)) {
						for (java.util.Map.Entry<String, Double> resource : result1.get(activity).entrySet()) {
							if(resources.get(activity).containsKey(resource.getKey())) {
								resources.get(activity).put(resource.getKey(), resources.get(activity).get(resource.getKey())+resource.getValue());
							}
							else {
								resources.get(activity).put(resource.getKey(),resource.getValue());
							}
						}
					}
					else {
						resources.put(activity, result1.get(activity));
					}
				}
			}
			for(String activity : resources.keySet()) {
				resourcesRelative.put(activity, new HashMap<String, Double>());
				double cnts= resources.get(activity).values().stream().mapToDouble(x->x).sum();
				for(java.util.Map.Entry<String, Double> resource : resources.get(activity).entrySet()) {
					resourcesRelative.get(activity).put(resource.getKey(), resource.getValue()/cnts);
				}
				
			}
			System.out.println("Resources: "+resourcesRelative);
			System.out.println("");
			
			//baselineStats.write(String.join(";",Long.toString(end-start),Integer.toString(log.size()), Double.toString(fitness), deviationsRel.toString(), resourcesRelative.toString()+"\n"));
			cnt++;
		}
		//baselineStats.close();
	}
	
	
	
	protected Map<String, Map<String,Double>> getResourcesFromSkipSteps(XTrace trace, List<StepTypes> stepTypes) {
		Map<String, Map<String,Double>> result = new HashMap<>();
		for(StepTypes step : stepTypes) {
			int eventIndex = 0;
			if(step == StepTypes.L) {
				XEvent event = trace.get(eventIndex);
				if (event.getAttributes().get("org:resource") != null) {
					String resource = event.getAttributes().get("org:resource").toString();
					String activity = event.getAttributes().get("concept:name").toString();
					addViolatingResource(result, activity, resource);
				}
			}
			if(step == StepTypes.L  || step == StepTypes.LMGOOD) {
				eventIndex++;
			}
		}
		return result;
	}
	
	protected Map<String, Map<String, Double>> getUnauthorizedResources(XTrace trace, ResourceAssignment resAssignment) {
		Map<String, Map<String, Double>> result = new HashMap<>();
		for (XEvent event : trace) {
			String activity = event.getAttributes().get("concept:name").toString();
			if (event.getAttributes().get("org:resource") != null) {
				String resource = event.getAttributes().get("org:resource").toString();
				if (!resAssignment.isAuthorized(activity, resource)) {
					addViolatingResource(result, activity, resource);
				}
			}
		}
		return result;
	}
	
	protected void addViolatingResource(Map<String, Map<String, Double>> violationMap, String activity, String resource) {
		Map<String, Double> activityViolations;
		activityViolations = violationMap.getOrDefault(activity, new HashMap<String, Double>());
		if(activityViolations.containsKey(resource))
			activityViolations.put(resource, activityViolations.get(resource)+1);
		else
			activityViolations.put(resource, 1.0);
		violationMap.put(activity, activityViolations);
	}

	/**
	 * get the performance information for fitness and deviations conformance checking cases for multiple different parameter settings
	 * @param context
	 * @throws Exception
	 */
	private void parameterEvaluation(final UIPluginContext context) throws Exception{
		int repetitions =5;

		String[] inputNames = {"Road_Traffic_Fines_Management_Process", "BPI_Challenge_2012", "Detail_Incident_Activity"};
		System.out.println("Evaluating parameters");
		for(String input : inputNames) {
			System.out.println(">Loading "+input);
			String NET_PATH = "input/" + input + ".pnml";
			String LOG_PATH = "input/" + input + ".xes";
			XLog log = loadLog(LOG_PATH);
			PetrinetGraph net = importPNML(NET_PATH, context);
			System.out.println(">Done");
			TransEvClassMapping mapping = computeTransEventMapping(log, net);				
			
			PrintWriter fitness 		= new PrintWriter(input+"_fitness.csv", "UTF-8");
			PrintWriter fitnessApprox 	= new PrintWriter(input+"_fitnessApprox.csv", "UTF-8");
			PrintWriter deviation 		= new PrintWriter(input+"_deviations.csv", "UTF-8");
			PrintWriter deviationApprox = new PrintWriter(input+"_deviationsApprox.csv", "UTF-8");
			fitness			.write(String.join(";","delta","alpha","epsilon","k","time","logSize","fitness","deviations","approximationMode","resources\n"));
			fitnessApprox	.write(String.join(";","delta","alpha","epsilon","k","time","logSize","fitness","deviations","approximationMode","resources\n"));
			deviation		.write(String.join(";","delta","alpha","epsilon","k","time","logSize","fitness","deviations","approximationMode","resources\n"));
			deviationApprox	.write(String.join(";","delta","alpha","epsilon","k","time","logSize","fitness","deviations","approximationMode","resources\n"));
			//Delta = 0.01, 0.05, 0.1
			//Alpha = 0.99, 0.95, 0.9
			//Epsilon = 0.01, 0.05, 0.1
			//K = 0.1, 0.2, 0.3
			ArrayList<IccParameter> list = new ArrayList<IccParameter>();
			//fitness
			list.add(new IccParameter(0.01, 0.99, 0.01, 0.2 , IncrementalConformanceChecker.Goals.FITNESS, false, false, false));
			list.add(new IccParameter(0.05, 0.99, 0.01, 0.2 ,IncrementalConformanceChecker.Goals.FITNESS, false, false, false));
			list.add(new IccParameter(0.1, 0.99, 0.01, 0.2 , IncrementalConformanceChecker.Goals.FITNESS, false, false, false));
			list.add(new IccParameter(0.01, 0.9, 0.01, 0.2 ,IncrementalConformanceChecker.Goals.FITNESS, false, false, false));
			list.add(new IccParameter(0.01, 0.95, 0.01, 0.2 , IncrementalConformanceChecker.Goals.FITNESS, false, false, false));
			list.add(new IccParameter(0.01, 0.99, 0.05, 0.2 , IncrementalConformanceChecker.Goals.FITNESS, false, false, false));
			list.add(new IccParameter(0.01, 0.99, 0.1, 0.2 , IncrementalConformanceChecker.Goals.FITNESS, false, false, false));
			//fitness approximated
			list.add(new IccParameter(0.01, 0.99, 0.01, 0.2 , IncrementalConformanceChecker.Goals.FITNESS, true, false, false));
			list.add(new IccParameter(0.05, 0.99, 0.01, 0.2 , IncrementalConformanceChecker.Goals.FITNESS, true, false, false));
			list.add(new IccParameter(0.1, 0.99, 0.01, 0.2 , IncrementalConformanceChecker.Goals.FITNESS, true, false, false));
			list.add(new IccParameter(0.01, 0.9, 0.01, 0.2 ,IncrementalConformanceChecker.Goals.FITNESS, true, false, false));
			list.add(new IccParameter(0.01, 0.95, 0.01, 0.2 , IncrementalConformanceChecker.Goals.FITNESS, true, false, false));
			list.add(new IccParameter(0.01, 0.99, 0.05, 0.2 , IncrementalConformanceChecker.Goals.FITNESS, true, false, false));
			list.add(new IccParameter(0.01, 0.99, 0.1, 0.2 , IncrementalConformanceChecker.Goals.FITNESS, true, false, false));
			list.add(new IccParameter(0.01, 0.99, 0.01, 0.1 , IncrementalConformanceChecker.Goals.FITNESS, true, false, false));
			list.add(new IccParameter(0.01, 0.99, 0.01, 0.3 , IncrementalConformanceChecker.Goals.FITNESS, true, false, false));
			//deviations
			list.add(new IccParameter(0.01, 0.99, 0.01, 0.2 , IncrementalConformanceChecker.Goals.DEVIATIONS, false, false, false));
			list.add(new IccParameter(0.05, 0.99, 0.01, 0.2 , IncrementalConformanceChecker.Goals.DEVIATIONS, false, false, false));
			list.add(new IccParameter(0.1, 0.99, 0.01, 0.2 , IncrementalConformanceChecker.Goals.DEVIATIONS, false, false, false));
			list.add(new IccParameter(0.01, 0.9, 0.01, 0.2 ,IncrementalConformanceChecker.Goals.DEVIATIONS, false, false, false));
			list.add(new IccParameter(0.01, 0.95, 0.01, 0.2 , IncrementalConformanceChecker.Goals.DEVIATIONS, false, false, false));
			list.add(new IccParameter(0.01, 0.99, 0.05, 0.2 , IncrementalConformanceChecker.Goals.DEVIATIONS, false, false, false));
			list.add(new IccParameter(0.01, 0.99, 0.1, 0.2 , IncrementalConformanceChecker.Goals.DEVIATIONS, false, false, false));
			//deviations approximated - all deviations
			list.add(new IccParameter(0.01, 0.99, 0.01, 0.2 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_ALL, false, false));
			list.add(new IccParameter(0.05, 0.99, 0.01, 0.2 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_ALL, false, false));
			list.add(new IccParameter(0.1, 0.99, 0.01, 0.2 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_ALL, false, false));
			list.add(new IccParameter(0.01, 0.9, 0.01, 0.2 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_ALL, false, false));
			list.add(new IccParameter(0.01, 0.95, 0.01, 0.2 ,  IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_ALL, false, false));
			list.add(new IccParameter(0.01, 0.99, 0.05, 0.2 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_ALL, false, false));
			list.add(new IccParameter(0.01, 0.99, 0.1, 0.2 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_ALL, false, false));
			list.add(new IccParameter(0.01, 0.99, 0.01, 0.1 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_ALL, false, false));
			list.add(new IccParameter(0.01, 0.99, 0.01, 0.3 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_ALL, false, false));
			//deviations approximated - known deviations
			list.add(new IccParameter(0.01, 0.99, 0.01, 0.2 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_KNOWN, false, false));
			list.add(new IccParameter(0.05, 0.99, 0.01, 0.2 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_KNOWN, false, false));
			list.add(new IccParameter(0.1, 0.99, 0.01, 0.2 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_KNOWN, false, false));
			list.add(new IccParameter(0.01, 0.9, 0.01, 0.2 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_KNOWN, false, false));
			list.add(new IccParameter(0.01, 0.95, 0.01, 0.2 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_KNOWN, false, false));
			list.add(new IccParameter(0.01, 0.99, 0.05, 0.2 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_KNOWN, false, false));
			list.add(new IccParameter(0.01, 0.99, 0.1, 0.2 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_KNOWN, false, false));
			list.add(new IccParameter(0.01, 0.99, 0.01, 0.1 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_KNOWN, false, false));
			list.add(new IccParameter(0.01, 0.99, 0.01, 0.3 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_KNOWN, false, false));
			
			int total=list.size()*repetitions;
			int cnt=1;
			for(IccParameter parameter : list) {
				for(int i=0;i<repetitions;i++) {
					System.out.println("Parameter Evaluation>"+input+" - repetition "+cnt+"/"+total);
					
					XLog copyLog=(XLog) log.clone();
					Replayer replayer = ReplayerFactory.createReplayer(net, copyLog, mapping, true);
					IncrementalTraceAnalyzer<?> analyzer = TraceAnalyzerFactory.createTraceAnalyzer(parameter, replayer, mapping, log, net, null);
					long start = System.currentTimeMillis();
					GlobalConformanceResult result = checkForGlobalConformanceWithICC(context, net, copyLog, analyzer, parameter, null, null, SEEDS[i]);
					long end = System.currentTimeMillis();
					String out=String.join(";", 
							Double.toString(parameter.getDelta()),Double.toString(parameter.getAlpha()),Double.toString(parameter.getEpsilon()),Double.toString(parameter.getK()),
							Long.toString(end-start), Integer.toString(result.getCnt()), 
							Double.toString(result.getFitness()), result.getDeviations().toString(), parameter.getApproximationHeuristic().toString(), result.getResourceDeviations().toString()+"\n");

					if(parameter.getGoal().equals(IncrementalConformanceChecker.Goals.FITNESS) && !parameter.isApproximate())
						fitness.write(out);
					if(parameter.getGoal().equals(IncrementalConformanceChecker.Goals.FITNESS) && parameter.isApproximate())
						fitnessApprox.write(out);
					if(parameter.getGoal().equals(IncrementalConformanceChecker.Goals.DEVIATIONS) && !parameter.isApproximate())
						deviation.write(out);
					if(parameter.getGoal().equals(IncrementalConformanceChecker.Goals.DEVIATIONS) && parameter.isApproximate())
						deviationApprox.write(out);
					
					cnt++;
				}
			}
			fitness.close();
			fitnessApprox.close();
			deviation.close();
			deviationApprox.close();
		}
	}
//
//	
	/**
	 * evaluate the approximation-based deviating activity use case using the prefixsuffix heuristic.
	 * This heuristic needs more granular fine tuning than the others, hence it's own evaluation run
	 * @param context
	 * @throws Exception
	 */
	private void prefixsuffixEvaluation(UIPluginContext context) throws Exception{
		String[] inputNames = {"BPI_Challenge_2012"};
		System.out.println("Evaluating prefix-suffix based deviations");
		int repetitions =5;
		for(String input : inputNames) {
			System.out.println("	>"+input);
			String NET_PATH = "input/" + input + ".pnml";
			String LOG_PATH = "input/" + input + ".xes";
			XLog log = loadLog(LOG_PATH);
			PetrinetGraph net = importPNML(NET_PATH, context);
			System.out.println("	>Loaded log and net");
			TransEvClassMapping mapping = computeTransEventMapping(log, net);				
			
			PrintWriter prefixsuffix = new PrintWriter(input+"_prefixsuffix", "UTF-8");
			prefixsuffix.write(String.join(";","time","logSize","fitness","asynchMovesRel","deviatingResourcesRel\n"));
				
			ArrayList<IccParameter> list = new ArrayList<IccParameter>();
			//deviations approximated - prefix suffix
			list.add(new IccParameter(0.01, 0.99, 0.01, 0.1 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_KNOWN, false, false));
			list.add(new IccParameter(0.01, 0.99, 0.01, 0.05 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_KNOWN, false, false));
			list.add(new IccParameter(0.01, 0.99, 0.01, 0.01 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_KNOWN, false, false));
			
			for(IccParameter parameter : list) {
				int cnt=1;
				int total=list.size()*repetitions;
				for(int i=0;i<repetitions;i++) {
					System.out.println("prefixSuffix Evaluation>"+input+" - repetition "+cnt+"/"+total);
					XLog copyLog=(XLog) log.clone();
					Replayer replayer = ReplayerFactory.createReplayer(net, copyLog, mapping, true);
					IncrementalTraceAnalyzer<?> analyzer = TraceAnalyzerFactory.createTraceAnalyzer(parameter, replayer, mapping, log, net, null);
					long start = System.currentTimeMillis();
					GlobalConformanceResult result = checkForGlobalConformanceWithICC(context, net, copyLog, analyzer, parameter, null, null);
					long end = System.currentTimeMillis();
					String out=String.join(";", 
							Double.toString(parameter.getDelta()),Double.toString(parameter.getAlpha()),Double.toString(parameter.getEpsilon()),Double.toString(parameter.getK()),
							Long.toString(end-start), Integer.toString(result.getCnt()), 
							Double.toString(result.getFitness()), result.getDeviations().toString(), parameter.getApproximationHeuristic().toString(), result.getResourceDeviations().toString()+"\n");
					prefixsuffix.write(out);
					cnt++;
				}
			}
			prefixsuffix.close();

		}
	}
//
//	/**
//	 * get results for different quality checking scenarios
//	 * @param context
//	 * @throws Exception
//	 */
//	private void qualityCheckingEvaluation(UIPluginContext context) throws Exception{
//		String[] inputNames = {"BPI_Challenge_2012"};
//		System.out.println("Evaluating Quality Checking");
//		int repetitions =5;
//		PrintWriter quality = new PrintWriter("quality", "UTF-8");
//		quality.write("log; delta; alpha; epsilon; quality checker; dfDistrib; attrib; times triggered; fitness; time; distToOriginal; original\n");
//
//		for(String input : inputNames) {
//			System.out.println("	>"+input);
//			String NET_PATH = "input/" + input + ".pnml";
//			String LOG_PATH = "input/" + input + ".xes";
//			XLog log = loadLog(LOG_PATH);
//			PetrinetGraph net = importPNML(NET_PATH, context);
//			System.out.println("	>Loaded log and net");
//			TransEvClassMapping mapping = computeTransEventMapping(log, net);				
//			
//			double origFitness = 0;
//			if (input.equals("bpi14")) {
//				origFitness = 0.9008334329271833; 
//			}
//			if (input.equals("traffic")) {
//				origFitness = 0.9823429057173879;
//			}
//			if (input.equals("bpi12")) {
//				origFitness = 0.7310599598129743;
//			}
//
//			
//			ArrayList<IccParameter> list = new ArrayList<IccParameter>();
//			ArrayList<QualityCheckManager> qualityList = new ArrayList<QualityCheckManager>();
//			IccParameter setting;
//			//internal checks
//			//without quality checking
//			setting =  new IccParameter(0.01, 0.99, 0.01, 0.2, IncrementalConformanceChecker.Goals.FITNESS, false, true, false);
//			list.add(setting);
//			qualityList.add(new QualityCheckManager(true));
//			//with directly follows
//			setting =  new IccParameter(0.01, 0.99, 0.01, 0.2, IncrementalConformanceChecker.Goals.FITNESS, false, true, false);
//			QualityCheckManager dfInternal = new QualityCheckManager(true);
//			dfInternal.addDirectlyFollowsChecking();
//			list.add(setting);
//			qualityList.add(dfInternal);
//			//with data attribute
//			setting =  new IccParameter(0.01, 0.99, 0.01, 0.2, IncrementalConformanceChecker.Goals.FITNESS, false, true, false);
//			QualityCheckManager attrInternal = new QualityCheckManager(true);
//			addDataAttribute(attrInternal, input);
//			list.add(setting);
//			qualityList.add(attrInternal);
//			//with directly follows and data attribute
//			setting =  new IccParameter(0.01, 0.99, 0.01, 0.2, IncrementalConformanceChecker.Goals.FITNESS, false, true, false);
//			QualityCheckManager dfAttrInternal = new QualityCheckManager(true);
//			dfAttrInternal.addDirectlyFollowsChecking();				
//			addDataAttribute(dfAttrInternal, input);
//			list.add(setting);
//			qualityList.add(dfAttrInternal);
//
//			for(int i=0;i<list.size();i++) {
//				for(int j=0;j<repetitions;j++) {
//					XLog copyLog=(XLog) log.clone();
//					IccParameter current = list.get(i);
//					QualityCheckManager currentInternal = qualityList.get(i);
//					
//					Replayer replayer = ReplayerFactory.createReplayer(net, copyLog, mapping, true);
//					IncrementalTraceAnalyzer analyzer = AnalyzerFactory.createTraceAnalyzer(current, replayer, mapping, copyLog, net, null);
//					TraceAnalysisResultMap result = checkForGlobalConformanceWithICC(context, net, copyLog, analyzer, current, null, null);
//					
//					String toOut = input + ";0.01; 0.99; 0.01; internal; "+currentInternal.checkDirectlyFollows()+"; "+ currentInternal.getCheckedAttributes()+ currentInternal.timesTriggered()+";";
//
//					double difference=Math.abs(origFitness-result.getFitness());
//					toOut=toOut+result.toString()+"; -;"+difference + ";" + origFitness+"\n";
//					quality.write(toOut);
//					currentInternal.resetAll();
//				}
//			}
//			
//			//external
//			list=new ArrayList<IccParameter>();
//			qualityList=new ArrayList<QualityCheckManager>();
//			//without quality checking
//			setting =  new IccParameter(0.01, 0.99, 0.01, 0.2, IncrementalConformanceChecker.Goals.FITNESS, false, false, true);
//			list.add(setting);
//			qualityList.add(new QualityCheckManager(false));
//			//with directly follows
//			setting =  new IccParameter(0.01, 0.99, 0.01, 0.2, IncrementalConformanceChecker.Goals.FITNESS, false, false, true);
//			QualityCheckManager dfExternal = new QualityCheckManager(false);
//			dfInternal.addDirectlyFollowsChecking();
//			list.add(setting);
//			qualityList.add(dfExternal);
//			//with data attribute
//			setting =  new IccParameter(0.01, 0.99, 0.01, 0.2, IncrementalConformanceChecker.Goals.FITNESS, false, false, true);
//			QualityCheckManager attrExternal = new QualityCheckManager(false);
//			addDataAttribute(attrExternal, input);
//			list.add(setting);
//			qualityList.add(attrExternal);
//			//with directly follows and data attribute
//			setting =  new IccParameter(0.01, 0.99, 0.01, 0.2, IncrementalConformanceChecker.Goals.FITNESS, false, false, true);
//			QualityCheckManager dfAttrExternal = new QualityCheckManager(false);
//			dfAttrExternal.addDirectlyFollowsChecking();				
//			addDataAttribute(dfAttrExternal, input);
//			list.add(setting);
//			qualityList.add(dfAttrExternal);
//			
//			for(int i=0;i<list.size();i++) {
//				for(int j=0;j<repetitions;j++) {
//					XLog copyLog=(XLog) log.clone();
//					IccParameter current = list.get(i);
//					QualityCheckManager currentExternal = qualityList.get(i);
//					
//					Replayer replayer = ReplayerFactory.createReplayer(net, copyLog, mapping, true);
//					IncrementalTraceAnalyzer analyzer = AnalyzerFactory.createTraceAnalyzer(current, replayer, mapping, copyLog, net, null);
//					TraceAnalysisResultMap result = checkForGlobalConformanceWithICC(context, net, copyLog, analyzer, current, null, null);
//					
//					String toOut = input + ";0.01; 0.99; 0.01; external; "+currentExternal.checkDirectlyFollows()+"; "+ currentExternal.getCheckedAttributes()+ currentExternal.timesTriggered()+";";
//
//					double difference=Math.abs(origFitness-result.getFitness());
//					toOut=toOut+result.toString()+"; -;"+difference + ";" + origFitness+"\n";
//					quality.write(toOut);
//					currentExternal.resetAll();
//				}
//			}
//		}
//		quality.close();
//	}
//	
//	/**
//	 * set the checked data attribute depending on the given log
//	 * @param manager
//	 * @param logname
//	 */
//	private void addDataAttribute(QualityCheckManager manager, String logname) {
//		if (logname.equals("Road_Traffic_Fines_Management_Process")) {
//			manager.addNumericEventAttribute("Create Fine", "amount");
//		}
//		if (logname.equals("BPI_Challenge_2012")) {
//			manager.addNominalEventAttribute("W_Completeren aanvraag", "org:resource");
//		}
//		if (logname.equals("Detail_Incident_Activity")) {
//			manager.addNominalEventAttribute("Closed", "org:resource");
//		}
//	}
//
	/**
	 * get runtime and fitness results for the synthetic datasets, as used in TODO add paper
	 * @param context
	 * @throws Exception
	 */
	private void syntheticEvaluation(UIPluginContext context)throws Exception {
		String[] inputNames = {"prAm6","prBm6","prCm6","prDm6","prEm6","prFm6","prGm6"};
		System.out.println("Evaluating synthetic log model pairs");
		int repetitions =5;

		for(String input : inputNames) {
			PrintWriter synthetic = new PrintWriter(input+"_synthetic.csv", "UTF-8");
			synthetic.write(String.join(";","time","logSize","fitness","asynchMovesRel","deviatingResourcesRel\n"));

			String NET_PATH = "input/BPM2013benchmarks/" + input + ".pnml";
			String LOG_PATH = "input/BPM2013benchmarks/" + input + ".xes";
			XLog log = loadLog(LOG_PATH);
			PetrinetGraph net = importPNML(NET_PATH, context);
			System.out.println("	>Loaded log and net");
			TransEvClassMapping mapping = computeTransEventMapping(log, net);
			
			int cnt=1;
			int total=repetitions;
			for(int i=0;i<repetitions;i++) {
				System.out.println("Synthetic Evaluation>"+input+" - repetition "+cnt+"/"+total);

				IccParameter parameter = new IccParameter(0.05, 0.99, 0.01, 0.2 , IncrementalConformanceChecker.Goals.FITNESS, false, false, false);
				XLog copyLog=(XLog) log.clone();
				Replayer replayer = ReplayerFactory.createReplayer(net, copyLog, mapping, true);
				IncrementalTraceAnalyzer<?> analyzer = TraceAnalyzerFactory.createTraceAnalyzer(parameter, replayer, mapping, log, net, null);
				long start = System.currentTimeMillis();
				GlobalConformanceResult result = checkForGlobalConformanceWithICC(context, net, copyLog, analyzer, parameter, null, null, SEEDS[i]);
				long end = System.currentTimeMillis();
				String out=String.join(";", 
						Double.toString(parameter.getDelta()),Double.toString(parameter.getAlpha()),Double.toString(parameter.getEpsilon()),Double.toString(parameter.getK()),
						Long.toString(end-start), Integer.toString(result.getCnt()), 
						Double.toString(result.getFitness()), result.getDeviations().toString(), parameter.getApproximationHeuristic().toString(), result.getResourceDeviations().toString()+"\n");
				synthetic.write(out);
				cnt++;
			}
			synthetic.close();
		}
	}
//
//	/**
//	 * get results for the resource deviation use case
//	 * @param context
//	 * @throws Exception
//	 */
//	private void resourceDeviationEvaluation(UIPluginContext context) throws Exception {
//		String[] inputNames = {"BPI_Challenge_2012"};
//		System.out.println("Evaluating resource deviations");
//		int repetitions =5;
//
//		for(String input : inputNames) {
//			System.out.println("	>"+input);
//
//			String NET_PATH = "input/" + input + ".pnml";
//			String LOG_PATH = "input/" + input + ".xes";
//			XLog log = loadLog(LOG_PATH);
//			PetrinetGraph net = importPNML(NET_PATH, context);
//			System.out.println("	>Loaded log and net");
//			TransEvClassMapping mapping = computeTransEventMapping(log, net);				
//			
//			PrintWriter resources = new PrintWriter(input+"_resources", "UTF-8");
//			resources.write("delta; alpha; epsilon; k; fitness; time; logSize; asynchMovesAbs; asynchMovesRel; approximationMode; resourcesAbs; resourcesRel\n");
//			
//			ArrayList<IccParameter> list = new ArrayList<IccParameter>();
//			//deviations approximated - prefix suffix
//			list.add(new IccParameter(0.01, 0.99, 0.01, 0.1 , IncrementalConformanceChecker.Goals.FITNESS, false, IncrementalConformanceChecker.Heuristics.NONALIGNING_KNOWN, false, false));
//			for(IccParameter parameter : list) {
//				for(int i=0;i<repetitions;i++) {
//					XLog copyLog=(XLog) log.clone();
//					Replayer replayer = ReplayerFactory.createReplayer(net, copyLog, mapping, true);
//					IncrementalTraceAnalyzer analyzer = AnalyzerFactory.createTraceAnalyzer(parameter, replayer, mapping, log, net, null);
//					long start = System.currentTimeMillis();
//					TraceAnalysisResultMap result = checkForGlobalConformanceWithICC(context, net, copyLog, analyzer, parameter, null, null);
//					long end = System.currentTimeMillis();
//					String out="";
//					out=out+parameter.getDelta()+"; "+
//							parameter.getAlpha()+"; "+
//							parameter.getEpsilon()+"; "+
//							parameter.getK()+"; "+
//							result.getFitness()+"; "+
//							(end-start)+"; "+
//							result.getSampledTraceCnt()+"; "+
//							result.getDeviationsAbsolute()+"; "+
//							result.getDeviations()+"; "+
//							parameter.getApproximationHeuristic()+"\n";
//					resources.write(out);
//				}
//			}
//			resources.close();
//		}
//	}
//	
//
//	/**
//	 * get different sample sizes for the different possible binomial confidence intervals
//	 * @param context
//	 */
//	private void sampleSizeEvaluation(UIPluginContext context) {
//		// TODO print stats for different sample size methods
//		
//	}
//
//
	/**
	 * call the incremental conformance checking algorihtm using the specified parameter
	 * @param context
	 * @param net
	 * @param log
	 * @param analyzer
	 * @param iccParameters
	 * @param internalQualityCheckManager
	 * @param externalQualityCheckManager
	 * @return
	 * @throws AStarException
	 */
	private GlobalConformanceResult checkForGlobalConformanceWithICC(UIPluginContext context, PetrinetGraph net, XLog log,
			IncrementalTraceAnalyzer<?> analyzer, IccParameter iccParameters, QualityCheckManager internalQualityCheckManager, QualityCheckManager externalQualityCheckManager) throws AStarException {
		IncrementalConformanceChecker icc = new IncrementalConformanceChecker(analyzer, iccParameters);
		return icc.apply(context, log, net);
	}
	
	private GlobalConformanceResult checkForGlobalConformanceWithICC(UIPluginContext context, PetrinetGraph net, XLog log,
			IncrementalTraceAnalyzer<?> analyzer, IccParameter iccParameters, QualityCheckManager internalQualityCheckManager, QualityCheckManager externalQualityCheckManager, long seed) throws AStarException {
		IncrementalConformanceChecker icc = new IncrementalConformanceChecker(analyzer, iccParameters, seed);
		return icc.apply(context, log, net);
	}
		
	/**
	 * load a log and convert is to XLog
	 * @param path - path to log, relative to cwd
	 * @return
	 */
	public static XLog loadLog(String path) {
		File file = new File(path);
		XFactory  factory = XFactoryRegistry.instance().currentDefault();
		XParser parser = new XesXmlParser(factory);
		//XMxmlParser parser = new XMxmlParser();
		
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
	

	/**
	 * convert the imported pnml objects to petrinet
	 * @param fileName - path to file, relative to cwd
	 * @param context
	 * @return
	 * @throws Exception
	 */
	public static Petrinet importPNML(String fileName, PluginContext context) throws Exception {
		return (Petrinet) (importPNMLObjects(fileName, context))[0];
	}
	
	
	/**
	 * import a pnml file and convert it to a list of pnmlobjects
	 * @param fileName
	 * @param context
	 * @return
	 * @throws Exception
	 */
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
	
	/**
	 * construct an ectivity-event mapping for used logs and model
	 * @param log
	 * @param net
	 * @return
	 */
	public static  TransEvClassMapping computeTransEventMapping(XLog log, PetrinetGraph net) {
		XEventClass evClassDummy = EvClassLogPetrinetConnectionFactoryUI.DUMMY;
		TransEvClassMapping mapping = new TransEvClassMapping(XLogInfoImpl.STANDARD_CLASSIFIER, evClassDummy);
		XEventClasses ecLog = XLogInfoFactory.createLogInfo(log, XLogInfoImpl.STANDARD_CLASSIFIER).getEventClasses();
		for (Transition t : net.getTransitions()) {
			//TODO: this part is rather hacky, I'll admit.
			//TODO: Martin: this is only problematic for BPI2012 anyway, we'll fix this to be based on equality
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