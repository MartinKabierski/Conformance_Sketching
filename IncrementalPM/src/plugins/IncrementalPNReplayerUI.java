package plugins;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Map.Entry;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.deckfour.uitopia.api.event.TaskListener.InteractionResult;
import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClassifier;

import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.framework.connections.Connection;
import org.processmining.framework.connections.ConnectionCannotBeObtained;
import org.processmining.framework.connections.annotations.ConnectionObjectFactory;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.PluginExecutionResult;
import org.processmining.framework.plugin.PluginParameterBinding;
import org.processmining.framework.util.Pair;
import org.processmining.models.connections.petrinets.EvClassLogPetrinetConnection;
import org.processmining.models.connections.petrinets.behavioral.FinalMarkingConnection;
import org.processmining.models.connections.petrinets.behavioral.InitialMarkingConnection;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;
import org.processmining.plugins.petrinet.replayer.algorithms.IPNReplayParamProvider;
import org.processmining.plugins.petrinet.replayer.ui.PNAlgorithmStep;
import org.processmining.plugins.petrinet.replayer.ui.PNReplayerUI;

/**
 * a hacky copy of the original PNReplayerUI, that allows the retrieval of the used XEventClassifier during the Mapping process
 * @author martin
 *
 */
public class IncrementalPNReplayerUI extends PNReplayerUI{
	public static final int MAPPING = 0;
	public static final int ALGORITHM = 1;
	public static final int PARAMETERS = 2;

	// steps
	private int currentStep;
	private int nofSteps = 2; // algorithm and parameter

	// gui for each steps
	private JComponent[] replaySteps;

	// reference
	private IPNReplayParamProvider paramProvider = null;
	XEventClassifier classifier = null;
	
	
	public Object[] getConfiguration(UIPluginContext context, PetrinetGraph net, XLog log) {
		// init local parameter
		EvClassLogPetrinetConnection conn = null;

		// check existence of initial marking
		try {
			InitialMarkingConnection initCon = context.getConnectionManager().getFirstConnection(
					InitialMarkingConnection.class, context, net);

			if (((Marking) initCon.getObjectWithRole(InitialMarkingConnection.MARKING)).isEmpty()) {
				JOptionPane
						.showMessageDialog(
								new JPanel(),
								"The initial marking is an empty marking. If this is not intended, remove the currently existing InitialMarkingConnection object and then use \"Create Initial Marking\" plugin to create a non-empty initial marking.",
								"Empty Initial Marking", JOptionPane.INFORMATION_MESSAGE);
			}
		} catch (ConnectionCannotBeObtained exc) {
			if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(new JPanel(),
					"No initial marking is found for this model. Do you want to create one?", "No Initial Marking",
					JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE)) {
				createMarking(context, net, InitialMarkingConnection.class);
			}
			;
		} catch (Exception e) {
			e.printStackTrace();
		}

		// check existence of final marking
		try {
			context.getConnectionManager().getFirstConnection(FinalMarkingConnection.class, context, net);
		} catch (ConnectionCannotBeObtained exc) {
			if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(new JPanel(),
					"No final marking is found for this model. Do you want to create one?", "No Final Marking",
					JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE)) {
				createMarking(context, net, FinalMarkingConnection.class);
			}
			;
		} catch (Exception e) {
			e.printStackTrace();
		}

		// check connection in order to determine whether mapping step is needed
		// of not
		try {
			// connection is found, no need for mapping step
			// connection is not found, another plugin to create such connection
			// is automatically
			// executed
			conn = context.getConnectionManager().getFirstConnection(EvClassLogPetrinetConnection.class, context, net,
					log);
		} catch (Exception e) {
			JOptionPane.showMessageDialog(new JPanel(), "No mapping can be constructed between the net and the log");
			return null;
		}

		// init gui for each step
		TransEvClassMapping mapping = (TransEvClassMapping) conn
				.getObjectWithRole(EvClassLogPetrinetConnection.TRANS2EVCLASSMAPPING);

		classifier = conn.getObjectWithRole(EvClassLogPetrinetConnection.EVENTCLASSIFIER);
		
		// check invisible transitions
		Set<Transition> unmappedTrans = new HashSet<Transition>();
		for (Entry<Transition, XEventClass> entry : mapping.entrySet()) {
			if (entry.getValue().equals(mapping.getDummyEventClass())) {
				if (!entry.getKey().isInvisible()) {
					unmappedTrans.add(entry.getKey());
				}
			}
		}
		if (!unmappedTrans.isEmpty()) {
			JList list = new JList(unmappedTrans.toArray());
			JPanel panel = new JPanel();
			BoxLayout layout = new BoxLayout(panel, BoxLayout.Y_AXIS);
			panel.setLayout(layout);
			panel.add(new JLabel("The following transitions are not mapped to any event class:"));

			JScrollPane sp = new JScrollPane(list);
			panel.add(sp);
			panel.add(new JLabel("Do you want to consider these transitions as invisible (unlogged activities)?"));

			Object[] options = { "Yes, set them to invisible", "No, keep them as they are" };

			if (0 == JOptionPane.showOptionDialog(null, panel, "Configure transition visibility",
					JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0])) {
				for (Transition t : unmappedTrans) {
					t.setInvisible(true);
				}
			}
		}

		replaySteps = new JComponent[nofSteps];
		replaySteps[0] = new PNAlgorithmStep(context, net, log, mapping);

		// set current step
		currentStep = 0;

		return showConfiguration(context, log, net, mapping);
	}
	
	private boolean createMarking(UIPluginContext context, PetrinetGraph net, Class<? extends Connection> classType) {
		boolean result = false;
		Collection<Pair<Integer, PluginParameterBinding>> plugins = context.getPluginManager().find(
				ConnectionObjectFactory.class, classType, context.getClass(), true, false, false, net.getClass());
		PluginContext c2 = context.createChildContext("Creating connection of Type " + classType);
		Pair<Integer, PluginParameterBinding> pair = plugins.iterator().next();
		PluginParameterBinding binding = pair.getSecond();
		try {
			PluginExecutionResult pluginResult = binding.invoke(c2, net);
			pluginResult.synchronize();
			context.getProvidedObjectManager().createProvidedObjects(c2); // push the objects to main context
			result = true;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			c2.getParentContext().deleteChild(c2);
		}
		return result;
	}

	private Object[] showConfiguration(UIPluginContext context, XLog log, PetrinetGraph net, TransEvClassMapping mapping) {
		// init result variable
		InteractionResult result = InteractionResult.NEXT;

		// configure interaction with user
		while (true) {
			if (currentStep < 0) {
				currentStep = 0;
			}
			if (currentStep >= nofSteps) {
				currentStep = nofSteps - 1;
			}

			result = context.showWizard("Replay in Petri net", currentStep == 0, currentStep == nofSteps - 1,
					replaySteps[currentStep]);
			switch (result) {
				case NEXT :
					go(1, context, net, log, mapping);
					break;
				case PREV :
					go(-1, context, net, log, mapping);
					break;
				case FINISHED :
					return new Object[] { mapping, ((PNAlgorithmStep) replaySteps[0]).getAlgorithm(),
							paramProvider.constructReplayParameter(replaySteps[1])};
				default :
					return new Object[] { null };
			}
		}
	}

	/**
	 * go to next step in replaying. petri net and connection to log can be used
	 * as necessary by the implemented GUI
	 * 
	 * @param direction
	 * @param isTestingMode
	 * @param net
	 * @param conn
	 * @return
	 */
	private int go(int direction, UIPluginContext context, PetrinetGraph net, XLog log, TransEvClassMapping mapping) {
		currentStep += direction;

		// check which algorithm is selected and adjust parameter as necessary
		if (currentStep == 1) {
			this.paramProvider = ((PNAlgorithmStep) replaySteps[0]).getAlgorithm().constructParamProvider(context, net,
					log, mapping);
			replaySteps[1] = paramProvider.constructUI();
		}

		if ((currentStep >= 0) && (currentStep < nofSteps)) {
			return currentStep;
		}
		return 0;
	}
}
