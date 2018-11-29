import argparse
import sys
import pandas as pd
import matplotlib.pyplot as plt
import numpy as np
import re
from collections import defaultdict
from math import log10

def main():
        #build a dataframe for each result file
        #original
        bpi2012_orig_df=pd.read_csv("../final/bpi2012_orig", sep=';')
        bpi2014_orig_df=pd.read_csv("../final/bpi2014_orig", sep=';')
        road_traffic_orig_df=pd.read_csv("../final/road_traffic_orig", sep=';')

        #fitness
        bpi2012_fitness_df=pd.read_csv("../final/bpi2012_fitness", sep=';')
        bpi2014_fitness_df=pd.read_csv("../final/bpi2014_fitness", sep=';')
        road_traffic_fitness_df=pd.read_csv("../final/road_traffic_fitness", sep=';')

        #fitness_approximated
        bpi2012_fitness_approx_df=pd.read_csv("../final/bpi2012_fitness_approx", sep=';')
        bpi2014_fitness_approx_df=pd.read_csv("../final/bpi2014_fitness_approx", sep=';')
        road_traffic_fitness_approx_df=pd.read_csv("../final/road_traffic_fitness_approx", sep=';')

        #alignment
        bpi2012_alignment_df=pd.read_csv("../final/bpi2012_alignment", sep=';')
        bpi2014_alignment_df=pd.read_csv("../final/bpi2014_alignment", sep=';')
        road_traffic_alignment_df=pd.read_csv("../final/road_traffic_alignment", sep=';')

        #alignment_approximated
        bpi2012_alignment_approx_df=pd.read_csv("../final/bpi2012_alignment_approx", sep=';')
        bpi2014_alignment_approx_df=pd.read_csv("../final/bpi2014_alignment_approx", sep=';')
        road_traffic_alignment_approx_df=pd.read_csv("../final/road_traffic_alignment_approx", sep=';')

        print("Loaded input files")
        print()

        #set parameters for original-to-implementation comparison for all three logs
        chosen_delta=0.01
        chosen_alpha=0.99
        chosen_epsilon=0.01

        print("###used parameters for Evaluation###")
        print("Delta:    " + str(chosen_delta))
        print("Alpha:    " + str(chosen_alpha))
        print("Epsilon:  " + str(chosen_epsilon))
        print()     

        #long copy and paste list that creates the 12 dataframes for the plots
        #2012#

        bpi2012_fitness_delta_df=bpi2012_fitness_df["delta"]==float(chosen_delta)
        bpi2012_fitness_alpha_df=bpi2012_fitness_df[" alpha"]==float(chosen_alpha)
        bpi2012_fitness_epsilon_df=bpi2012_fitness_df[" epsilon"]==float(chosen_epsilon)
        bpi2012_fitness_plot_df=bpi2012_fitness_df[bpi2012_fitness_delta_df & bpi2012_fitness_alpha_df & bpi2012_fitness_epsilon_df]

        bpi2012_fitness_approx_delta_df=bpi2012_fitness_approx_df["delta"]==float(chosen_delta)
        bpi2012_fitness_approx_alpha_df=bpi2012_fitness_approx_df[" alpha"]==float(chosen_alpha)
        bpi2012_fitness_approx_epsilon_df=bpi2012_fitness_approx_df[" epsilon"]==float(chosen_epsilon)
        bpi2012_fitness_approx_plot_df=bpi2012_fitness_approx_df[bpi2012_fitness_approx_delta_df & bpi2012_fitness_approx_alpha_df & bpi2012_fitness_approx_epsilon_df]

        bpi2012_alignment_delta_df=bpi2012_alignment_df["delta"]==float(chosen_delta)
        bpi2012_alignment_alpha_df=bpi2012_alignment_df[" alpha"]==float(chosen_alpha)
        bpi2012_alignment_epsilon_df=bpi2012_alignment_df[" epsilon"]==float(chosen_epsilon)
        bpi2012_alignment_plot_df=bpi2012_alignment_df[bpi2012_alignment_delta_df & bpi2012_alignment_alpha_df & bpi2012_alignment_epsilon_df]

        bpi2012_alignment_approx_delta_df=bpi2012_alignment_approx_df["delta"]==float(chosen_delta)
        bpi2012_alignment_approx_alpha_df=bpi2012_alignment_approx_df[" alpha"]==float(chosen_alpha)
        bpi2012_alignment_approx_epsilon_df=bpi2012_alignment_approx_df[" epsilon"]==float(chosen_epsilon)
        bpi2012_alignment_approx_plot_df=bpi2012_alignment_approx_df[bpi2012_alignment_approx_delta_df & bpi2012_alignment_approx_alpha_df & bpi2012_alignment_approx_epsilon_df]

        #2014#

        bpi2014_fitness_delta_df=bpi2014_fitness_df["delta"]==float(chosen_delta)
        bpi2014_fitness_alpha_df=bpi2014_fitness_df[" alpha"]==float(chosen_alpha)
        bpi2014_fitness_epsilon_df=bpi2014_fitness_df[" epsilon"]==float(chosen_epsilon)
        bpi2014_fitness_plot_df=bpi2014_fitness_df[bpi2014_fitness_delta_df &bpi2014_fitness_alpha_df & bpi2014_fitness_epsilon_df]

        bpi2014_fitness_approx_delta_df=bpi2014_fitness_approx_df["delta"]==float(chosen_delta)
        bpi2014_fitness_approx_alpha_df=bpi2014_fitness_approx_df[" alpha"]==float(chosen_alpha)
        bpi2014_fitness_approx_epsilon_df=bpi2014_fitness_approx_df[" epsilon"]==float(chosen_epsilon)
        bpi2014_fitness_approx_plot_df=bpi2014_fitness_approx_df[bpi2014_fitness_approx_delta_df & bpi2014_fitness_approx_alpha_df & bpi2014_fitness_approx_epsilon_df]

        bpi2014_alignment_delta_df=bpi2014_alignment_df["delta"]==float(chosen_delta)
        bpi2014_alignment_alpha_df=bpi2014_alignment_df[" alpha"]==float(chosen_alpha)
        bpi2014_alignment_epsilon_df=bpi2014_alignment_df[" epsilon"]==float(chosen_epsilon)
        bpi2014_alignment_plot_df=bpi2014_alignment_df[bpi2014_alignment_delta_df &bpi2014_alignment_alpha_df & bpi2014_alignment_epsilon_df]

        bpi2014_alignment_approx_delta_df=bpi2014_alignment_approx_df["delta"]==float(chosen_delta)
        bpi2014_alignment_approx_alpha_df=bpi2014_alignment_approx_df[" alpha"]==float(chosen_alpha)
        bpi2014_alignment_approx_epsilon_df=bpi2014_alignment_approx_df[" epsilon"]==float(chosen_epsilon)
        bpi2014_alignment_approx_plot_df=bpi2014_alignment_approx_df[bpi2014_alignment_approx_delta_df & bpi2014_alignment_approx_alpha_df & bpi2014_alignment_approx_epsilon_df]

        #road traffic#

        road_traffic_fitness_delta_df=road_traffic_fitness_df["delta"]==float(chosen_delta)
        road_traffic_fitness_alpha_df=road_traffic_fitness_df[" alpha"]==float(chosen_alpha)
        road_traffic_fitness_epsilon_df=road_traffic_fitness_df[" epsilon"]==float(chosen_epsilon)
        road_traffic_fitness_plot_df=road_traffic_fitness_df[road_traffic_fitness_delta_df & road_traffic_fitness_alpha_df & road_traffic_fitness_epsilon_df]

        road_traffic_fitness_approx_delta_df=road_traffic_fitness_approx_df["delta"]==float(chosen_delta)
        road_traffic_fitness_approx_alpha_df=road_traffic_fitness_approx_df[" alpha"]==float(chosen_alpha)
        road_traffic_fitness_approx_epsilon_df=road_traffic_fitness_approx_df[" epsilon"]==float(chosen_epsilon)
        road_traffic_fitness_approx_plot_df=road_traffic_fitness_approx_df[road_traffic_fitness_approx_delta_df & road_traffic_fitness_approx_alpha_df & road_traffic_fitness_approx_epsilon_df]

        road_traffic_alignment_delta_df=road_traffic_alignment_df["delta"]==float(chosen_delta)
        road_traffic_alignment_alpha_df=road_traffic_alignment_df[" alpha"]==float(chosen_alpha)
        road_traffic_alignment_epsilon_df=road_traffic_alignment_df[" epsilon"]==float(chosen_epsilon)
        road_traffic_alignment_plot_df=road_traffic_alignment_df[road_traffic_alignment_delta_df & road_traffic_alignment_alpha_df & road_traffic_alignment_epsilon_df]

        road_traffic_alignment_approx_delta_df=road_traffic_alignment_approx_df["delta"]==float(chosen_delta)
        road_traffic_alignment_approx_alpha_df=road_traffic_alignment_approx_df[" alpha"]==float(chosen_alpha)
        road_traffic_alignment_approx_epsilon_df=road_traffic_alignment_approx_df[" epsilon"]==float(chosen_epsilon)
        road_traffic_alignment_approx_plot_df=road_traffic_alignment_approx_df[road_traffic_alignment_approx_delta_df & road_traffic_alignment_approx_alpha_df & road_traffic_alignment_approx_epsilon_df]

        #plot computing time comparisons

        bpi2012_orig_mean=bpi2012_orig_df[" time"].mean()
        bpi2012_list=[]
        bpi2012_list.append(bpi2014_fitness_plot_df[" time"].values)
        bpi2012_list.append(bpi2014_fitness_approx_plot_df[" time"].values)
        bpi2012_list.append(bpi2014_alignment_plot_df[" time"].values)
        bpi2012_list.append(bpi2014_alignment_approx_plot_df[" time"].values)

        bpi2014_orig_mean=bpi2014_orig_df[" time"].mean()
        bpi2014_list=[]
        bpi2014_list.append(bpi2014_fitness_plot_df[" time"].values)
        bpi2014_list.append(bpi2014_fitness_approx_plot_df[" time"].values)
        bpi2014_list.append(bpi2014_alignment_plot_df[" time"].values)
        bpi2014_list.append(bpi2014_alignment_approx_plot_df[" time"].values)

        road_traffic_orig_mean=road_traffic_orig_df[" time"].mean()
        road_traffic_list=[]
        road_traffic_list.append(road_traffic_fitness_plot_df[" time"].values)
        road_traffic_list.append(road_traffic_fitness_approx_plot_df[" time"].values)
        road_traffic_list.append(road_traffic_alignment_plot_df[" time"].values)
        road_traffic_list.append(road_traffic_alignment_approx_plot_df[" time"].values)

        #plot time
        plt.figure(num=None, figsize=(20, 5), dpi=80, facecolor='w')
        plt.subplots_adjust(wspace=0.2)

        plt.subplot(1, 3, 1)
        plt.boxplot(convert_to_log(bpi2012_list))
        plt.axhline(log10(bpi2012_orig_mean), color='b', linestyle='--')

        plt.subplot(1, 3, 2)
        plt.boxplot(convert_to_log(bpi2014_list))
        plt.axhline(log10(bpi2014_orig_mean), color='b', linestyle='--')

        plt.subplot(1, 3, 3)
        plt.boxplot(convert_to_log(road_traffic_list))
        plt.axhline(log10(road_traffic_orig_mean), color='b', linestyle='--')

        plt.show()



        #plot trace comparisons

        bpi2012_orig_mean=bpi2012_orig_df[" logSize"].mean()
        bpi2012_list=[]
        bpi2012_list.append(bpi2012_fitness_plot_df[" logSize"].values)
        bpi2012_list.append(bpi2012_fitness_approx_plot_df[" logSize"].values)
        bpi2012_list.append(bpi2012_alignment_plot_df[" logSize"].values)
        bpi2012_list.append(bpi2012_alignment_approx_plot_df[" logSize"].values)

        bpi2014_orig_mean=bpi2014_orig_df[" logSize"].mean()
        bpi2014_list=[]
        bpi2014_list.append(bpi2014_fitness_plot_df[" logSize"].values)
        bpi2014_list.append(bpi2014_fitness_approx_plot_df[" logSize"].values)
        bpi2014_list.append(bpi2014_alignment_plot_df[" logSize"].values)
        bpi2014_list.append(bpi2014_alignment_approx_plot_df[" logSize"].values)

        road_traffic_orig_mean=road_traffic_orig_df[" logSize"].mean()
        road_traffic_list=[]
        road_traffic_list.append(road_traffic_fitness_plot_df[" logSize"].values)
        road_traffic_list.append(road_traffic_fitness_approx_plot_df[" logSize"].values)
        road_traffic_list.append(road_traffic_alignment_plot_df[" logSize"].values)
        road_traffic_list.append(road_traffic_alignment_approx_plot_df[" logSize"].values)

        plt.figure(num=None, figsize=(20, 5), dpi=80, facecolor='w')
        plt.subplots_adjust(wspace=0.2)

        plt.subplot(1, 3, 1)
        plt.boxplot(convert_to_log(bpi2012_list))
        plt.axhline(log10(bpi2012_orig_mean), color='b', linestyle='--')

        plt.subplot(1, 3, 2)
        plt.boxplot(convert_to_log(bpi2014_list))
        plt.axhline(log10(bpi2014_orig_mean), color='b', linestyle='--')

        plt.subplot(1, 3, 3)
        plt.boxplot(convert_to_log(road_traffic_list))
        plt.axhline(log10(road_traffic_orig_mean), color='b', linestyle='--')

        plt.show()


        #plot fitness comparisons

        bpi2012_orig_mean=bpi2012_orig_df["fitness"].mean()
        bpi2012_list=[]
        bpi2012_list.append(bpi2012_fitness_plot_df[" fitness"].values)
        bpi2012_list.append(bpi2012_fitness_approx_plot_df[" fitness"].values)
        #bpi2012_list.append(bpi2012_alignment_plot_df[" fitness"].values)
        #bpi2012_list.append(bpi2012_alignment_approx_plot_df[" fitness"].values)

        bpi2014_orig_mean=bpi2014_orig_df["fitness"].mean()
        bpi2014_list=[]
        bpi2014_list.append(bpi2014_fitness_plot_df[" fitness"].values)
        bpi2014_list.append(bpi2014_fitness_approx_plot_df[" fitness"].values)
        #bpi2014_list.append(bpi2014_alignment_plot_df[" fitness"].values)
        #bpi2014_list.append(bpi2014_alignment_approx_plot_df[" fitness"].values)

        road_traffic_orig_mean=road_traffic_orig_df["fitness"].mean()
        road_traffic_list=[]
        road_traffic_list.append(road_traffic_fitness_plot_df[" fitness"].values)
        road_traffic_list.append(road_traffic_fitness_approx_plot_df[" fitness"].values)
        #road_traffic_list.append(road_traffic_alignment_plot_df[" fitness"].values)
        #road_traffic_list.append(road_traffic_alignment_approx_plot_df[" fitness"].values)

        plt.figure(num=None, figsize=(20, 5), dpi=80, facecolor='w')
        plt.subplots_adjust(wspace=0.2)

        plt.subplot(1, 3, 1)
        plt.boxplot(bpi2012_list)
        plt.axhline(bpi2012_orig_mean, color='b', linestyle='--')

        plt.subplot(1, 3, 2)
        plt.boxplot(bpi2014_list)
        plt.axhline(bpi2014_orig_mean, color='b', linestyle='--')

        plt.subplot(1, 3, 3)
        plt.boxplot(road_traffic_list)
        plt.axhline(road_traffic_orig_mean, color='b', linestyle='--')

        plt.show()

def convert_to_log(input):
        to_return=[]
        for x in input:
                to_return.append([log10(y) for y in x])
        return to_return

"""
        
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

"""

if __name__ == "__main__":
    #sys.argv = [sys.argv[0], 'ICC_alignment_approx_FINAL', 'alignment_ORIG_FINAL']
    main()#sys.argv)

