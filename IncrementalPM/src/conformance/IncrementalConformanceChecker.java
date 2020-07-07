package conformance;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import conformance.sampling.ThresholdCalculator;
import conformance.traceAnalysis.IncrementalTraceAnalyzer;
import conformance.traceAnalysis.TraceAnalysisTask;
import nl.tue.astar.AStarException;
import qualitychecking.QualityCheckManager;
import ressources.GlobalConformanceResult;
import ressources.IccParameter;
public class IncrementalConformanceChecker {
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
		
	IccParameter parameter;
	IncrementalTraceAnalyzer<?> calculator;
	Long seed;
	XLog sampledTraces = XFactoryRegistry.instance().currentDefault().createLog();
	
	public IncrementalConformanceChecker(IncrementalTraceAnalyzer<?> iccAlg, IccParameter iccParameters) {
		this.parameter = iccParameters;
		this.calculator = iccAlg;
		this.seed = System.currentTimeMillis();
	}
	
	public IncrementalConformanceChecker(IncrementalTraceAnalyzer<?> iccAlg, IccParameter iccParameters, long seed) {
		this.parameter = iccParameters;
		this.calculator = iccAlg;
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
	public GlobalConformanceResult apply(final UIPluginContext context, XLog log, PetrinetGraph net, TransEvClassMapping mapping) {
		//init
		int sampledCnt=0;
		int consecSamplesWithoutChangeCnt=0;
		Random randNrGenerator=new Random(this.seed);
		
		//get minimal sample size using binomial proportions interval
		int minimalSampleSize = ThresholdCalculator.calculateThreshold(this.parameter.getDelta(), this.parameter.getAlpha());
		System.out.println("Minimal Sample Size: "+minimalSampleSize);
		//sample trace from log
		XAttributeMap logAttributes=log.getAttributes();
		
		ThreadPoolExecutor executorService = (ThreadPoolExecutor)Executors.newFixedThreadPool(this.parameter.getNoThreads());
		ExecutorCompletionService<Pair<Boolean, XTrace>> CompletionService = new ExecutorCompletionService<Pair<Boolean, XTrace>>(executorService);
		
		//Initially add minimum number of traces+ no of threads.
		//the additional threads ensure, that thread pool is always full until the last task
		for(int i=0;i<minimalSampleSize+this.parameter.getNoThreads();i++) {
			XTrace currentTrace= sampleRandomTrace(log, randNrGenerator);
			if (parameter.storeSampledTraces()) {
				sampledTraces.add(currentTrace);
			}
			sampledCnt++;
			CompletionService.submit(new TraceAnalysisTask(currentTrace, this.calculator, this.parameter, net, logAttributes, mapping));
			if (parameter.isCheckExternalQuality()) {
				this.parameter.getExternalQualityCheckManager().addTraceToDistributions(currentTrace);
			}
		}

		while (true/*traces get removed in each iteration, so size will be 0 eventually*/) {
			try {
				//wait until a treace has been analyzed
				Future<Pair<Boolean, XTrace>> novelConformanceInformation = CompletionService.take();
				if(!novelConformanceInformation.get().getLeft()) {
					consecSamplesWithoutChangeCnt++;
					//System.out.println(consecSamplesWithoutChangeCnt);
				}
				else {
					//System.out.println(consecSamplesWithoutChangeCnt);
					//read tasks to thread pool, so that all tasks in pool may reach minimal sample size or sample whole log
					int newTasks=Math.min(log.size(),consecSamplesWithoutChangeCnt+1);
					for(int i=0;i<newTasks;i++){
						XTrace currentTrace= sampleRandomTrace(log, randNrGenerator);
						if (currentTrace==null) continue;
						if (parameter.storeSampledTraces()) {
							sampledTraces.add(currentTrace);
						}
						sampledCnt++;
						CompletionService.submit(new TraceAnalysisTask(currentTrace, this.calculator, this.parameter, net, logAttributes, mapping));
					}
					consecSamplesWithoutChangeCnt=0;
				}
			//if stopping criterion is reached or whole log is sampled, output result
			if (consecSamplesWithoutChangeCnt>=minimalSampleSize || log.size()==0) {
				boolean stopSampling=true;
				
				//TODO stop threads if external quality is checked
				//apply quality checking to assure that sample is not biased
				if (parameter.isCheckExternalQuality()) {
					int validationSize = sampledCnt;
					// TODO: make size of validation log a parameter
					// TODO: if validationSize >= size of remaining log, what should be done?
					if (validationSize < log.size()) {
						List<XTrace> validationSample = new ArrayList<XTrace>();
						for(int i=0;i<sampledCnt;i++) {
							validationSample.add(sampleRandomTrace(log, randNrGenerator));
						}
						//if sample seems to be biased, then do not stop sampling
						if (this.parameter.getExternalQualityCheckManager().hasSignificantDifference(
								validationSample, parameter.getAlpha())) {
							consecSamplesWithoutChangeCnt=0;
							this.parameter.getExternalQualityCheckManager().wasUsed();
							stopSampling=false;
							System.out.println("External");
							for(int i=0;i<minimalSampleSize+this.parameter.getNoThreads();i++) {
								XTrace currentTrace= sampleRandomTrace(log, randNrGenerator);
								sampledCnt++;
								CompletionService.submit(new TraceAnalysisTask(currentTrace, this.calculator, this.parameter, net, logAttributes, mapping));
								if (parameter.isCheckExternalQuality()) {
									this.parameter.getExternalQualityCheckManager().addTraceToDistributions(currentTrace);
								}
							}
						}
					}
				}
				if (stopSampling) {
					//stop remaining threads
					executorService.shutdown();
					return this.calculator.getAnalysisResult();
				}
			}
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * choose a trace at random from the log and remove it
	 * @param log
	 * @param randNrGenerator
	 * @return
	 */
	private XTrace sampleRandomTrace(XLog log, Random randNrGenerator) {
		if (log.size()==0) return null;
		int indexOfPickedTrace=randNrGenerator.nextInt(log.size());
		XTrace currentTrace = log.get(indexOfPickedTrace);
		log.remove(indexOfPickedTrace);
		return currentTrace;
	}
	
	
	public XLog getSampledTraces() {
		return sampledTraces;
	}
}

