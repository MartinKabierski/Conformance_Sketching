package conformance;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XLogImpl;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import conformance.sampling.ThresholdCalculator;
import conformance.traceAnalysis.IncrementalTraceAnalyzer;
import conformance.traceAnalysis.TraceAnalysisTask;
import nl.tue.astar.AStarException;
import ressources.GlobalConformanceResult;
import ressources.IccParameter;
public class IncrementalConformanceChecker {
	//TODO should all these go into the parameter?
	public enum Goals{
		FITNESS,
		DEVIATIONS,
		RESOURCES
	}
	
	public enum Heuristics{
		PREFIXSUFFIX,
		NONALIGNING_ALL,
		NONALIGNING_KNOWN
	}
	
	public enum SamplingMode{
		BINOMIAL,//sampling with replacement, i.e. i.i.d assumption holds
		HYPERGEOMETRIC//sampling without replacement, i.e. conditional probabilities
	}
		
	IccParameter parameter;
	IncrementalTraceAnalyzer<?> calculator;
	Long seed;
	Set<XTrace> currentlySampled;
	Set<XTrace> validationCurrentlySampled;
	
	//TODO ask Han what this is about
	XLog sampledTraces = XFactoryRegistry.instance().currentDefault().createLog();
	
	public IncrementalConformanceChecker(IncrementalTraceAnalyzer<?> iccAlg, IccParameter iccParameters) {
		this.parameter = iccParameters;
		this.calculator = iccAlg;
		this.currentlySampled = new HashSet<XTrace>();
		this.validationCurrentlySampled = new HashSet<XTrace>();
		this.seed = System.currentTimeMillis();
	}
	
	public IncrementalConformanceChecker(IncrementalTraceAnalyzer<?> iccAlg, IccParameter iccParameters, long seed) {
		this.parameter = iccParameters;
		this.calculator = iccAlg;
		this.currentlySampled = new HashSet<XTrace>();
		this.validationCurrentlySampled = new HashSet<XTrace>();
		this.seed = seed;
	}
	
	
	

	/**
	 * apply one run of the incremental conformance checking task
	 * @param context
	 * @param log
	 * @param net
	 * @return
	 * @throws AStarException
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 */
	//TODO add proper exception handling
	//TODO bring structure into this mess
	public GlobalConformanceResult apply(final UIPluginContext context, XLog log, PetrinetGraph net, SamplingMode samplingMode) {
		//init
		double fitnessAtInitialStop=0.0;
		boolean initialExternal = true;
		
		int sampledCnt=0;
		int consecSamplesWithoutChangeCnt=0;
		Random randNrGenerator=new Random(this.seed);
		XLog validationLog = new XLogImpl(log.getAttributes());
		//Random randNrGenerator=new Random();
		if (parameter.isCheckExternalQuality() && this.parameter.getExternalQualityCheckManager().getDistributions().size()>0) {
			validationLog=(XLog) log.clone();
		}
		//XLog deletionLog = new XLogImpl(log.getAttributes());
		//deletionLog=(XLog) log.clone();
		
		//get minimal sample size using binomial proportions interval
		int minimalSampleSize = ThresholdCalculator.calculateThreshold(this.parameter.getDelta(), this.parameter.getAlpha());
		System.out.println("Minimal Sample Size: "+minimalSampleSize);
		XAttributeMap logAttributes=log.getAttributes();
		
		ThreadPoolExecutor executorService = (ThreadPoolExecutor)Executors.newFixedThreadPool(this.parameter.getNoThreads());
		ExecutorCompletionService<Triple<Boolean, XTrace, Integer>> CompletionService = new ExecutorCompletionService<Triple<Boolean, XTrace, Integer>>(executorService);
		
		//Initially add minimum number of traces + no of threads (to ensure every thread is runnign until very end).
		int taskNo = Math.min(minimalSampleSize+this.parameter.getNoThreads(), log.size());
		for(int i=0;i<taskNo;i++) {
			Pair<XTrace, Integer> currentTrace= sampleRandomTrace(log, randNrGenerator, samplingMode);
			CompletionService.submit(new TraceAnalysisTask(currentTrace.getLeft(), currentTrace.getRight(), this.calculator, this.parameter, net, logAttributes));
		}
		
		List<XTrace> validationSample = new ArrayList<XTrace>();
		while (true/*traces get removed in each iteration, so size will be 0 eventually*/) {
			try {
				//wait until a trace has been analyzed
				Future<Triple<Boolean, XTrace, Integer>> novelConformanceInformation = CompletionService.take();
				//deletionLog.remove(novelConformanceInformation.get().getMiddle());
				//add new analysis task //TODO catch case where to traces are left
				Pair<XTrace, Integer> currentTrace= sampleRandomTrace(log, randNrGenerator, samplingMode);
				CompletionService.submit(new TraceAnalysisTask(currentTrace.getLeft(), currentTrace.getRight(), this.calculator, this.parameter, net, logAttributes));

				currentlySampled.add(novelConformanceInformation.get().getMiddle());
				sampledCnt++;
				taskNo--;
				if (parameter.storeSampledTraces()) {
					sampledTraces.add(novelConformanceInformation.get().getMiddle());
				}
				if (parameter.isCheckExternalQuality() && this.parameter.getExternalQualityCheckManager().getDistributions().size()>0) {
					this.parameter.getExternalQualityCheckManager().addTraceToDistributions(novelConformanceInformation.get().getMiddle());
					//validationsample may sample hypergeometrically, so that validation sample is closer to real distribution
					XTrace validationTrace = sampleRandomTrace(validationLog, randNrGenerator, samplingMode).getLeft();
					//validationLog.remove(validationTrace);
					////also remove validationCurrentlySampled in this setting
					//
					validationSample.add(validationTrace);
					if(IncrementalConformanceChecker.SamplingMode.BINOMIAL == samplingMode) {
						validationCurrentlySampled.add(validationTrace);
					}
				}
				
				if(!novelConformanceInformation.get().getLeft()) {
					consecSamplesWithoutChangeCnt++;
				}
				else {
					//System.out.println(sampledCnt);
					//read taken tasks back to thread pool
					int newTasks=Math.min(log.size(),consecSamplesWithoutChangeCnt+1);//ensure, that number of new tasks incorporates current analyzed Task
					if (IncrementalConformanceChecker.SamplingMode.BINOMIAL == samplingMode) {
						removeTraces(log, currentlySampled);
						currentlySampled.clear();
						//log = deletionLog;
						//deletionLog = new XLogImpl(log.getAttributes());
						//deletionLog = (XLog) log.clone();
					}
					//for(int i=0;i<newTasks;i++){
					//	Pair<XTrace, Integer> currentTrace= sampleRandomTrace(log, randNrGenerator, samplingMode);
					//	CompletionService.submit(new TraceAnalysisTask(currentTrace.getLeft(), currentTrace.getRight(), this.calculator, this.parameter, net, logAttributes));
					//}
					taskNo+=newTasks;
					consecSamplesWithoutChangeCnt=0;
					
				}
			if (consecSamplesWithoutChangeCnt>=minimalSampleSize || log.size()==0) {
				//apply quality checking to assure that sample is not biased
				if (parameter.isCheckExternalQuality() && this.parameter.getExternalQualityCheckManager().getDistributions().size()>0) {
						
					System.out.println("Number of sampled traces: "+sampledCnt);
					System.out.println("Validation Sample Size: "+validationSample.size());
					//if sample seems to be biased, then do not stop sampling
					if (this.parameter.getExternalQualityCheckManager().hasSignificantDifference(validationSample)) {
						//some fitness for evaluation
						if (initialExternal) {
							fitnessAtInitialStop = this.calculator.getAnalysisResult().getFitness();
							initialExternal=false;
						}
						
						consecSamplesWithoutChangeCnt=0;

						this.parameter.getExternalQualityCheckManager().wasUsed();
						if (IncrementalConformanceChecker.SamplingMode.BINOMIAL == samplingMode) {
							removeTraces(log, currentlySampled);
							currentlySampled.clear();
							
							removeTraces(validationLog, validationCurrentlySampled);
							validationCurrentlySampled.clear();
						}
						
						int newTasks=Math.min(log.size(),minimalSampleSize);//ensure, that number of new tasks incorporates current analyzed Task
						for(int i=0;i<newTasks;i++) {
						//	Pair<XTrace, Integer> curr= sampleRandomTrace(log, randNrGenerator, samplingMode);
						//	CompletionService.submit(new TraceAnalysisTask(curr.getLeft(), curr.getRight(), this.calculator, this.parameter, net, logAttributes));
						}
					}
				}
				if (consecSamplesWithoutChangeCnt>=minimalSampleSize || log.size()==0) {
					//System.out.println("Finished");
					//stop remaining threads
					executorService.shutdown();
					GlobalConformanceResult result = this.calculator.getAnalysisResult();
					result.setCnt(sampledCnt);
					result.fitnessAtFirst=fitnessAtInitialStop;
					return result;
				}
			}
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * choose a trace at random from the log and remove it, throws NullPointerException if log is empty
	 * @param log
	 * @param randNrGenerator
	 * @return
	 */	
	private Pair<XTrace, Integer> sampleRandomTrace(XLog log, Random randNrGenerator, IncrementalConformanceChecker.SamplingMode samplingMode) {
		int indexOfPickedTrace=randNrGenerator.nextInt(log.size());
		XTrace currentTrace = log.get(indexOfPickedTrace);
		
		//if i.i.d. sampling, we add trace, once we analyze its result
		if (samplingMode == IncrementalConformanceChecker.SamplingMode.BINOMIAL) {
		}
		//if biased sampling, delete trace right away
		else if (samplingMode == IncrementalConformanceChecker.SamplingMode.HYPERGEOMETRIC) {
			log.remove(currentTrace);
		}
		return new ImmutablePair<XTrace, Integer>(currentTrace, indexOfPickedTrace);
	}
	
	private void removeTraces(XLog log, Set<XTrace> traces) {
		log.removeAll(traces);
	}
	
	
	public XLog getSampledTraces() {
		return sampledTraces;
	}
}

