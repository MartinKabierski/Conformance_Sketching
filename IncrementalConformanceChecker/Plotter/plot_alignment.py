import argparse
import sys
import pandas as pd
import matplotlib.pyplot as plt
import numpy as np
import re
from collections import defaultdict

def main(argv):
        parser = argparse.ArgumentParser()
        parser.add_argument("iccFile", help="the icc outpout file")
        parser.add_argument("origFile", help="the output file of the original replayer")
        args = parser.parse_args()

        #open files
        origResultsFile ="../results(alignment)/"+str(args.origFile)
        iccResultsFile="../results(alignment)/"+str(args.iccFile)

        #try to read in original file
        orig_results_df = pd.read_csv(origResultsFile, sep=';')
        print("###Original Results###")
        print(orig_results_df)
        print(list(orig_results_df.columns.values))
        print()

        #original asynchronous step distribution
        orig_avg_time=orig_results_df[" time"].mean()
        orig_avg_logSize=orig_results_df[" logSize"].mean()

        print("###Original Step Distribution###")

        result=get_orig_asynchMovesDict(orig_results_df)
        orig_asynchRelDict=result[0]
        #orig_asynchKeys=result[0]
        #orig_asynchValues=result[1]
        orig_sorted_by_value = sorted(orig_asynchRelDict.items(), key=lambda kv: kv[1], reverse=True)

        #origMeanValues=[orig_results_df["Fitness"].mean(),orig_results_df[" Traces"].mean(),orig_results_df[" Time"].mean()]

        #read in icc file
        icc_results_df = pd.read_csv(iccResultsFile, sep=';')
        print("###ICCResults###")
        print(icc_results_df)
        print(list(icc_results_df.columns.values))
        print()

        deltaValues=icc_results_df["delta"].unique()
        alphaValues=icc_results_df[" alpha"].unique()
        epsilonValues=icc_results_df[" epsilon"].unique()

        print("###used parameters for Evaluation###")
        print("Delta:    " + str(deltaValues))
        print("Alpha:    " + str(alphaValues))
        print("Epsilon:  " + str(epsilonValues))
        print()

        #find parameter setting that is closest to true distribution on average
        bestDelta=0
        bestAlpha=0
        bestEpsilon=0
        smallestMean=10.0
        for delta in deltaValues:
                for alpha in alphaValues:
                        for epsilon in epsilonValues:
                                deltaDf=icc_results_df['delta']==float(delta)
                                alphaDf=icc_results_df[' alpha']==float(alpha)
                                epsilonDf=icc_results_df[' epsilon']==float(epsilon)
                                parameter_df=icc_results_df[deltaDf & alphaDf & epsilonDf]
                                avg_distToOrig=parameter_df[' distToOriginal'].mean()
                                print("Delta: "+str(delta)+", Alpha: "+str(alpha)+", Epsilon: "+str(epsilon)+", Avg Dist to Orig: "+str(avg_distToOrig))
                                if avg_distToOrig<smallestMean:
                                        smallestMean=avg_distToOrig
                                        bestDelta=delta
                                        bestAlpha=alpha
                                        bestEpsilon=epsilon
        print()

        bestDelta=0.05
        bestAlpha=0.95
        bestEpsilon=0.05
        
        print("###Chosen parameters###")
        print("Delta:    " +str(bestDelta))
        print("Alpha:    " +str(bestAlpha))
        print("Epsilon:  " +str(bestEpsilon))
        print()

        bestDelta=0.05
        bestAlpha=0.95
        bestEpsilon=0.01

        #get df slice that contains the best parameter
        bestDeltaDf=icc_results_df['delta']==float(bestDelta)
        bestAlphaDf=icc_results_df[' alpha']==float(bestAlpha)
        bestEpsilonDf=icc_results_df[' epsilon']==float(bestEpsilon)

        bestParameterRuns=icc_results_df[bestDeltaDf & bestAlphaDf & bestEpsilonDf]

        print("###Results of best performing Runs###")
        print(bestParameterRuns)
        print()

        print("###Mean & Variance Values for chosen Runs###")
        print("Time, sampleSize, totalNoAsynchMoves")

        print( [bestParameterRuns[" time"].mean(), bestParameterRuns[" logSize"].mean(), bestParameterRuns[" totalNoAsynchMoves"].mean()])
        print( [bestParameterRuns[" time"].var(), bestParameterRuns[" logSize"].var(), bestParameterRuns[" totalNoAsynchMoves"].var()])
        print()

        for i in range(0,3):
                delta=bestDelta
                alpha=bestAlpha
                epsilon=bestEpsilon
                DeltaDf = icc_results_df['delta'] == float(bestDelta)
                AlphaDf = icc_results_df[' alpha'] == float(bestAlpha)
                EpsilonDf = icc_results_df[' epsilon'] == float(bestEpsilon)
                if i==0:
                        traces=[]
                        time=[]
                        distances=[]
                        asynchMovesDict=[]
                        asynchMovesKeys=[]
                        asynchMovesValues=[]
                        for currentDelta in deltaValues:
                                delta=currentDelta
                                print("Delta: "+str(delta))
                                DeltaDf = icc_results_df['delta'] == float(currentDelta)
                                currentDf = icc_results_df[DeltaDf & AlphaDf & EpsilonDf]
                                result=evaluate_df(currentDf)
                                traces.append(result[0])
                                time.append(result[1])
                                asynchMovesKeys.append(result[2])
                                #asynchMovesValues.append(result[3])
                                asynchMovesDict.append(result[4])
                                #print(asynchMovesDict)
                                
                                distances.append(result[5])

                        #order Values based on ordering of values in original result
                        for asynchMoveDict in asynchMovesDict:
                                asynchMoveValueList=[]
                                #print(orig_sorted_by_value)
                                #print(asynchMoveDict)
                                for key in orig_sorted_by_value:
                                        if key[0] in asynchMoveDict:
                                                asynchMoveValueList.append(asynchMoveDict[key[0]])
                                        if not key[0] in asynchMoveDict:
                                                asynchMoveValueList.append([0])
                                #print(asynchMoveValueList)
                                asynchMovesValues.append(asynchMoveValueList)
                                #print(orig_sorted_by_values)
                                #print(asynchMovesValues)
                        plt.figure(num=None, figsize=(20, 5), dpi=80, facecolor='w')
                        plt.subplots_adjust(wspace=0.2)

                        plt.subplot(1,3+len(deltaValues),1)
                        plt.boxplot(traces)
                        plt.axhline(orig_avg_logSize, color='b', linestyle='--')
                        plt.xlabel("Delta")
                        plt.ylabel("Sample Size")
                        plt.xticks(np.arange(1,len(deltaValues)+1) ,deltaValues)

                        plt.subplot(1, 3+len(deltaValues), 2)
                        plt.boxplot(time)
                        plt.axhline(orig_avg_time, color='b', linestyle='--')
                        plt.xlabel("Delta")
                        plt.ylabel("Time")
                        plt.xticks(np.arange(1,len(deltaValues)+1) ,deltaValues)

                        plt.subplot(1, 3+len(deltaValues), 3)
                        plt.boxplot(distances)
                        plt.xlabel("Delta")
                        plt.ylabel("Distance")
                        plt.xticks(np.arange(1,len(deltaValues)+1) ,deltaValues)

                        for i in range(4,4+len(deltaValues)):
                                plt.subplot(1,3+len(deltaValues), i)
                                plt.boxplot(asynchMovesValues[i-4])
                                orig_values=[0]+[x[1] for x in orig_sorted_by_value]
                                plt.plot(orig_values,'b.')
                                plt.xlabel("Activities for Delta="+str(deltaValues[3-i]))
                                plt.ylabel("rel. Frequency")
                        plt.show()

                        
                if i==1:
                        traces=[]
                        time=[]
                        distances=[]
                        asynchMovesDict=[]
                        asynchMovesKeys=[]
                        asynchMovesValues=[]
                        for currentAlpha in alphaValues:
                                alpha=currentAlpha
                                print("Alpha: "+str(alpha))
                                AlphaDf = icc_results_df[' alpha'] == float(currentAlpha)
                                currentDf = icc_results_df[DeltaDf & AlphaDf & EpsilonDf]
                                result=evaluate_df(currentDf)
                                traces.append(result[0])
                                time.append(result[1])
                                asynchMovesKeys.append(result[2])
                                #asynchMovesValues.append(result[3])
                                asynchMovesDict.append(result[4])
                                distances.append(result[5])

                        #order Values based on ordering of values in original result
                        for asynchMoveDict in asynchMovesDict:
                                asynchMoveValueList=[]
                                for key in orig_sorted_by_value:
                                        if key[0] in asynchMoveDict:
                                                asynchMoveValueList.append(asynchMoveDict[key[0]])
                                        if not key[0] in asynchMoveDict:
                                                asynchMoveValueList.append([0])
                                asynchMovesValues.append(asynchMoveValueList)


                        plt.figure(num=None, figsize=(20 , 5), dpi=80, facecolor='w')
                        plt.subplots_adjust(wspace=0.25)

                        plt.subplot(1,3+len(alphaValues),1)
                        plt.boxplot(traces)
                        plt.axhline(y=orig_avg_logSize, color='b', linestyle='--')
                        plt.xlabel("Alpha")
                        plt.ylabel("Sample Size")
                        plt.xticks(np.arange(1,len(alphaValues)+1) ,alphaValues)

                        plt.subplot(1, 3+len(alphaValues), 2)
                        plt.boxplot(time)
                        plt.axhline(y=orig_avg_time, color='b', linestyle='--')
                        plt.xlabel("Alpha")
                        plt.ylabel("Time")
                        plt.xticks(np.arange(1,len(alphaValues)+1) ,alphaValues)

                        plt.subplot(1, 3+len(alphaValues), 3)
                        plt.boxplot(distances)
                        plt.xlabel("Alpha")
                        plt.ylabel("Distance")
                        plt.xticks(np.arange(1,len(alphaValues)+1) ,alphaValues)

                        for i in range(4,4+len(deltaValues)):
                                plt.subplot(1,3+len(deltaValues), i)
                                plt.boxplot(asynchMovesValues[i-4])
                                orig_values=[0]+[x[1] for x in orig_sorted_by_value]
                                plt.plot(orig_values,'b.')
                                plt.xlabel("Activitiesfor Alpha="+str(alphaValues[3-i]))
                                plt.ylabel("rel. Frequency")
                        plt.show()

                if i==2:
                        traces=[]
                        time=[]
                        distances=[]
                        asynchMovesDict=[]
                        asynchMovesKeys=[]
                        asynchMovesValues=[]
                        for currentEpsilon in epsilonValues:
                                epsilon=currentEpsilon
                                print("Epsilon: "+str(epsilon))
                                EpsilonDf = icc_results_df[' epsilon'] == float(currentEpsilon)
                                currentDf = icc_results_df[DeltaDf & AlphaDf & EpsilonDf]
                                result=evaluate_df(currentDf)
                                traces.append(result[0])
                                time.append(result[1])
                                asynchMovesKeys.append(result[2])
                               # asynchMovesValues.append(result[3])
                                asynchMovesDict.append(result[4])
                                distances.append(result[5])

                        #order Values based on ordering of values in original result
                        for asynchMoveDict in asynchMovesDict:
                                asynchMoveValueList=[]
                                for key in orig_sorted_by_value:
                                        if key[0] in asynchMoveDict:
                                                asynchMoveValueList.append(asynchMoveDict[key[0]])
                                        if not key[0] in asynchMoveDict:
                                                asynchMoveValueList.append([0])
                                asynchMovesValues.append(asynchMoveValueList)

                                
                        plt.figure(num=None, figsize=(20 , 5), dpi=80, facecolor='w')
                        plt.subplots_adjust(wspace=0.25)

                        plt.subplot(1,3+len(epsilonValues),1)
                        plt.boxplot(traces)
                        plt.axhline(y=orig_avg_logSize, color='b', linestyle='--')
                        plt.xlabel("Epsilon")
                        plt.ylabel("Sample Size")
                        plt.xticks(np.arange(1,len(epsilonValues)+1) ,epsilonValues)

                        plt.subplot(1, 3+len(epsilonValues), 2)
                        plt.boxplot(time)
                        plt.axhline(y=orig_avg_time, color='b', linestyle='--')
                        plt.xlabel("Epsilon")
                        plt.ylabel("Time")
                        plt.xticks(np.arange(1,len(epsilonValues)+1) ,epsilonValues)

                        plt.subplot(1, 3+len(epsilonValues), 3)
                        plt.boxplot(distances)
                        plt.xlabel("Epsilon")
                        plt.ylabel("Distance")
                        plt.xticks(np.arange(1,len(epsilonValues)+1) ,epsilonValues)

                        for i in range(4,4+len(epsilonValues)):
                                plt.subplot(1,3+len(epsilonValues), i)
                                plt.boxplot(asynchMovesValues[i-4])
                                orig_values=[0]+[x[1] for x in orig_sorted_by_value]
                                plt.plot(orig_values,'b.')
                                plt.xlabel("Activities for Epsilon="+str(epsilonValues[3-i]))
                                plt.ylabel("rel. Frequency")
                        plt.show()


def evaluate_df(df):
        asynchMovesRelDict=defaultdict(list)
        asynchMovesRel=df[" asynchMovesRel"].values
        asynchMovesRelKeys=[]
        asynchMovesRelValues=[]
        for asynchmoves in asynchMovesRel:
                 matchObject = re.search("\{(.+)\}", asynchmoves, flags=0)
                 
                 #matchObject=matchObject[1:-1]
                 event= matchObject.group(1).split(",")
                 for keyValuePair in event:
                         #print(keyValuePair)
                         key,value=keyValuePair.split("=")
                         asynchMovesRelDict[key].append(float(value))
        #print(asynchMovesRel)
        for key in asynchMovesRelDict.keys():
                asynchMovesRelKeys.append(key)
                asynchMovesRelValues.append(asynchMovesRelDict[key])
        #asynchMovesRelKeys.extend(asynchMovesRelDict.keys())
        #asynchMovesRelValues.extend(asynchMovesRelDict.values())
        #print(asynchMovesRelKeys)
        #print(asynchMovesRelValues)
        #print(asynchMovesRelKeys)
        return [df[" logSize"].values, df[" time"].values, asynchMovesRelKeys, asynchMovesRelValues, asynchMovesRelDict, df[" distToOriginal"].values]

def get_orig_asynchMovesDict(df):
        asynchMovesRelDict=defaultdict(list)
        asynchMovesRel=df[" asynchMovesRel"].values
        asynchMovesRelKeys=[]
        asynchMovesRelValues=[]
        for asynchmoves in asynchMovesRel:
                 matchObject = re.search("\{(.+)\}", asynchmoves, flags=0)
                 event= matchObject.group(1).split(",")
                 for keyValuePair in event:
                         key,value=keyValuePair.split("=")
                         asynchMovesRelDict[key].append(float(value))
        for key in asynchMovesRelDict.keys():
                asynchMovesRelDict[key]=sum(asynchMovesRelDict[key]) / float(len(asynchMovesRelDict[key]))
        return [asynchMovesRelDict]

if __name__ == "__main__":
    sys.argv = [sys.argv[0], 'ICC_alignment_approx_FINAL', 'alignment_ORIG_FINAL']
    main(sys.argv)

