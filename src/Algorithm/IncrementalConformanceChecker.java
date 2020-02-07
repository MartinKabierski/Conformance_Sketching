package Algorithm;

import java.util.List;
import java.util.Random;

import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;

import Replayer.IncrementalReplayer;
import Ressources.IccParameters;
import Ressources.IccResult;
import Ressources.ReplayResultsContainer;
import Utils.ReplayerFactory;
import Utils.ThresholdCalculator;
//TODO function returns ICCResult. Should return AlignmentReplayResult???
public class IncrementalConformanceChecker {
	
	ReplayResultsContainer alignmentContainer;
	ReplayerFactory replayerFactory;
	IccParameters iccParameters;
	IncrementalReplayer replayer;
	
	public IncrementalConformanceChecker(final UIPluginContext context, IncrementalReplayer replayer, IccParameters iccParameters, XLog completeLog, PetrinetGraph net) {
		this.alignmentContainer = new ReplayResultsContainer();
		this.iccParameters = iccParameters;
		this.replayer = replayer;		
		System.out.println("Parameters set to :\n"+this.iccParameters.toString());
	}


	public IccResult apply(final UIPluginContext context, XLog log, PetrinetGraph net, TransEvClassMapping mapping) {
		//ConvertPetriNetToAcceptingPetriNetPlugin transformer = new ConvertPetriNetToAcceptingPetriNetPlugin();
		//AcceptingPetriNet newNet=transformer.runUI(context, (Petrinet) net);
		
		int pickedTraceCount=0;
		int consecSamplesWithoutChange=0;
		long startingTimeInMillis=System.currentTimeMillis();
		Random randNrGenerator=new Random();
		int minimalSampleSize= calculateMinimalSampleSize(iccParameters);
		System.out.println(minimalSampleSize);
		//TODO build log from log, not insert trace into empty log
		XAttributeMap logAttributes=log.getAttributes();

		
		for (int i=0;i<log.size();/*traces get removed in each iteration, so size will be 0 eventually*/){
			XTrace currentTrace= sampleNextTrace(log, randNrGenerator);
			pickedTraceCount++;
	
			if (pickedTraceCount % 500 == 0) {
				System.out.println(pickedTraceCount + " traces done");
			}
			
			boolean containsInformation=false;
			if (this.replayer.TraceVariantKnown(currentTrace)){
				containsInformation=this.replayer.incrementAndCheckPredicate(currentTrace);
			}
			else {
				Object[] additionalInformation= {net, logAttributes, mapping};
				containsInformation=this.replayer.abstractAndCheckPredicate(currentTrace, additionalInformation);
			}
						
			// update attributes for external quality checking
			if (iccParameters.isCheckExternalQuality()) {
				iccParameters.getExternalQualityCheckContainer().addTraceToDistributions(currentTrace);
			}
			
			//updateExperimentCounter
			if(!containsInformation) {
				consecSamplesWithoutChange++;
			}
			else consecSamplesWithoutChange=0;			
			//System.out.println(consecSamplesWithoutChange);
			if (consecSamplesWithoutChange>=minimalSampleSize) {
				
				if (iccParameters.isCheckExternalQuality()) {
					// perform external sample quality assessment
					int validationStart = 1;
					int validationEnd = validationStart + pickedTraceCount;
					if (validationEnd < log.size()) {
						// TODO: actually should pick traces at random, right?
						// TODO: make size of validation log a parameter
						List<XTrace> validationSample = log.subList(validationStart, validationEnd);
						if (iccParameters.getExternalQualityCheckContainer().hasSignificantDifference(
								validationSample, iccParameters.getAlpha())) {
							consecSamplesWithoutChange=0;
							iccParameters.getExternalQualityCheckContainer().wasUsed();
						}
					}
				}
				long endingTimeInMillis=System.currentTimeMillis();
				long totalTime=endingTimeInMillis-startingTimeInMillis;
				if(iccParameters.getGoal().equals("fitness")) {
					ReplayResultsContainer sample= this.replayer.getResult();
					double fitness= sample.getFitness();
					return new IccResult(pickedTraceCount,totalTime,fitness, sample);
				}
				else {
					ReplayResultsContainer sample=this.replayer.getResult();
					return new IccResult(pickedTraceCount,totalTime,-1, sample);
				}
				
			}
		}
		long endingTimeInMillis=System.currentTimeMillis();
		long totalTime=endingTimeInMillis-startingTimeInMillis;
		if(iccParameters.getGoal().equals("fitness")) {
			ReplayResultsContainer sample=this.replayer.getResult();
			double fitness= sample.getFitness();
			return new IccResult(pickedTraceCount,totalTime,fitness, sample);
		}
		else {
			ReplayResultsContainer sample=this.replayer.getResult();
			return new IccResult(pickedTraceCount, totalTime,-1, sample);
		}
	}
	
	//TODO make thresholdCalculator static
	private int calculateMinimalSampleSize(IccParameters iccParameters) {
		ThresholdCalculator calculator=new ThresholdCalculator(this.iccParameters.getDelta(), this.iccParameters.getAlpha());
		return calculator.calculateThreshold();
	}
	
	private XTrace sampleNextTrace(XLog log, Random randNrGenerator) {
		int indexOfPickedTrace=randNrGenerator.nextInt(log.size());
		XTrace currentTrace = log.get(indexOfPickedTrace);
		log.remove(indexOfPickedTrace);
		return currentTrace;
	}
}

