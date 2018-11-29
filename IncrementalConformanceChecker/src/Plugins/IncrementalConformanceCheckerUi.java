package Plugins;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.models.connections.petrinets.EvClassLogPetrinetConnection;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;
import org.processmining.plugins.petrinet.replayer.algorithms.IPNReplayParamProvider;
import org.processmining.plugins.petrinet.replayer.ui.PNReplayerUI;

public class IncrementalConformanceCheckerUi extends PNReplayerUI{

	public static final int MAPPING = 0;
	public static final int PARAMETERS = 1;
	//Algorithm will be AStarLargeLP
	
	// steps
	private int currentStep;
	private int nofSteps = 2; // algorithm and parameter

	// gui for each steps
	private JComponent[] replaySteps;

	// reference
	private IPNReplayParamProvider paramProvider = null;

	
	@Override
	public Object[] getConfiguration(UIPluginContext context, PetrinetGraph net, XLog log) {
		// init local parameter
		EvClassLogPetrinetConnection conn = null;
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
		return  new Object[] { mapping};
		//return showConfiguration(context, log, net, mapping);
	}

}
