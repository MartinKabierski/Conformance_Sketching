import argparse
import sys
import pandas as pd
import matplotlib.pyplot as plt
import numpy as np


def main(argv):
	parser = argparse.ArgumentParser()
	parser.add_argument("logtypes", help="input log class")
	args = parser.parse_args()

#open files
	origResultsFile ="../results/"+str(args.logtypes)+"_origResults"
	iccResultsFile="../results/"+str(args.logtypes)+"_iccResults"
	iccBestRunParameterFile="../results/"+str(args.logtypes)+"_iccTraceFitness"

#read in original file
	orig_results_df = pd.read_csv(origResultsFile)
	origMeanValues=[orig_results_df["Fitness"].mean(),orig_results_df[" Traces"].mean(),orig_results_df[" Time"].mean()]
	print()
	print("###Original Results###")
	print(orig_results_df)
	print()
	print("###Mean of Original Results###")
	print(origMeanValues)
	print()

	icc_results_df = pd.read_csv(iccResultsFile)
	print("###ICCResults###")
	print(icc_results_df)
	print()

	#parameters have struct delta, alpha, epsilon, k, initsize
	with open(iccBestRunParameterFile) as file:
		head = [next(file) for x in range(0,2)]
	parameters=head[1].split()

	deltaValues=icc_results_df["Delta"].unique()
	alphaValues=icc_results_df[" Alpha"].unique()
	epsilonValues=icc_results_df[" Epsilon"].unique()
	kValues=icc_results_df[" K"].unique()
	initSizeValues=icc_results_df[" initSize"].unique()
	print("###used parameters for Evaluation###")
	print("Delta:    " + str(deltaValues))
	print("Alpha:    " + str(alphaValues))
	print("Epsilon:  " + str(epsilonValues))
	print("K:        " + str(kValues))
	print("initSize: " + str(initSizeValues))
	print()

	bestDelta=parameters[0][:-1]
	bestAlpha=parameters[1][:-1]
	bestEpsilon=parameters[2][:-1]
	bestK=parameters[3][:-1]
	bestInitSize=parameters[4]
	print("###Best performing parameters###")
	print("Delta:    " +bestDelta)
	print("Alpha:    " +bestAlpha)
	print("Epsilon:  " +bestEpsilon)
	print("K:        " +bestK)
	print("initSize: " +bestInitSize)
	print()

	#get df slice that contains the best parameter
	bestDeltaDf=icc_results_df['Delta']==float(bestDelta)
	bestAlphaDf=icc_results_df[' Alpha']==float(bestAlpha)
	bestEpsilonDf=icc_results_df[' Epsilon']==float(bestEpsilon)
	bestKDf=icc_results_df[' K']==float(bestK)
	bestInitSizeDf=icc_results_df[' initSize']==float(bestInitSize)
	#print(bestDelta)

	bestParameterRuns=icc_results_df[bestDeltaDf & bestAlphaDf & bestEpsilonDf & bestKDf & bestInitSizeDf]

	print("###Results of best Runs###")
	print(bestParameterRuns)
	print()

	print("###Mean & Variance Values for best Runs###")
	print([bestParameterRuns[" Fitness"].mean(), bestParameterRuns[" Traces"].mean(), bestParameterRuns[" Time"].mean()])
	print([bestParameterRuns[" Fitness"].var(), bestParameterRuns[" Traces"].var(), bestParameterRuns[" Time"].var()])
	print()

	print("###Mean Values based on best Parameters. One Parameter is changed###")
	for i in range (0,5):
		delta=bestDelta
		alpha=bestAlpha
		epsilon=bestEpsilon
		k=bestK
		initSize=bestInitSize

		DeltaDf = icc_results_df['Delta'] == float(bestDelta)
		AlphaDf = icc_results_df[' Alpha'] == float(bestAlpha)
		EpsilonDf = icc_results_df[' Epsilon'] == float(bestEpsilon)
		KDf = icc_results_df[' K'] == float(bestK)
		InitSizeDf = icc_results_df[' initSize'] == float(bestInitSize)
		if i==0:
			fitness=[]
			traces=[]
			time=[]
			for currentDelta in deltaValues:
				delta=currentDelta
				print("Delta: "+str(delta))
				DeltaDf = icc_results_df['Delta'] == float(currentDelta)
				currentDf = icc_results_df[DeltaDf & AlphaDf & EpsilonDf & KDf & InitSizeDf]
				result=evaluate_df(currentDf)
				print(result)
				fitness.append(result[0])
				traces.append(result[1])
				time.append(result[2])
			print (traces)

			plt.figure(num=None, figsize=(8 , 5), dpi=80, facecolor='w')
			plt.subplots_adjust(wspace=2)


			plt.subplot(1,3,1)
			plt.boxplot(fitness)
			plt.xlabel("Delta")
			plt.ylabel("Fitness")
			plt.xticks(np.arange(1,len(deltaValues)+1) ,deltaValues)
			#plt.savefig(args.logtypes+"DeltaChanging_Fitness.png")

			plt.subplot(1, 3, 2)
			plt.boxplot(traces)
			plt.xlabel("Delta")
			plt.ylabel("Traces")
			plt.xticks(np.arange(1,len(deltaValues)+1) ,deltaValues)
			#plt.savefig(args.logtypes+"DeltaChanging_Traces.png")

			plt.subplot(1, 3, 3)
			plt.boxplot(time)
			plt.xlabel("Delta")
			plt.ylabel("Time")
			plt.xticks(np.arange(1,len(deltaValues)+1) ,deltaValues)
			#plt.savefig(args.logtypes+"DeltaChanging_Time.png")
			plt.savefig(args.logtypes+"DeltaChanging.png")			

		if i==1:
			fitness=[]
			traces=[]
			time=[]
			for currentAlpha in alphaValues:
				alpha=currentAlpha
				print("Alpha: "+str(alpha))
				AlphaDf = icc_results_df[' Alpha'] == float(currentAlpha)
				currentDf = icc_results_df[DeltaDf & AlphaDf & EpsilonDf & KDf & InitSizeDf]
				result=evaluate_df(currentDf)
				print(result)
				fitness.append(result[0])
				traces.append(result[1])
				time.append(result[2])

			plt.figure(num=None, figsize=(8 , 5), dpi=80, facecolor='w')
			plt.subplots_adjust(wspace=2)

			plt.subplot(1,3,1)
			plt.boxplot(fitness)
			plt.xlabel("Alpha")
			plt.ylabel("Fitness")
			plt.xticks(np.arange(1,len(alphaValues)+1) ,alphaValues)
			#plt.savefig(args.logtypes+"DeltaChanging_Fitness.png")

			plt.subplot(1, 3, 2)
			plt.boxplot(traces)
			plt.xlabel("Alpha")
			plt.ylabel("Traces")
			plt.xticks(np.arange(1,len(alphaValues)+1) ,alphaValues)
			#plt.savefig(args.logtypes+"DeltaChanging_Traces.png")

			plt.subplot(1, 3, 3)
			plt.boxplot(time)
			plt.xlabel("Alpha")
			plt.ylabel("Time")
			plt.xticks(np.arange(1,len(alphaValues)+1) ,alphaValues)
			#plt.savefig(args.logtypes+"DeltaChanging_Time.png")
			plt.savefig(args.logtypes+"AlphaChanging.png")

		if i==2:
			fitness=[]
			traces=[]
			time=[]
			for currentEpsilon in epsilonValues:
				epsilon=currentEpsilon
				print("Epsilon: "+str(epsilon))
				EpsilonDf = icc_results_df[' Epsilon'] == float(currentEpsilon)
				currentDf = icc_results_df[DeltaDf & AlphaDf & EpsilonDf & KDf & InitSizeDf]
				result=evaluate_df(currentDf)
				print(result)
				fitness.append(result[0])
				traces.append(result[1])
				time.append(result[2])

			plt.figure(num=None, figsize=(8 , 5), dpi=80, facecolor='w')
			plt.subplots_adjust(wspace=2)

			plt.subplot(1,3,1)
			plt.boxplot(fitness)
			plt.xlabel("Epsilon")
			plt.ylabel("Fitness")
			plt.xticks(np.arange(1,len(epsilonValues)+1) ,epsilonValues)
			#plt.savefig(args.logtypes+"DeltaChanging_Fitness.png")

			plt.subplot(1, 3, 2)
			plt.boxplot(traces)
			plt.xlabel("Epsilon")
			plt.ylabel("Traces")
			plt.xticks(np.arange(1,len(epsilonValues)+1) ,epsilonValues)
			#plt.savefig(args.logtypes+"DeltaChanging_Traces.png")

			plt.subplot(1, 3, 3)
			plt.boxplot(time)
			plt.xlabel("Epsilon")
			plt.ylabel("Time")
			plt.xticks(np.arange(1,len(epsilonValues)+1) ,epsilonValues)
			#plt.savefig(args.logtypes+"DeltaChanging_Time.png")
			plt.savefig(args.logtypes+"EpsilonChanging.png")

		if i==3:
			fitness=[]
			traces=[]
			time=[]
			for currentK in kValues:
				k=currentK
				print("K: "+ str(k))
				KDf = icc_results_df[' K'] == float(currentK)
				currentDf = icc_results_df[DeltaDf & AlphaDf & EpsilonDf & KDf & InitSizeDf]
				result=evaluate_df(currentDf)
				print(result)
				fitness.append(result[0])
				traces.append(result[1])
				time.append(result[2])

			plt.figure(num=None, figsize=(8 , 5), dpi=80, facecolor='w')
			plt.subplots_adjust(wspace=2)

			plt.subplot(1,3,1)
			plt.boxplot(fitness)
			plt.xlabel("K")
			plt.ylabel("Fitness")
			plt.xticks(np.arange(1,len(kValues)+1) ,kValues)
			#plt.savefig(args.logtypes+"DeltaChanging_Fitness.png")

			plt.subplot(1, 3, 2)
			plt.boxplot(traces)
			plt.xlabel("K")
			plt.ylabel("Traces")
			plt.xticks(np.arange(1,len(kValues)+1) ,kValues)
			#plt.savefig(args.logtypes+"DeltaChanging_Traces.png")

			plt.subplot(1, 3, 3)
			plt.boxplot(time)
			plt.xlabel("K")
			plt.ylabel("Time")
			plt.xticks(np.arange(1,len(kValues)+1) ,kValues)
			#plt.savefig(args.logtypes+"DeltaChanging_Time.png")
			plt.savefig(args.logtypes+"KChanging.png")

		if i==4:
			fitness=[]
			traces=[]
			time=[]
			for currentInitSize in initSizeValues:
				initSize=currentInitSize
				print("initSize: "+str(initSize))
				InitSizeDf = icc_results_df[' initSize'] == float(currentInitSize)
				currentDf = icc_results_df[DeltaDf & AlphaDf & EpsilonDf & KDf & InitSizeDf]
				result=evaluate_df(currentDf)
				print(result)
				fitness.append(result[0])
				traces.append(result[1])
				time.append(result[2])

			plt.figure(num=None, figsize=(8 , 5), dpi=80, facecolor='w')
			plt.subplots_adjust(wspace=2)

			plt.subplot(1,3,1)
			plt.boxplot(fitness)
			plt.xlabel("initial Size")
			plt.ylabel("Fitness")
			plt.xticks(np.arange(1,len(initSizeValues)+1) ,initSizeValues)
			#plt.savefig(args.logtypes+"DeltaChanging_Fitness.png")

			plt.subplot(1, 3, 2)
			plt.boxplot(traces)
			plt.xlabel("initial Size")
			plt.ylabel("Traces")
			plt.xticks(np.arange(1,len(initSizeValues)+1) ,initSizeValues)
			#plt.savefig(args.logtypes+"DeltaChanging_Traces.png")

			plt.subplot(1, 3, 3)
			plt.boxplot(time)
			plt.xlabel("initial Size")
			plt.ylabel("Time")
			plt.xticks(np.arange(1,len(initSizeValues)+1) ,initSizeValues)
			#plt.savefig(args.logtypes+"DeltaChanging_Time.png")
			plt.savefig(args.logtypes+"initSizeChanging.png")
		print()

def evaluate_df(df):
        asynchMovesRel=df[" asynchMovesRel"].values
        print(asynchMovesRel)
        return [df[" Fitness"].values, df[" Traces"].values, df[" Time"].values]


if __name__ == '__main__':
    main(sys.argv)
