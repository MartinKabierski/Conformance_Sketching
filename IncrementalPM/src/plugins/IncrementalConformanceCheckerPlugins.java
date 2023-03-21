package plugins;

import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.deckfour.uitopia.api.event.TaskListener.InteractionResult;
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
import org.processmining.framework.util.ui.widgets.ProMComboBox;
import org.processmining.framework.util.ui.widgets.ProMPropertiesPanel;
import org.processmining.framework.util.ui.widgets.ProMTextField;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;
import org.processmining.plugins.petrinet.replayer.ui.PNReplayerUI;

import com.fluxicon.slickerbox.components.NiceSlider;
import com.fluxicon.slickerbox.components.NiceSlider.Orientation;
import com.fluxicon.slickerbox.factory.SlickerFactory;

import conformance.IncrementalConformanceChecker;
import conformance.IncrementalConformanceChecker.Goals;
import conformance.IncrementalConformanceChecker.Heuristics;
import conformance.traceAnalysis.IncrementalTraceAnalyzer;
import conformance.traceAnalysis.TraceAnalyzerFactory;
import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstants;
//TODO own functions for parameter windows, reduce number of plugins, refactor whole structure
import nl.tue.astar.AStarException;
import qualitychecking.QualityCheckManager;
import resourcedeviations.ResourceAssignment;
import ressources.IncrementalConformanceResult;
import ressources.IccParameter;

/**
 * @author MartinBauer
 */
public class IncrementalConformanceCheckerPlugins{
	@Plugin(name = "Check sample-based Conformance using Incremental Conformance Checking", returnLabels = { "Incremental Sample-based Conformance Result" }, returnTypes = { IncrementalConformanceResult.class }, parameterLabels = {}, userAccessible = true)
	@UITopiaVariant(affiliation = "Humboldt-Universität zu Berlin", author = "Martin Kabierski", email = "martin.kabierski@hu-berlin.de", uiLabel = UITopiaVariant.USEPLUGIN)
	@PluginVariant(variantLabel = "Check sample-based Conformance using Incremental Conformance Checking", requiredParameterLabels = {0,1})
	public IncrementalConformanceResult CheckConformanceWithICCUI(final UIPluginContext context, PetrinetGraph net, XLog log) throws Exception {
		
		IncrementalConformanceCheckerParameterDialog parameterDialog = new IncrementalConformanceCheckerParameterDialog();
		final InteractionResult interactionResult = context.showConfiguration("Incremental Conformance Checking: Parameters", parameterDialog);
		if (interactionResult == InteractionResult.FINISHED ||interactionResult == InteractionResult.CONTINUE ||interactionResult == InteractionResult.NEXT) {
			
		}
		else return null;
		
		//retrieve parameters set by user
		double delta= parameterDialog.getDelta();
		double alpha= parameterDialog.getAlpha();
		double epsilon= parameterDialog.getEpsilon();
		double k= parameterDialog.getk();
		Goals goal= parameterDialog.getGoal();
		Heuristics approximationHeuristic = parameterDialog.getHeuristics();
		boolean useApproximation = parameterDialog.getApproximate();

		
		boolean checkInternalQuality = false;
		boolean checkExternalQuality = false;

		IccParameter iccParameters = new IccParameter(delta, alpha, epsilon, k, goal, useApproximation, approximationHeuristic, checkInternalQuality, checkExternalQuality);
		
		//Alignment Parameters
		IncrementalPNReplayerUI pnReplayerUI = new IncrementalPNReplayerUI();
		Object[] resultConfiguration = pnReplayerUI.getConfiguration(context, net, log);
		TransEvClassMapping mapping = (TransEvClassMapping) resultConfiguration[PNReplayerUI.MAPPING];
		
		XEventClassifier classifier = pnReplayerUI.classifier;
		System.out.println("Used Classifier: "+classifier.name());
		
		XLog copyLog = (XLog) log.clone();

		System.out.println(iccParameters);
		
		//QualityChecking Parameter
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
		
		//ResourceAssignments
		ResourceAssignment resourceAssignment = new ResourceAssignment();
		
		//run incremental conformance checking
		IncrementalTraceAnalyzer<?> analyzer = TraceAnalyzerFactory.createTraceAnalyzer(iccParameters, mapping, classifier, log, net, resourceAssignment);
		IncrementalConformanceResult result = checkConformanceWithICC(context, net, copyLog, analyzer, iccParameters, internalQualityCheckManager, externalQualityCheckManager);

		//TODO visualizer
		System.out.println(result.toString());
		System.out.println();
		return result;
	} 

	private IncrementalConformanceResult checkConformanceWithICC(UIPluginContext context, PetrinetGraph net, XLog log, 
			IncrementalTraceAnalyzer<?> analyzer, IccParameter iccParameters, QualityCheckManager internalQualityCheckManager, QualityCheckManager externalQualityCheckManager) throws AStarException, InterruptedException, ExecutionException {
		IncrementalConformanceChecker icc = new IncrementalConformanceChecker(analyzer, iccParameters);
		return icc.apply(context, log, net, IncrementalConformanceChecker.SamplingMode.BINOMIAL);
	}
}