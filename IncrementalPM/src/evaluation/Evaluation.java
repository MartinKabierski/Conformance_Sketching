package evaluation;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

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
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.plugins.petrinet.replayresult.StepTypes;
import org.processmining.plugins.pnml.base.Pnml;
import org.processmining.plugins.pnml.importing.PnmlImportUtils;
import org.processmining.plugins.replayer.replayresult.SyncReplayResult;

import conformance.IncrementalConformanceChecker;
import conformance.replay.ReplayerFactory;
import conformance.sampling.ThresholdCalculator;
import conformance.traceAnalysis.IncrementalTraceAnalyzer;
import conformance.traceAnalysis.TraceAnalyzerFactory;
import nl.tue.alignment.Progress;
import nl.tue.alignment.Replayer;
import nl.tue.astar.AStarException;
import qualitychecking.AbstractValueDistribution;
import qualitychecking.QualityCheckManager;
import resourcedeviations.ResourceAssignment;
import resourcedeviations.ResourceAssignmentComputer;
import ressources.GlobalConformanceResult;
import ressources.IccParameter;

/**
 * @author MartinBauer
 */
public class Evaluation{
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

	
	
	@Plugin(name = "TEST - Evaluate Incremental Conformance Checker", returnLabels = { "Global Conformance Result" }, returnTypes = { GlobalConformanceResult.class }, parameterLabels = {}, userAccessible = true)
	@UITopiaVariant(affiliation = "Humboldt-University Berlin", author = "Martin Bauer", email = "bauemart@hu-berlin.de", uiLabel = UITopiaVariant.USEPLUGIN)
	@PluginVariant(variantLabel = "TEST - Evaluate Incremental Conformance Checker", requiredParameterLabels = {})
	public GlobalConformanceResult evalueICC(final UIPluginContext context) throws Exception {
		//parameterEvaluation(context);
		//prefixsuffixEvaluation(context);
		//syntheticEvaluation(context);
		//resourceDeviationEvaluation(context);
		//qualityCheckingEvaluation(context);
		//resourceDeviationEvaluation(context);
		//baselineResultsEvaluation(context);
		
		test(context);
		
		//sampleSizeEvaluation(context);
		return null;
	}
	

	private void test(UIPluginContext context) throws Exception {
		//String[] inputNames = {"Road_Traffic_Fines_Management_Process", "RTFM_model2"};
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
			//settings.add (new IccParameter(0.01, 0.99, 0.01, 0.2, IncrementalConformanceChecker.Goals.RESOURCES, 	false, false, false));
			
//			settings.add (new IccParameter(0.01, 0.99, 0.01, 0.2, IncrementalConformanceChecker.Goals.FITNESS, 		false, false, false));
//			settings.add (new IccParameter(0.01, 0.99, 0.01, 0.2, IncrementalConformanceChecker.Goals.FITNESS, 		false, false, false));
//			settings.add (new IccParameter(0.01, 0.99, 0.01, 0.2, IncrementalConformanceChecker.Goals.FITNESS, 		false, false, false));
//			
//			settings.add (new IccParameter(0.01, 0.99, 0.01, 0.2, IncrementalConformanceChecker.Goals.FITNESS, 		true,  false, false));
//			settings.add (new IccParameter(0.01, 0.99, 0.01, 0.2, IncrementalConformanceChecker.Goals.FITNESS, 		true,  false, false));
//			settings.add (new IccParameter(0.01, 0.99, 0.01, 0.2, IncrementalConformanceChecker.Goals.FITNESS, 		true,  false, false));
//			
			settings.add (new IccParameter(0.01, 0.99, 0.01, 0.2, IncrementalConformanceChecker.Goals.DEVIATIONS, 	false, false, false));
			//settings.add (new IccParameter(0.05, 0.99, 0.01, 0.2, IncrementalConformanceChecker.Goals.DEVIATIONS, 	false, false, false));
			//settings.add (new IccParameter(0.05, 0.99, 0.01, 0.2, IncrementalConformanceChecker.Goals.DEVIATIONS, 	false, false, false));
//			
//			//TODO approximation with retaining seem to work worse for logs with a small number of trace variants, as critical events may be discarded
			
			//settings.add (new IccParameter(0.05, 0.99, 0.01, 0.2, IncrementalConformanceChecker.Goals.DEVIATIONS, 	true, IncrementalConformanceChecker.Heuristics.NONALIGNING_KNOWN, 	false, false));
			//settings.add (new IccParameter(0.05, 0.99, 0.01, 0.2, IncrementalConformanceChecker.Goals.DEVIATIONS, 	true, IncrementalConformanceChecker.Heuristics.NONALIGNING_KNOWN, 	false, false));
			//settings.add (new IccParameter(0.05, 0.99, 0.01, 0.2, IncrementalConformanceChecker.Goals.DEVIATIONS, 	true, IncrementalConformanceChecker.Heuristics.NONALIGNING_KNOWN, 	false, false));
			
			//settings.add (new IccParameter(0.05, 0.99, 0.01, 0.2, IncrementalConformanceChecker.Goals.DEVIATIONS,   true, IncrementalConformanceChecker.Heuristics.NONALIGNING_ALL, 	false, false));
			//settings.add (new IccParameter(0.05, 0.99, 0.01, 0.2, IncrementalConformanceChecker.Goals.DEVIATIONS, 	true, IncrementalConformanceChecker.Heuristics.NONALIGNING_ALL, 	false, false));
			//settings.add (new IccParameter(0.05, 0.99, 0.01, 0.2, IncrementalConformanceChecker.Goals.DEVIATIONS, 	true, IncrementalConformanceChecker.Heuristics.NONALIGNING_ALL, 	false, false));
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
				//Replayer replayer = ReplayerFactory.createReplayer(net, copyLog, mapping, true);
				ResourceAssignment resAssignment = new ResourceAssignment();
				if (setting.getGoal() == IncrementalConformanceChecker.Goals.RESOURCES) {
					ResourceAssignmentComputer assignmentComputer = new ResourceAssignmentComputer(0.20);
					resAssignment = assignmentComputer.createResourceAssignment(copyLog);
				}
				IncrementalTraceAnalyzer<?> analyzer = TraceAnalyzerFactory.createTraceAnalyzer(setting, mapping, copyLog, net, resAssignment);
				long start = System.currentTimeMillis();
				System.out.println(setting.toString());
				GlobalConformanceResult result = checkForGlobalConformanceWithICC(context, net, copyLog, mapping, analyzer, setting, null, null,start);
				long end = System.currentTimeMillis();
				
				System.out.println(result.toString());
				System.out.println(end-start);
				System.out.println();
				
				//get baseline result as comparison
				Map<String,Double> deviationsRel=new HashMap<String,Double>();
				
				copyLog = (XLog) log.clone();
				Replayer replayer = ReplayerFactory.createReplayer(net, copyLog, mapping, false);
				
				start = System.currentTimeMillis();
				PNRepResult result2 = replayer.computePNRepResult(Progress.INVISIBLE, copyLog);
				end = System.currentTimeMillis();
				System.out.println("replay time Baseline:"+(end-start));
				

				//deviations
				Map<String, Double> deviations = new HashMap<String, Double>();
				for(SyncReplayResult replaySteps : result2) {
					//System.out.println(replaySteps.getStepTypes().toString());
					for (int j=0;j<replaySteps.getStepTypes().size();j++) {
						if(replaySteps.getStepTypes().get(j).toString().equals("Model move")||replaySteps.getStepTypes().get(j).toString().equals("Log move")) {
							//System.out.println(mapping.toString());
							//System.out.println(replaySteps.getStepTypes().get(j).toString()+" , "+replaySteps.getNodeInstance().get(j).toString());
							if(replaySteps.getStepTypes().get(j).toString().equals("Model move")) {
								if(mapping.containsKey(replaySteps.getNodeInstance().get(j))){
									deviations.put(mapping.get(replaySteps.getNodeInstance().get(j)).toString(),deviations.getOrDefault(mapping.get(replaySteps.getNodeInstance().get(j)).toString(), 0.0)+1.0);
								}
								else {
									deviations.put(replaySteps.getNodeInstance().get(j).toString(),deviations.getOrDefault(replaySteps.getNodeInstance().get(j).toString(), 0.0)+1.0);
								}
							}
							if(replaySteps.getStepTypes().get(j).toString().equals("Log move")) {
								deviations.put(replaySteps.getNodeInstance().get(j).toString(),deviations.getOrDefault(replaySteps.getNodeInstance().get(j).toString(), 0.0)+1.0);
							}
							//System.out.println("Updated Deviations: "+deviations);
						}
					}
				}
				double total =0;
				for (java.util.Map.Entry<String, Double> x : deviations.entrySet()) {
					total+=x.getValue();
				}
				double test = total;
				//deviationsAbs=Maps.asMap(deviatingActivities.elementSet(), elem -> deviatingActivities.count(elem));
				deviations.keySet().stream().forEach(x-> deviationsRel.put(x, deviations.get(x)/test));
				Map<String, Double> sortedDeviations =  deviationsRel
						.entrySet()
						.stream()
						.sorted(Map.Entry.<String, Double>comparingByValue().reversed())
						.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
						(e1, e2) -> e1, LinkedHashMap::new));
				System.out.println(sortedDeviations);
			}
			

		}

	}

//	/**
//	 * get results of the baseline conformance checking method for fitness, deviations and resource allocation
//	 * @param context
//	 * @throws Exception
//	 */
//	
	private void baselineResultsEvaluation(UIPluginContext context) throws Exception{
		//TODO baseline for both inputs and baselines
		String[] inputNames = {"RTFM_model2"};
		//String[] inputNames = {"Road_Traffic_Fines_Management_Process", "BPI_Challenge_2012", "Detail_Incident_Activity", "prAm6","prBm6","prCm6","prDm6","prEm6","prFm6","prGm6"};
		//String[] inputNames = {"prAm6","prBm6","prCm6","prDm6","prEm6","prFm6","prGm6"};
		System.out.println("Retrieving Baseline Results");
		for(String input : inputNames) {
			System.out.println(">"+input);
			PrintWriter baselineStats = new PrintWriter(input+"_baseline5.csv", "UTF-8");
			baselineStats.write(String.join(";","time","logSize","fitness","deviations","resources\n"));
			String NET_PATH = "input/" + input + ".pnml";
			String LOG_PATH = "input/" + input + ".xes";
			XLog log = loadLog(LOG_PATH);
			PetrinetGraph net = importPNML(NET_PATH, context);
			System.out.println(">Loaded log and net");
			TransEvClassMapping mapping = computeTransEventMapping(log, net);
			
			double fitness=-1.0;
			Map<String,Double> deviationsRel=new HashMap<String,Double>();
			Map<String, Map<String,Double>> resourcesRelative = new HashMap<String, Map<String,Double>>();

			int repetitions=5;
			int cnt=1;
			for(int i=0;i<repetitions;i++) {
				System.out.println("Baseline Evaluation>"+input+" - repetition "+cnt+"/"+repetitions);
				
				XLog copyLog = (XLog) log.clone();
				Replayer replayer = ReplayerFactory.createReplayer(net, copyLog, mapping, false);
				
				long start = System.currentTimeMillis();
				PNRepResult result = replayer.computePNRepResult(Progress.INVISIBLE, copyLog);
				long end = System.currentTimeMillis();
				//System.out.println("replay time :"+(end-start));
				

				//fitness
				fitness = (Double) result.getInfo().get("Trace Fitness");
				//System.out.println("Fitness: "+fitness);
				
				//deviations
				Map<String, Double> deviations = new HashMap<String, Double>();
				for(SyncReplayResult replaySteps : result) {
					//System.out.println(replaySteps.getStepTypes().toString());
					for (int j=0;j<replaySteps.getStepTypes().size();j++) {
						if(replaySteps.getStepTypes().get(j).toString().equals("Model move")||replaySteps.getStepTypes().get(j).toString().equals("Log move")) {
							//System.out.println(mapping.toString());
							//System.out.println(replaySteps.getStepTypes().get(j).toString()+" , "+replaySteps.getNodeInstance().get(j).toString());
							if(replaySteps.getStepTypes().get(j).toString().equals("Model move")) {
								if(mapping.containsKey(replaySteps.getNodeInstance().get(j))){
									deviations.put(mapping.get(replaySteps.getNodeInstance().get(j)).toString(),deviations.getOrDefault(mapping.get(replaySteps.getNodeInstance().get(j)).toString(), 0.0)+1.0);
								}
								else {
									deviations.put(replaySteps.getNodeInstance().get(j).toString(),deviations.getOrDefault(replaySteps.getNodeInstance().get(j).toString(), 0.0)+1.0);
								}
							}
							if(replaySteps.getStepTypes().get(j).toString().equals("Log move")) {
								deviations.put(replaySteps.getNodeInstance().get(j).toString(),deviations.getOrDefault(replaySteps.getNodeInstance().get(j).toString(), 0.0)+1.0);
							}
							//System.out.println("Updated Deviations: "+deviations);
						}
					}
				}
				double total =0;
				for (java.util.Map.Entry<String, Double> x : deviations.entrySet()) {
					total+=x.getValue();
				}
				double test = total;
				//deviationsAbs=Maps.asMap(deviatingActivities.elementSet(), elem -> deviatingActivities.count(elem));
				deviations.keySet().stream().forEach(x-> deviationsRel.put(x, deviations.get(x)/test));
				
				//System.out.println("Deviations: "+deviationsRel);
				
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
							for(Entry<String, Double> resource : result2.get(activity).entrySet()) {
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
							for (Entry<String, Double> resource : result1.get(activity).entrySet()) {
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
					for(Entry<String, Double> resource : resources.get(activity).entrySet()) {
						resourcesRelative.get(activity).put(resource.getKey(), resource.getValue()/cnts);
					}
					
				}
				//System.out.println("Resources: "+resourcesRelative);
				//System.out.println("");
				
				baselineStats.write(String.join(";",Long.toString(end-start),Integer.toString(log.size()), Double.toString(fitness), deviationsRel.toString(), resourcesRelative.toString()+"\n"));
				cnt++;
			}
			baselineStats.close();
		}
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
		int repetitions =10;

		String[] inputNames = {"RTFM_model2"};
		//String[] inputNames = {"Road_Traffic_Fines_Management_Process", "BPI_Challenge_2012"};
		//String[] inputNames = {"Detail_Incident_Activity"};

		System.out.println("Evaluating parameters");
		for(String input : inputNames) {
			System.out.println(">Loading "+input);
			String NET_PATH = "input/" + input + ".pnml";
			String LOG_PATH = "input/" + input + ".xes";
			XLog log = loadLog(LOG_PATH);
			PetrinetGraph net = importPNML(NET_PATH, context);
			System.out.println(">Done");
			TransEvClassMapping mapping = computeTransEventMapping(log, net);			
			boolean test = true;
			while(test) {
				
			}
			PrintWriter fitness 		= new PrintWriter(input+"_fitness5.csv", "UTF-8");
			PrintWriter fitnessApprox 	= new PrintWriter(input+"_fitnessApprox5.csv", "UTF-8");
			PrintWriter deviation 		= new PrintWriter(input+"_deviations5.csv", "UTF-8");
			PrintWriter deviationApprox = new PrintWriter(input+"_deviationsApprox5.csv", "UTF-8");
			fitness			.write(String.join(";","delta","alpha","epsilon","k","time","logSize","variants","approximated","fitness","deviations","approximationMode","resources\n"));
			fitnessApprox	.write(String.join(";","delta","alpha","epsilon","k","time","logSize","variants","approximated","fitness","deviations","approximationMode","resources\n"));
			deviation		.write(String.join(";","delta","alpha","epsilon","k","time","logSize","variants","approximated","fitness","deviations","approximationMode","resources\n"));
			deviationApprox	.write(String.join(";","delta","alpha","epsilon","k","time","logSize","variants","approximated","fitness","deviations","approximationMode","resources\n"));
			//Delta = 0.01, 0.05, 0.1
			//Alpha = 0.99, 0.95, 0.9
			//Epsilon = 0.01, 0.05, 0.1
			//K = 0.1, 0.2, 0.3
			ArrayList<IccParameter> list = new ArrayList<IccParameter>();
			//fitness
			//list.add(new IccParameter(0.01, 0.99, 0.01, 0.2 , IncrementalConformanceChecker.Goals.FITNESS, true, false, false));
			//list.add(new IccParameter(0.01, 0.99, 0.01, 0.2 , IncrementalConformanceChecker.Goals.DEVIATIONS, false, false, false));
			//list.add(new IccParameter(0.01, 0.99, 0.01, 0.2 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_KNOWN, false, false));
			list.add(new IccParameter(0.01, 0.99, 0.01, 0.1 , IncrementalConformanceChecker.Goals.FITNESS, false, false, false));
			list.add(new IccParameter(0.01, 0.95, 0.01, 0.1 , IncrementalConformanceChecker.Goals.FITNESS, false, false, false));
			list.add(new IccParameter(0.01, 0.90, 0.01, 0.1 , IncrementalConformanceChecker.Goals.FITNESS, false, false, false));
			list.add(new IccParameter(0.05, 0.99, 0.01, 0.1 , IncrementalConformanceChecker.Goals.FITNESS, false, false, false));
			list.add(new IccParameter(0.05, 0.95, 0.01, 0.1 , IncrementalConformanceChecker.Goals.FITNESS, false, false, false));
			list.add(new IccParameter(0.05, 0.90, 0.01, 0.1 , IncrementalConformanceChecker.Goals.FITNESS, false, false, false));
			list.add(new IccParameter(0.10, 0.99, 0.01, 0.1 , IncrementalConformanceChecker.Goals.FITNESS, false, false, false));
			list.add(new IccParameter(0.10, 0.95, 0.01, 0.1 , IncrementalConformanceChecker.Goals.FITNESS, false, false, false));
			list.add(new IccParameter(0.10, 0.90, 0.01, 0.1 , IncrementalConformanceChecker.Goals.FITNESS, false, false, false));
			list.add(new IccParameter(0.01, 0.99, 0.05, 0.1 , IncrementalConformanceChecker.Goals.FITNESS, false, false, false));
			list.add(new IccParameter(0.01, 0.99, 0.10, 0.1 , IncrementalConformanceChecker.Goals.FITNESS, false, false, false));

			
			
			//fitness with approximation
			list.add(new IccParameter(0.01, 0.99, 0.01, 0.1 , IncrementalConformanceChecker.Goals.FITNESS, true, false, false));
			list.add(new IccParameter(0.01, 0.95, 0.01, 0.1 , IncrementalConformanceChecker.Goals.FITNESS, true, false, false));
			list.add(new IccParameter(0.01, 0.90, 0.01, 0.1 , IncrementalConformanceChecker.Goals.FITNESS, true, false, false));
			list.add(new IccParameter(0.05, 0.99, 0.01, 0.1 , IncrementalConformanceChecker.Goals.FITNESS, true, false, false));
			list.add(new IccParameter(0.05, 0.95, 0.01, 0.1 , IncrementalConformanceChecker.Goals.FITNESS, true, false, false));
			list.add(new IccParameter(0.05, 0.90, 0.01, 0.1 , IncrementalConformanceChecker.Goals.FITNESS, true, false, false));
			list.add(new IccParameter(0.10, 0.99, 0.01, 0.1 , IncrementalConformanceChecker.Goals.FITNESS, true, false, false));
			list.add(new IccParameter(0.10, 0.95, 0.01, 0.1 , IncrementalConformanceChecker.Goals.FITNESS, true, false, false));
			list.add(new IccParameter(0.10, 0.90, 0.01, 0.1 , IncrementalConformanceChecker.Goals.FITNESS, true, false, false));
			list.add(new IccParameter(0.01, 0.99, 0.05, 0.1 , IncrementalConformanceChecker.Goals.FITNESS, true, false, false));
			list.add(new IccParameter(0.01, 0.99, 0.10, 0.1 , IncrementalConformanceChecker.Goals.FITNESS, true, false, false));

			list.add(new IccParameter(0.01, 0.99, 0.01, 0.2 , IncrementalConformanceChecker.Goals.FITNESS, true, false, false));
			list.add(new IccParameter(0.01, 0.95, 0.01, 0.2 , IncrementalConformanceChecker.Goals.FITNESS, true, false, false));
			list.add(new IccParameter(0.01, 0.90, 0.01, 0.2 , IncrementalConformanceChecker.Goals.FITNESS, true, false, false));
			list.add(new IccParameter(0.05, 0.99, 0.01, 0.2 , IncrementalConformanceChecker.Goals.FITNESS, true, false, false));
			list.add(new IccParameter(0.05, 0.95, 0.01, 0.2 , IncrementalConformanceChecker.Goals.FITNESS, true, false, false));
			list.add(new IccParameter(0.05, 0.90, 0.01, 0.2 , IncrementalConformanceChecker.Goals.FITNESS, true, false, false));
			list.add(new IccParameter(0.10, 0.99, 0.01, 0.2 , IncrementalConformanceChecker.Goals.FITNESS, true, false, false));
			list.add(new IccParameter(0.10, 0.95, 0.01, 0.2 , IncrementalConformanceChecker.Goals.FITNESS, true, false, false));
			list.add(new IccParameter(0.10, 0.90, 0.01, 0.2 , IncrementalConformanceChecker.Goals.FITNESS, true, false, false));
			list.add(new IccParameter(0.01, 0.99, 0.05, 0.2 , IncrementalConformanceChecker.Goals.FITNESS, true, false, false));
			list.add(new IccParameter(0.01, 0.99, 0.10, 0.2 , IncrementalConformanceChecker.Goals.FITNESS, true, false, false));
			
			list.add(new IccParameter(0.01, 0.99, 0.01, 0.3 , IncrementalConformanceChecker.Goals.FITNESS, true, false, false));
			list.add(new IccParameter(0.01, 0.95, 0.01, 0.3 , IncrementalConformanceChecker.Goals.FITNESS, true, false, false));
			list.add(new IccParameter(0.01, 0.90, 0.01, 0.3 , IncrementalConformanceChecker.Goals.FITNESS, true, false, false));
			list.add(new IccParameter(0.05, 0.99, 0.01, 0.3 , IncrementalConformanceChecker.Goals.FITNESS, true, false, false));
			list.add(new IccParameter(0.05, 0.95, 0.01, 0.3 , IncrementalConformanceChecker.Goals.FITNESS, true, false, false));
			list.add(new IccParameter(0.05, 0.90, 0.01, 0.3 , IncrementalConformanceChecker.Goals.FITNESS, true, false, false));
			list.add(new IccParameter(0.10, 0.99, 0.01, 0.3 , IncrementalConformanceChecker.Goals.FITNESS, true, false, false));
			list.add(new IccParameter(0.10, 0.95, 0.01, 0.3 , IncrementalConformanceChecker.Goals.FITNESS, true, false, false));
			list.add(new IccParameter(0.10, 0.90, 0.01, 0.3 , IncrementalConformanceChecker.Goals.FITNESS, true, false, false));
			list.add(new IccParameter(0.01, 0.99, 0.05, 0.3 , IncrementalConformanceChecker.Goals.FITNESS, true, false, false));
			list.add(new IccParameter(0.01, 0.99, 0.10, 0.3 , IncrementalConformanceChecker.Goals.FITNESS, true, false, false));
			
			
			
			//deviations
			list.add(new IccParameter(0.01, 0.99, 0.01, 0.1 , IncrementalConformanceChecker.Goals.DEVIATIONS, false, false, false));
			list.add(new IccParameter(0.01, 0.95, 0.01, 0.1 , IncrementalConformanceChecker.Goals.DEVIATIONS, false, false, false));
			list.add(new IccParameter(0.01, 0.90, 0.01, 0.1 , IncrementalConformanceChecker.Goals.DEVIATIONS, false, false, false));
			list.add(new IccParameter(0.05, 0.99, 0.01, 0.1 , IncrementalConformanceChecker.Goals.DEVIATIONS, false, false, false));
			list.add(new IccParameter(0.05, 0.95, 0.01, 0.1 , IncrementalConformanceChecker.Goals.DEVIATIONS, false, false, false));
			list.add(new IccParameter(0.05, 0.90, 0.01, 0.1 , IncrementalConformanceChecker.Goals.DEVIATIONS, false, false, false));
			list.add(new IccParameter(0.10, 0.99, 0.01, 0.1 , IncrementalConformanceChecker.Goals.DEVIATIONS, false, false, false));
			list.add(new IccParameter(0.10, 0.95, 0.01, 0.1 , IncrementalConformanceChecker.Goals.DEVIATIONS, false, false, false));
			list.add(new IccParameter(0.10, 0.90, 0.01, 0.1 , IncrementalConformanceChecker.Goals.DEVIATIONS, false, false, false));
			list.add(new IccParameter(0.01, 0.99, 0.05, 0.1 , IncrementalConformanceChecker.Goals.DEVIATIONS, false, false, false));
			list.add(new IccParameter(0.01, 0.99, 0.10, 0.1 , IncrementalConformanceChecker.Goals.DEVIATIONS, false, false, false));
			
			
			
			//deviations with approximation - nonaligning_all
			list.add(new IccParameter(0.01, 0.99, 0.01, 0.1 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_ALL, false, false));
			list.add(new IccParameter(0.01, 0.95, 0.01, 0.1 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_ALL, false, false));
			list.add(new IccParameter(0.01, 0.90, 0.01, 0.1 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_ALL, false, false));
			list.add(new IccParameter(0.05, 0.99, 0.01, 0.1 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_ALL, false, false));
			list.add(new IccParameter(0.05, 0.95, 0.01, 0.1 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_ALL, false, false));
			list.add(new IccParameter(0.05, 0.90, 0.01, 0.1 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_ALL, false, false));
			list.add(new IccParameter(0.10, 0.99, 0.01, 0.1 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_ALL, false, false));
			list.add(new IccParameter(0.10, 0.95, 0.01, 0.1 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_ALL, false, false));
			list.add(new IccParameter(0.10, 0.90, 0.01, 0.1 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_ALL, false, false));
			list.add(new IccParameter(0.01, 0.99, 0.05, 0.1 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_ALL, false, false));
			list.add(new IccParameter(0.01, 0.99, 0.10, 0.1 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_ALL, false, false));

			list.add(new IccParameter(0.01, 0.99, 0.01, 0.2 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_ALL, false, false));
			list.add(new IccParameter(0.01, 0.95, 0.01, 0.2 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_ALL, false, false));
			list.add(new IccParameter(0.01, 0.90, 0.01, 0.2 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_ALL, false, false));
			list.add(new IccParameter(0.05, 0.99, 0.01, 0.2 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_ALL, false, false));
			list.add(new IccParameter(0.05, 0.95, 0.01, 0.2 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_ALL, false, false));
			list.add(new IccParameter(0.05, 0.90, 0.01, 0.2 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_ALL, false, false));
			list.add(new IccParameter(0.10, 0.99, 0.01, 0.2 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_ALL, false, false));
			list.add(new IccParameter(0.10, 0.95, 0.01, 0.2 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_ALL, false, false));
			list.add(new IccParameter(0.10, 0.90, 0.01, 0.2 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_ALL, false, false));
			list.add(new IccParameter(0.01, 0.99, 0.05, 0.2 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_ALL, false, false));
			list.add(new IccParameter(0.01, 0.99, 0.10, 0.2 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_ALL, false, false));
			
			list.add(new IccParameter(0.01, 0.99, 0.01, 0.3 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_ALL, false, false));
			list.add(new IccParameter(0.01, 0.95, 0.01, 0.3 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_ALL, false, false));
			list.add(new IccParameter(0.01, 0.90, 0.01, 0.3 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_ALL, false, false));
			list.add(new IccParameter(0.05, 0.99, 0.01, 0.3 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_ALL, false, false));
			list.add(new IccParameter(0.05, 0.95, 0.01, 0.3 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_ALL, false, false));
			list.add(new IccParameter(0.05, 0.90, 0.01, 0.3 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_ALL, false, false));
			list.add(new IccParameter(0.10, 0.99, 0.01, 0.3 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_ALL, false, false));
			list.add(new IccParameter(0.10, 0.95, 0.01, 0.3 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_ALL, false, false));
			list.add(new IccParameter(0.10, 0.90, 0.01, 0.3 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_ALL, false, false));
			list.add(new IccParameter(0.01, 0.99, 0.05, 0.3 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_ALL, false, false));
			list.add(new IccParameter(0.01, 0.99, 0.10, 0.3 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_ALL, false, false));
			
	
			
			//deviations with approximation - nonaligning_known
			list.add(new IccParameter(0.01, 0.99, 0.01, 0.1 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_KNOWN, false, false));
			list.add(new IccParameter(0.01, 0.95, 0.01, 0.1 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_KNOWN, false, false));
			list.add(new IccParameter(0.01, 0.90, 0.01, 0.1 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_KNOWN, false, false));
			list.add(new IccParameter(0.05, 0.99, 0.01, 0.1 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_KNOWN, false, false));
			list.add(new IccParameter(0.05, 0.95, 0.01, 0.1 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_KNOWN, false, false));
			list.add(new IccParameter(0.05, 0.90, 0.01, 0.1 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_KNOWN, false, false));
			list.add(new IccParameter(0.10, 0.99, 0.01, 0.1 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_KNOWN, false, false));
			list.add(new IccParameter(0.10, 0.95, 0.01, 0.1 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_KNOWN, false, false));
			list.add(new IccParameter(0.10, 0.90, 0.01, 0.1 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_KNOWN, false, false));
			list.add(new IccParameter(0.01, 0.99, 0.05, 0.1 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_KNOWN, false, false));
			list.add(new IccParameter(0.01, 0.99, 0.10, 0.1 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_KNOWN, false, false));

			list.add(new IccParameter(0.01, 0.99, 0.01, 0.2 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_KNOWN, false, false));
			list.add(new IccParameter(0.01, 0.95, 0.01, 0.2 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_KNOWN, false, false));
			list.add(new IccParameter(0.01, 0.90, 0.01, 0.2 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_KNOWN, false, false));
			list.add(new IccParameter(0.05, 0.99, 0.01, 0.2 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_KNOWN, false, false));
			list.add(new IccParameter(0.05, 0.95, 0.01, 0.2 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_KNOWN, false, false));
			list.add(new IccParameter(0.05, 0.90, 0.01, 0.2 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_KNOWN, false, false));
			list.add(new IccParameter(0.10, 0.99, 0.01, 0.2 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_KNOWN, false, false));
			list.add(new IccParameter(0.10, 0.95, 0.01, 0.2 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_KNOWN, false, false));
			list.add(new IccParameter(0.10, 0.90, 0.01, 0.2 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_KNOWN, false, false));
			list.add(new IccParameter(0.01, 0.99, 0.05, 0.2 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_KNOWN, false, false));
			list.add(new IccParameter(0.01, 0.99, 0.10, 0.2 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_KNOWN, false, false));
			
			list.add(new IccParameter(0.01, 0.99, 0.01, 0.3 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_KNOWN, false, false));
			list.add(new IccParameter(0.01, 0.95, 0.01, 0.3 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_KNOWN, false, false));
			list.add(new IccParameter(0.01, 0.90, 0.01, 0.3 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_KNOWN, false, false));
			list.add(new IccParameter(0.05, 0.99, 0.01, 0.3 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_KNOWN, false, false));
			list.add(new IccParameter(0.05, 0.95, 0.01, 0.3 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_KNOWN, false, false));
			list.add(new IccParameter(0.05, 0.90, 0.01, 0.3 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_KNOWN, false, false));
			list.add(new IccParameter(0.10, 0.99, 0.01, 0.3 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_KNOWN, false, false));
			list.add(new IccParameter(0.10, 0.95, 0.01, 0.3 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_KNOWN, false, false));
			list.add(new IccParameter(0.10, 0.90, 0.01, 0.3 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_KNOWN, false, false));
			list.add(new IccParameter(0.01, 0.99, 0.05, 0.3 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_KNOWN, false, false));
			list.add(new IccParameter(0.01, 0.99, 0.10, 0.3 , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_KNOWN, false, false));
			
			int total=list.size()*repetitions;
			int cnt=1;
			for(IccParameter parameter : list) {
				for(int i=0;i<repetitions;i++) {
					System.out.println("Parameter Evaluation>"+input+" - repetition "+cnt+"/"+total);
					
					XLog copyLog=(XLog) log.clone();
					//Replayer replayer = ReplayerFactory.createReplayer(net, copyLog, mapping, true);
					IncrementalTraceAnalyzer<?> analyzer = TraceAnalyzerFactory.createTraceAnalyzer(parameter, mapping, log, net, null);
					long start = System.currentTimeMillis();
					GlobalConformanceResult result = checkForGlobalConformanceWithICC(context, net, copyLog, mapping, analyzer, parameter, null, null, SEEDS[i]);
					long end = System.currentTimeMillis();
					String out=String.join(";", 
							Double.toString(parameter.getDelta()),Double.toString(parameter.getAlpha()),Double.toString(parameter.getEpsilon()),Double.toString(parameter.getK()),
							Long.toString(end-start), Integer.toString(result.getCnt()),Integer.toString(result.getTotalVariants()), Integer.toString(result.getApproximatedVariants()),
							Double.toString(result.getFitness()), result.getDeviations().toString(), parameter.getApproximationHeuristic().toString(), result.getResourceDeviations().toString()+"\n");
					System.out.println(end-start);
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

	/**
	 * evaluate the approximation-based deviating activity use case using the prefixsuffix heuristic.
	 * This heuristic needs more granular fine tuning than the others, hence it's own evaluation run
	 * @param context
	 * @throws Exception
	 */
	private void prefixsuffixEvaluation(UIPluginContext context) throws Exception{
		//String[] inputNames = {"Road_Traffic_Fines_Management_Process"};

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
			
			PrintWriter prefixsuffix = new PrintWriter(input+"_prefixsuffix5.csv", "UTF-8");
			prefixsuffix.write(String.join(";","delta","alpha","epsilon","k","time","logSize","variants","approximated","fitness","deviations","approximationMode","resources\n"));
				
			ArrayList<IccParameter> list = new ArrayList<IccParameter>();
			//deviations approximated - prefix suffix
			//list.add(new IccParameter(0.01, 0.99, 0.01, 0.3  , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.PREFIXSUFFIX, false, false));
			//list.add(new IccParameter(0.01, 0.99, 0.01, 0.2  , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.PREFIXSUFFIX, false, false));
			list.add(new IccParameter(0.01, 0.99, 0.01, 0.01  , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.PREFIXSUFFIX, false, false));
			list.add(new IccParameter(0.01, 0.99, 0.01, 0.05  , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.PREFIXSUFFIX, false, false));
			list.add(new IccParameter(0.01, 0.99, 0.01, 0.1   , IncrementalConformanceChecker.Goals.DEVIATIONS, true, IncrementalConformanceChecker.Heuristics.PREFIXSUFFIX, false, false));
			int cnt=1;
			int total=list.size()*repetitions;
			for(IccParameter parameter : list) {
				for(int i=0;i<repetitions;i++) {
					System.out.println("prefixSuffix Evaluation>"+input+" - repetition "+cnt+"/"+total);
					XLog copyLog=(XLog) log.clone();
					//Replayer replayer = ReplayerFactory.createReplayer(net, copyLog, mapping, true);
					IncrementalTraceAnalyzer<?> analyzer = TraceAnalyzerFactory.createTraceAnalyzer(parameter, mapping, log, net, null);
					long start = System.currentTimeMillis();
					GlobalConformanceResult result = checkForGlobalConformanceWithICC(context, net, copyLog, mapping, analyzer, parameter, null, null, SEEDS[i]);
					long end = System.currentTimeMillis();
					String out=String.join(";", 
							Double.toString(parameter.getDelta()),Double.toString(parameter.getAlpha()),Double.toString(parameter.getEpsilon()),Double.toString(parameter.getK()),
							Long.toString(end-start), Integer.toString(result.getCnt()),Integer.toString(result.getTotalVariants()), Integer.toString(result.getApproximatedVariants()),
							Double.toString(result.getFitness()), result.getDeviations().toString(), parameter.getApproximationHeuristic().toString(), result.getResourceDeviations().toString()+"\n");
					System.out.println(out);
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
	private void qualityCheckingEvaluation(UIPluginContext context) throws Exception{
//		String[] inputNames = {"BPI_Challenge_2012"};
		String[] inputNames = {"BPI_Challenge_2012"};
		System.out.println("Evaluating Quality Checking");
		int repetitions =1;
		PrintWriter writer = new PrintWriter("qualitychecking5.csv", "UTF-8");
		writer.write("log; delta; alpha; epsilon; quality checker; dfDistrib; dep measure; attrib; sample size; times triggered; fitness;"
				+ " time; distToOriginal; original; df repr.; dep measure repr.; attribute repr.; \n");

		for(String input : inputNames) {
			System.out.println("	>"+input);
			String NET_PATH = "input/" + input + ".pnml";
			String LOG_PATH = "input/" + input + ".xes";
			XLog log = loadLog(LOG_PATH);
			PetrinetGraph net = importPNML(NET_PATH, context);
			System.out.println("	>Loaded log and net");
			TransEvClassMapping mapping = computeTransEventMapping(log, net);				
			
			double origFitness = 0;
			if (input.equals("BPI_Challenge_2014")) {
				origFitness = 0.9008334329271833; 
			}
			if (input.equals("Road_Traffic_Fines_Management_Process")) {
				origFitness = 0.9823429057173879;
			}
			if (input.equals("BPI_Challenge_2012")) {
				origFitness = 0.7310599598129743;
			}

			
			ArrayList<IccParameter> settingList = new ArrayList<IccParameter>();
			
			IccParameter setting;
			QualityCheckManager checker;
			//internal checks
			//without quality checking
			setting =  new IccParameter(0.01, 0.99, 0.01, 0.2, IncrementalConformanceChecker.Goals.FITNESS, false, false, false);
			checker = new QualityCheckManager(true);
			setting.setInternalQualityCheckManager(checker); // necessary for cleaner code w.r.t. reset
			setting.setStoreSampledTraces(true);
			settingList.add(setting);
			
			
			//with directly follows checking
			setting =  new IccParameter(0.01, 0.99, 0.01, 0.2, IncrementalConformanceChecker.Goals.FITNESS, false, true, false);
			checker = new QualityCheckManager(true);
			checker.addDirectlyFollowsChecking();
			setting.setInternalQualityCheckManager(checker);
			setting.setStoreSampledTraces(true);
			settingList.add(setting);
			//with dependency measure checking
			setting =  new IccParameter(0.01, 0.99, 0.01, 0.2, IncrementalConformanceChecker.Goals.FITNESS, false, true, false);
			checker = new QualityCheckManager(true);
			checker.addDependencyMeasureChecking();
			setting.setInternalQualityCheckManager(checker);
			setting.setStoreSampledTraces(true);
			settingList.add(setting);
			
			
			//with data attribute
			setting =  new IccParameter(0.01, 0.99, 0.01, 0.2, IncrementalConformanceChecker.Goals.FITNESS, false, true, false);
			checker = new QualityCheckManager(true);
			addDataAttribute(checker, input);
			setting.setInternalQualityCheckManager(checker);
			setting.setStoreSampledTraces(true);
			settingList.add(setting);
			//with directly follows and data attribute
			setting =  new IccParameter(0.01, 0.99, 0.01, 0.2, IncrementalConformanceChecker.Goals.FITNESS, false, true, false);
			checker = new QualityCheckManager(true);
			checker.addDirectlyFollowsChecking();				
			addDataAttribute(checker, input);
			setting.setInternalQualityCheckManager(checker);
			setting.setStoreSampledTraces(true);
			settingList.add(setting);
			//with dependency measure and data attribute
			setting =  new IccParameter(0.01, 0.99, 0.01, 0.2, IncrementalConformanceChecker.Goals.FITNESS, false, true, false);
			checker = new QualityCheckManager(true);
			checker.addDependencyMeasureChecking();				
			addDataAttribute(checker, input);
			setting.setInternalQualityCheckManager(checker);
			setting.setStoreSampledTraces(true);
			settingList.add(setting);
			
//			settingList = new ArrayList<IccParameter>(); // quick hack to skip internal
			for(IccParameter currSetting : settingList) {
				checker = currSetting.getInternalQualityCheckManager();
				for(int j=0;j<repetitions;j++) {
					XLog copyLog=(XLog) log.clone();
					IncrementalTraceAnalyzer<?> analyzer = TraceAnalyzerFactory.createTraceAnalyzer(currSetting, mapping, log, net, null);
					long start = System.currentTimeMillis();
					IncrementalConformanceChecker icc = new IncrementalConformanceChecker(analyzer, currSetting, SEEDS[j]);
					GlobalConformanceResult result = icc.apply(context, copyLog, net, mapping);
					long end = System.currentTimeMillis();
					long time = end-start;
					
					// do some sample quality checking here. Can I retrieve the sampled traces?
					String samplequalityString = assessSampleQualityVersusFullLog(input, log, icc.getSampledTraces());
					
					String out = input + ";" + currSetting.getDelta() + ";" + currSetting.getAlpha() + ";" + currSetting.getEpsilon() + ";" + "internal" + ";" +
					checker.hasDirectlyFollowsChecker() + ";" + checker.hasDependencyMeasureChecker() + ";" + checker.hasAttributeChecker()  + ";" + icc.getSampledTraces().size() + ";" +  checker.timesTriggered()  + ";" +  
					result.getFitness()  + ";" +  (time) + ";"+ String.valueOf(Math.abs(result.getFitness() - origFitness))  + ";" +  origFitness + ";" + samplequalityString;
					System.out.println(out);
					out = out.replace('.', ',');
					writer.write(out + "\n");
					checker.resetAll();
				}
			}
			
//			//external
			settingList = new ArrayList<IccParameter>();
			//with directly follows checking
			setting =  new IccParameter(0.01, 0.99, 0.01, 0.2, IncrementalConformanceChecker.Goals.FITNESS, false, false, true);
			checker = new QualityCheckManager(false);
			checker.addDirectlyFollowsChecking();
			setting.setExternalQualityCheckManager(checker);
			setting.setStoreSampledTraces(true);
			settingList.add(setting);
				
			//with data attribute
			setting =  new IccParameter(0.01, 0.99, 0.01, 0.2, IncrementalConformanceChecker.Goals.FITNESS, false, false, true);
			checker = new QualityCheckManager(false);
			addDataAttribute(checker, input);
			setting.setExternalQualityCheckManager(checker);
			setting.setStoreSampledTraces(true);
			settingList.add(setting);
			//with directly follows and data attribute
			setting =  new IccParameter(0.01, 0.99, 0.01, 0.2, IncrementalConformanceChecker.Goals.FITNESS, false, false, true);
			checker = new QualityCheckManager(false);
			checker.addDirectlyFollowsChecking();				
			addDataAttribute(checker, input);
			setting.setExternalQualityCheckManager(checker);
			setting.setStoreSampledTraces(true);
			settingList.add(setting);			
			//settingList = new ArrayList<IccParameter>(); // quick hack to skip all settings with external quality checking
			
			for(IccParameter currSetting : settingList) {
				checker = currSetting.getExternalQualityCheckManager();
				for(int j=0;j<repetitions;j++) {
					XLog copyLog=(XLog) log.clone();
					IncrementalTraceAnalyzer<?> analyzer = TraceAnalyzerFactory.createTraceAnalyzer(currSetting, mapping, log, net, null);
					long start = System.currentTimeMillis();
					IncrementalConformanceChecker icc = new IncrementalConformanceChecker(analyzer, currSetting, SEEDS[j]);
					GlobalConformanceResult result = icc.apply(context, copyLog, net, mapping);
					long end = System.currentTimeMillis();
					long time = end-start;
					
					// do some sample quality checking here. Can I retrieve the sampled traces?
					String samplequalityString = assessSampleQualityVersusFullLog(input, log, icc.getSampledTraces());
					
					String out = input + ";" + currSetting.getDelta() + ";" + currSetting.getAlpha() + ";" + currSetting.getEpsilon() + ";" + "external" + ";" +
					checker.hasDirectlyFollowsChecker() + ";" + checker.hasDependencyMeasureChecker() + ";" + checker.hasAttributeChecker()  + ";" + icc.getSampledTraces().size() + ";" +  checker.timesTriggered()  + ";" +  
					result.getFitness()  + ";" +  (time) + ";"+ String.valueOf(Math.abs(result.getFitness() - origFitness))  + ";" +  origFitness + ";" + samplequalityString;
					System.out.println(out);
					out = out.replace('.', ',');
					writer.write(out + "\n");
					checker.resetAll();
				}
			}
		}
		writer.close();
	}
	
	private void addDataAttribute(QualityCheckManager manager, String logname) {
		if (logname.equals("Road_Traffic_Fines_Management_Process")) {
			manager.addNumericEventAttribute("Create Fine", "amount");
		}
		if (logname.equals("BPI_Challenge_2012")) {
			manager.addNominalEventAttribute("W_Completeren aanvraag", "org:resource");
		}
		if (logname.equals("Detail_Incident_Activity")) {
			manager.addNominalEventAttribute("Closed", "org:resource");
		}
	}
	
	private String assessSampleQualityVersusFullLog(String input, XLog originalLog, XLog sample) {
	
		QualityCheckManager checker = new QualityCheckManager(false);
		checker.addDirectlyFollowsChecking();
		checker.addDependencyMeasureChecking();
		addDataAttribute(checker, input);
		
		for (XTrace trace : sample) {
			checker.addTraceToDistributions(trace);
		}
		List<String> strValues = new ArrayList<String>();
		for (AbstractValueDistribution distrib : checker.getDistributions()) {
			AbstractValueDistribution validationDistrib = distrib.emptyCopy();
			for (XTrace trace : originalLog) {
				validationDistrib.addTrace(trace);
			}
			double val = distrib.computeTotalDistance(validationDistrib); 
			strValues.add(String.valueOf(val));
		}
		return String.join(";", strValues);
	}
	
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
		String[] inputNames = {/*"prAm6","prBm6",*/"prCm6","prDm6","prEm6","prFm6","prGm6"};
		System.out.println("Evaluating synthetic log model pairs");
		int repetitions =10;

		for(String input : inputNames) {
			PrintWriter synthetic = new PrintWriter(input+"_synthetic5.csv", "UTF-8");
			synthetic.write(String.join(";","delta","alpha","epsilon","k","time","logSize","variants","approximated","fitness","deviations","approximationMode","resources\n"));

			String NET_PATH = "input/" + input + ".pnml";
			String LOG_PATH = "input/" + input + ".xes";
			XLog log = loadLog(LOG_PATH);
			PetrinetGraph net = importPNML(NET_PATH, context);
			System.out.println("	>Loaded log and net");
			TransEvClassMapping mapping = computeTransEventMapping(log, net);
			
			List<IccParameter> settings = new ArrayList<IccParameter>();
			settings.add(new IccParameter(0.05, 0.99, 0.01, 0.2 , IncrementalConformanceChecker.Goals.FITNESS, false, false, false));
			settings.add(new IccParameter(0.01, 0.99, 0.01, 0.2 , IncrementalConformanceChecker.Goals.FITNESS, false, false, false));
	
			for(IccParameter parameter : settings) {
				int cnt=1;
				int total=repetitions;
				for(int i=0;i<repetitions;i++) {
					System.out.println("Synthetic Evaluation>"+input+" - repetition "+cnt+"/"+total);
					XLog copyLog=(XLog) log.clone();
					//Replayer replayer = ReplayerFactory.createReplayer(net, copyLog, mapping, true);
					IncrementalTraceAnalyzer<?> analyzer = TraceAnalyzerFactory.createTraceAnalyzer(parameter, mapping, log, net, null);
					long start = System.currentTimeMillis();
					GlobalConformanceResult result = checkForGlobalConformanceWithICC(context, net, copyLog, mapping, analyzer, parameter, null, null, SEEDS[i]);
					long end = System.currentTimeMillis();
					String out=String.join(";", 
							Double.toString(parameter.getDelta()),Double.toString(parameter.getAlpha()),Double.toString(parameter.getEpsilon()),Double.toString(parameter.getK()),
							Long.toString(end-start), Integer.toString(result.getCnt()),Integer.toString(result.getTotalVariants()), Integer.toString(result.getApproximatedVariants()),
							Double.toString(result.getFitness()), result.getDeviations().toString(), parameter.getApproximationHeuristic().toString(), result.getResourceDeviations().toString()+"\n");
					synthetic.write(out);
					cnt++;
				}
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
	private void resourceDeviationEvaluation(UIPluginContext context) throws Exception {
		String[] inputNames = {"BPI_Challenge_2012"};
		System.out.println("Evaluating resource deviations");
		int repetitions =10;

		for(String input : inputNames) {
			System.out.println("	>"+input);

			String NET_PATH = "input/" + input + ".pnml";
			String LOG_PATH = "input/" + input + ".xes";
			XLog log = loadLog(LOG_PATH);
			PetrinetGraph net = importPNML(NET_PATH, context);
			System.out.println("	>Loaded log and net");
			TransEvClassMapping mapping = computeTransEventMapping(log, net);				
			
			PrintWriter resources = new PrintWriter(input+"_resources10.csv", "UTF-8");
			resources.write(String.join(";","delta","alpha","epsilon","k","time","logSize","variants","approximated","approxThenCalc","fitness","deviations","approximationMode","resources\n"));
			
			ArrayList<IccParameter> list = new ArrayList<IccParameter>();
			//deviations approximated - prefix suffix
			list.add(new IccParameter(0.01, 0.99, 0.01, 0.1 , IncrementalConformanceChecker.Goals.RESOURCES, false, IncrementalConformanceChecker.Heuristics.NONALIGNING_KNOWN, false, false));
			list.add(new IccParameter(0.01, 0.99, 0.01, 0.1 , IncrementalConformanceChecker.Goals.RESOURCES, true, IncrementalConformanceChecker.Heuristics.NONALIGNING_KNOWN, false, false));

			int cnt=1;
			int total = repetitions*list.size();
			for(IccParameter parameter : list) {
				for(int i=0;i<repetitions;i++) {
					System.out.println("Resource Evaluation>"+input+" - repetition "+cnt+"/"+total);
					XLog copyLog=(XLog) log.clone();
					ResourceAssignment resAssignment = new ResourceAssignment();
					ResourceAssignmentComputer assignmentComputer = new ResourceAssignmentComputer(0.20);
					resAssignment = assignmentComputer.createResourceAssignment(copyLog);
					IncrementalTraceAnalyzer<?> analyzer = TraceAnalyzerFactory.createTraceAnalyzer(parameter, mapping, log, net, resAssignment);
					long start = System.currentTimeMillis();
					GlobalConformanceResult result = checkForGlobalConformanceWithICC(context, net, copyLog, mapping, analyzer, parameter, null, null, SEEDS[i]);
					long end = System.currentTimeMillis();
					System.out.println(end-start);
					String out=String.join(";", 
							Double.toString(parameter.getDelta()),Double.toString(parameter.getAlpha()),Double.toString(parameter.getEpsilon()),Double.toString(parameter.getK()),
							Long.toString(end-start), Integer.toString(result.getCnt()),Integer.toString(result.getTotalVariants()), Integer.toString(result.getApproximatedVariants()), Integer.toString(result.GetApproxThencalc()),
							Double.toString(result.getFitness()), result.getDeviations().toString(), parameter.getApproximationHeuristic().toString(), result.getResourceDeviations().toString()+"\n");
					resources.write(out);
					cnt++;
				}
			}
			resources.close();
		}
	}
//	
//
//	/**
//	 * get different sample sizes for the different possible binomial confidence intervals
//	 * @param context
//	 */
	private void sampleSizeEvaluation(UIPluginContext context) {
		Double[] deltas = {0.01, 0.05, 0.10};
		Double[] alphas = {0.99, 0.95, 0.90};
		for(Double delta : deltas) {
			for(Double alpha : alphas) {
				double minimalSampleSize = ThresholdCalculator.calculateThreshold(delta, alpha);
				System.out.println("Delta: "+delta+", Alpha: "+alpha+", N_min: "+minimalSampleSize);
			}	
		}
	}
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
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 */
	
	private GlobalConformanceResult checkForGlobalConformanceWithICC(UIPluginContext context, PetrinetGraph net, XLog log, TransEvClassMapping mapping,
			IncrementalTraceAnalyzer<?> analyzer, IccParameter iccParameters, QualityCheckManager internalQualityCheckManager, QualityCheckManager externalQualityCheckManager, long seed) throws AStarException, InterruptedException, ExecutionException {
		IncrementalConformanceChecker icc = new IncrementalConformanceChecker(analyzer, iccParameters, seed);
		return icc.apply(context, log, net, mapping);
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
			//System.out.println("Find Partner for: "+t.getLabel());
			//for (XEventClass x: ecLog.getClasses()) {
			//	System.out.println(x.getId());
			//}
			//System.out.println( ecLog.toString());
			//TODO: this part is rather hacky, I'll admit.
			XEventClass eventClass = ecLog.getByIdentity(t.getLabel() + "+complete");
			//if (eventClass == null) {
			//	eventClass = ecLog.getByIdentity(t.getLabel() + "+START");
			//}
			if (eventClass == null) {
				eventClass = ecLog.getByIdentity(t.getLabel() + "+COMPLETE");
			}
			//if (eventClass == null) {
			//	eventClass = ecLog.getByIdentity(t.getLabel() + "+SCHEDULE");
			//}
			if (eventClass == null) {
				eventClass = ecLog.getByIdentity(t.getLabel());
			}
			
			if (eventClass != null) {
				System.out.println("Transition "+t.getLabel()+" -> EventClass "+eventClass.getId());
				mapping.put(t, eventClass);
			} else {
				System.out.println("Transition "+t.getLabel()+" -> EventClass "+evClassDummy.getId());
				mapping.put(t, evClassDummy);
				t.setInvisible(true);
			}
		}
		return mapping;
	}
}