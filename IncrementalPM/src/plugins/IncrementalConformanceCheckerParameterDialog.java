package plugins;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.processmining.framework.util.ui.widgets.ProMPropertiesPanel;

import com.fluxicon.slickerbox.factory.SlickerFactory;

import conformance.IncrementalConformanceChecker;
import conformance.IncrementalConformanceChecker.Goals;
import conformance.IncrementalConformanceChecker.Heuristics;
import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstants;

public class IncrementalConformanceCheckerParameterDialog extends ProMPropertiesPanel{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -5758657313361884927L;

	double delta;
	double alpha;
	double epsilon;
	double k;
	
	boolean approximateValue;
	Goals goalValue;
	Heuristics heuristicsValue;
	
	public IncrementalConformanceCheckerParameterDialog() {
		super("Incremental Conformance Checking - Parameters");
		
		delta = 0.05;
		alpha = 0.99;
		epsilon = 0.01;
		k = 0.2;
		approximateValue = false;
		goalValue = Goals.FITNESS;
		heuristicsValue = Heuristics.NONALIGNING_KNOWN;
		
		//initial config of parameter dialog
		//ProMPropertiesPanel panel = new ProMPropertiesPanel("Incremental Conformance Checking - Parameters");
		double mainPanelSize[][] = { {0.05, TableLayoutConstants.MINIMUM, 0.3, TableLayoutConstants.FILL},{30, 20, 30, 30, 30, 30, 30, 10, 30, 30, 30, 30}};
		this.setLayout(new TableLayout(mainPanelSize));
		SlickerFactory factory = SlickerFactory.instance();
		
		//sampling goal
		JLabel goalsHeader = factory.createLabel("Conformance Goal - What do you want to measure?");
		JComboBox goalsList = factory.createComboBox(IncrementalConformanceChecker.Goals.values());
		goalsList.setSelectedIndex(0);

		//sampling parameters delta, alpha, epsilon
		JLabel parameterHeader = factory.createLabel("<html><h3>Parameters</h3></html>");
		JLabel deltaLabel = factory.createLabel("Delta");
		JSlider deltaSlider = factory.createSlider(SwingConstants.HORIZONTAL);
		deltaSlider.setMinimum(0);
		deltaSlider.setMaximum(1000);
		deltaSlider.setValue((int) (delta * 1000));
		JLabel deltaValue = factory.createLabel(String.format("%.2f", deltaSlider.getValue()/1000.0));
			
		JLabel alphaLabel = factory.createLabel("Alpha");
		JSlider alphaSlider = factory.createSlider(SwingConstants.HORIZONTAL);
		alphaSlider.setMinimum(0);
		alphaSlider.setMaximum(1000);
		alphaSlider.setValue((int) (alpha * 1000));
		JLabel alphaValue = factory.createLabel(String.format("%.2f", alphaSlider.getValue()/1000.0));
		
		JLabel epsilonLabel = factory.createLabel("Epsilon");
		JSlider epsilonSlider = factory.createSlider(SwingConstants.HORIZONTAL);
		epsilonSlider.setMinimum(0);
		epsilonSlider.setMaximum(1000);
		epsilonSlider.setValue((int) (epsilon * 1000));
		JLabel epsilonValue = factory.createLabel(String.format("%.2f", epsilonSlider.getValue()/1000.0));

		//approximation parameters
		JLabel approximationHeader = factory.createLabel("<html><h3>Approximate Conformance Checking</h3></html>");
		JCheckBox approximate = factory.createCheckBox("Use Conformance Approximation?", approximateValue);
		JLabel approximationLabel = factory.createLabel("Which Approximation Heuristics to apply?");
		JComboBox approximationList = factory.createComboBox(IncrementalConformanceChecker.Heuristics.values());
		approximationList.setSelectedIndex(1);
		
		JLabel kLabel = factory.createLabel("k");
		JSlider kSlider = factory.createSlider(SwingConstants.HORIZONTAL);
		kSlider.setMinimum(0);
		kSlider.setMaximum(1000);
		kSlider.setValue((int) (k * 1000));
		JLabel kValue = factory.createLabel(String.format("%.2f", kSlider.getValue()/1000.0));
		// set approximation parameters to inivisible initially
		approximationLabel.setVisible(false);
		approximationList.setVisible(false);
		kSlider.setVisible(false);
		kLabel.setVisible(false);
		kValue.setVisible(false);
		
		//add all JComponents to the parameter dialog
		this.add(goalsHeader,"0,0,2,0");	
		this.add(goalsList,"1,1,2,1");
		
		this.add(parameterHeader,"0,3,3,3,l,t");
		this.add(deltaLabel,"1,4,1,4,l,b"); 	this.add(deltaSlider,"2,4,2,4,l,b"); 	this.add(deltaValue,"3,4,3,4,l,b"); 
		this.add(alphaLabel,"1,5,1,5,l,b"); 	this.add(alphaSlider,"2,5,2,5,l,b"); 	this.add(alphaValue,"3,5,3,5,l,b"); 
		this.add(epsilonLabel,"1,6,1,6,l,b"); 	this.add(epsilonSlider,"2,6,2,6,l,b"); this.add(epsilonValue,"3,6,3,6,l,b"); 
		
		this.add(approximationHeader,"0,8,3,8,l,t");
		this.add(approximate,"1,9,1,9,l,b");
		this.add(approximationLabel,"1,10,1,10"); 	this.add(approximationList,"2,10,2,10");
		this.add(kLabel,"1,11,1,11,l,b"); 		this.add(kSlider,"2,11,2,11,l,b");		this.add(kValue,"3,11,3,11,l,b");

		//register EventListeners
		deltaSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				deltaValue.setText(String.format("%.2f", deltaSlider.getValue() / 1000.0));
				delta = deltaSlider.getValue() / 1000.0;
			}
		});
		alphaSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				alphaValue.setText(String.format("%.2f", alphaSlider.getValue() / 1000.0));
				alpha = alphaSlider.getValue() / 1000.0;
			}
		});
		epsilonSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				epsilonValue.setText(String.format("%.2f", epsilonSlider.getValue() / 1000.0));
				epsilon = epsilonSlider.getValue() / 1000.0;
			}
		});		
		kSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				kValue.setText(String.format("%.2f", kSlider.getValue() / 1000.0));
				k = kSlider.getValue() / 1000.0;
			}
		});
		
		approximate.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				approximationLabel.setVisible(approximate.isSelected());
				approximationList.setVisible(approximate.isSelected());
				kSlider.setVisible(approximate.isSelected());
				kLabel.setVisible(approximate.isSelected());
				kValue.setVisible(approximate.isSelected());
				
				approximateValue = approximate.isSelected();
			}
		});
		
		approximationList.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				heuristicsValue = (Heuristics)approximationList.getSelectedItem();
			}
		});
		
		goalsList.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				goalValue = (Goals)goalsList.getSelectedItem();
			}
		});
	}
	
	public double getDelta() {
		return this.delta;
	}
	
	
	public double getAlpha() {
		return this.alpha;
	}
	
	
	public double getEpsilon() {
		return this.epsilon;
	}
	
	public double getk() {
		return this.k;
	}
	
	public boolean getApproximate() {
		return this.approximateValue;
	}
	
	public Goals getGoal() {
		return this.goalValue;
	}
	
	public Heuristics getHeuristics() {
		return this.heuristicsValue;
	}
}
