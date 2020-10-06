package qualitychecking;

import org.deckfour.xes.model.XTrace;

public interface AbstractValueDistribution {
	
	public void addTrace(XTrace trace);
		
	public boolean addTraceAndCheckPredicate(XTrace trace, Double epsilon);
	
	public double computeExternalStatistic(AbstractValueDistribution validationDistribution); 
	
	public double computeTotalDistance(AbstractValueDistribution validationDistribution);
	
	AbstractValueDistribution emptyCopy();
	
}
