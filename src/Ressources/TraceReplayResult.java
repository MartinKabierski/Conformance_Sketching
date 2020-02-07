package Ressources;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.deckfour.xes.model.XTrace;
import org.processmining.plugins.petrinet.replayresult.StepTypes;

import com.google.common.collect.Multiset;

public class TraceReplayResult {
	private String activities;
	private XTrace trace;
	int multiplicity;
	int length;
	
	boolean ininitSet;
	boolean recalculated;
	boolean approximated;
	
	private double rawFitness;
	double fitness;
	private Multiset<String> asynchMoves;
	private Map<String, Set<String>> violatingResources;
	private List<StepTypes> stepTypes;
	
	//maybe reference trace and it's costs

	public TraceReplayResult(String name, XTrace trace, int length, boolean inInitSet, boolean recalculated, boolean approximated, double rawFitness, double fitness, Multiset<String> asynchMoves) {
		this.setActivities(name);
		this.setTrace(trace);
		this.multiplicity=1;
		this.length=length;
		this.ininitSet=inInitSet;
		this.recalculated=recalculated;
		this.approximated=approximated;
		this.setRawFitness(rawFitness);
		this.fitness=fitness;
		this.setAsynchMoves(asynchMoves);
	}

	public String getActivities() {
		return activities;
	}

	public void setActivities(String activities) {
		this.activities = activities;
	}

	public XTrace getTrace() {
		return trace;
	}

	public void setTrace(XTrace trace) {
		this.trace = trace;
	}

	public double getRawFitness() {
		return rawFitness;
	}

	public void setRawFitness(double rawFitness) {
		this.rawFitness = rawFitness;
	}

	public Multiset<String> getAsynchMoves() {
		return asynchMoves;
	}

	public void setAsynchMoves(Multiset<String> asynchMoves) {
		this.asynchMoves = asynchMoves;
	}
	
	public void setViolatingResources(Map<String, Set<String>> violatingResources) {
		this.violatingResources = violatingResources;
	}
	
	public void setStepTypes(List<StepTypes> stepTypes) {
		this.stepTypes = stepTypes;
	}
	
	public List<StepTypes> getStepTypes() {
		return stepTypes;
	}
	
	public Map<String, Set<String>> getViolatingResources() {
		return violatingResources;
	}
	
}