package plugins;

import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;
import org.processmining.plugins.petrinet.replayer.ui.PNReplayerUI;

import conformance.IncrementalConformanceChecker;
import conformance.IncrementalConformanceChecker.Goals;
import conformance.IncrementalConformanceChecker.Heuristics;
import conformance.traceAnalysis.IncrementalTraceAnalyzer;
import conformance.traceAnalysis.TraceAnalyzerFactory;
//TODO own functions for parameter windows, reduce number of plugins, refactor whole structure
import nl.tue.astar.AStarException;
import qualitychecking.QualityCheckManager;
import resourcedeviations.ResourceAssignment;
import ressources.GlobalConformanceResult;
import ressources.IccParameter;

/**
 * @author MartinBauer
 */
public class IncrementalConformanceCheckerPlugins{
	@Plugin(name = "Test - Check global Conformance with Incremental Conformance Checker", returnLabels = { "Global Conformance Result" }, returnTypes = { GlobalConformanceResult.class }, parameterLabels = {}, userAccessible = true)
	@UITopiaVariant(affiliation = "Humboldt-University Berlin", author = "Martin Bauer", email = "bauemart@hu-berlin.de", uiLabel = UITopiaVariant.USEPLUGIN)
	@PluginVariant(variantLabel = "Check global Conformance with Incremental Conformance Checker", requiredParameterLabels = {0,1})
	public GlobalConformanceResult CheckForGlobalConformanceWithICCUI(final UIPluginContext context, PetrinetGraph net, XLog log) throws Exception {
		//TODO parameter UI
		double delta=0.01;
		double alpha=0.99;
		double epsilon=0.01;
		double k=0.2;
		Goals goal=IncrementalConformanceChecker.Goals.FITNESS;
		Heuristics approximationHeuristic = IncrementalConformanceChecker.Heuristics.NONALIGNING_KNOWN;
		boolean useApproximation = false;
		boolean checkInternalQuality = false;
		boolean checkExternalQuality = false;
		IncrementalPNReplayerUI pnReplayerUI = new IncrementalPNReplayerUI();
		Object[] resultConfiguration = pnReplayerUI.getConfiguration(context, net, log);
		TransEvClassMapping mapping = (TransEvClassMapping) resultConfiguration[PNReplayerUI.MAPPING];
		
		//TODO make classifier accessible in resultconfiguration of parameter UI
		XEventClassifier classifier = pnReplayerUI.classifier;
		System.out.println("Used Classifier: "+classifier.name());
		
		XLog copyLog = (XLog) log.clone();
		IccParameter iccParameters = new IccParameter(delta, alpha, epsilon, k, goal, useApproximation, approximationHeuristic, checkInternalQuality, checkExternalQuality);

		QualityCheckManager internalQualityCheckManager = new QualityCheckManager(true);
		internalQualityCheckManager.addDirectlyFollowsChecking();
		QualityCheckManager externalQualityCheckManager = new QualityCheckManager(false);
		externalQualityCheckManager.addDirectlyFollowsChecking();
			//if RTF
		internalQualityCheckManager.addNumericEventAttribute("Create Fine", "amount");
			//ifBPI2012
		//internalQualityCheckManager.addNominalEventAttribute("W_Completeren aanvraag", "org:resource");
			//ifBPI2014
		//internalQualityCheckManager.addNominalEventAttribute("Closed", "org:resource");
			
			//if RTF
		externalQualityCheckManager.addNumericEventAttribute("Create Fine", "amount");
			//ifBPI2012
		//externalQualityCheckManager.addNominalEventAttribute("W_Completeren aanvraag", "org:resource");
			//ifBPI2014
		//externalQualityCheckManager.addNominalEventAttribute("Closed", "org:resource");
		iccParameters.setInternalQualityCheckManager(internalQualityCheckManager);
		iccParameters.setExternalQualityCheckManager(externalQualityCheckManager);
		
		ResourceAssignment resourceAssignment = new ResourceAssignment();

		//Replayer replayer = ReplayerFactory.createReplayer(net, copyLog, mapping, true);
		IncrementalTraceAnalyzer<?> analyzer = TraceAnalyzerFactory.createTraceAnalyzer(iccParameters, mapping, classifier, log, net, resourceAssignment);
		GlobalConformanceResult result = checkForGlobalConformanceWithICC(context, net, copyLog, analyzer, iccParameters, internalQualityCheckManager, externalQualityCheckManager);

		//TODO visualizer
		System.out.println(result.toString());
		System.out.println();
		return result;
	} 

	private GlobalConformanceResult checkForGlobalConformanceWithICC(UIPluginContext context, PetrinetGraph net, XLog log, 
			IncrementalTraceAnalyzer<?> analyzer, IccParameter iccParameters, QualityCheckManager internalQualityCheckManager, QualityCheckManager externalQualityCheckManager) throws AStarException, InterruptedException, ExecutionException {
		IncrementalConformanceChecker icc = new IncrementalConformanceChecker(analyzer, iccParameters);
		return icc.apply(context, log, net, IncrementalConformanceChecker.SamplingMode.BINOMIAL);
	}
}