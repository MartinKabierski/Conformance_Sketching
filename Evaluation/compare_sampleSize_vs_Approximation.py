import argparse
import sys
import pandas as pd
import matplotlib.pyplot as plt
import numpy as np
import re
from collections import defaultdict
from math import log10
import matplotlib.patches as mpatches
import matplotlib.ticker
import statistics

NO_THREADS_BASELINE = "10"
INPUT = "BPI_Challenge_2012"

def main():
        #read in data frame
        fitness=pd.read_csv("./results/results10/"+INPUT+"_fitness.csv", sep=';')
        fitnessApprox=pd.read_csv("./results/results10/"+INPUT+"_fitnessApprox.csv", sep=';')
        deviations=pd.read_csv("./results/results10/"+INPUT+"_deviations.csv", sep=';')
        deviationsApprox=pd.read_csv("./results/results10/"+INPUT+"_deviationsApprox.csv", sep=';')
        
        orig=pd.read_csv("./results/results10/"+INPUT+"_baseline.csv", sep=';')
        origFitness_mean=orig["fitness"].mean()
        origDeviations=orig["deviations"]


        print("Loaded input files")
        print()
        heuristics="NONALIGNING_KNOWN"

        #set list of used parameters
        delta_list=[0.1, 0.05, 0.01]
        alpha_list=[0.9, 0.95, 0.99]
        epsilon=0.01
        k_list=[0.1, 0.2, 0.3]
        print("###used parameters for Evaluation###")
        print("Delta:    " + str(delta_list))
        print("Alpha:    " + str(alpha_list))
        print("Epsilon:  " + str(epsilon))
        print("K      :  " + str(k_list))

        print()

#TODO MEAN SQUARED ERROR

#fitness
        fitness_dist=[]
        k_dist=[]
        for k in k_list:
            fitness_list=[]
            k_dist=[]
            for d in delta_list:
                for a in alpha_list:
                    fitness_value =fitness.loc[(fitness["delta"]==d) & (fitness["alpha"]==a) & (fitness["epsilon"]==epsilon)]["fitness"].mean()
                    fitness_list.append(derive_distance_fitness(fitness_value, origFitness_mean))

                    fitness_value =fitnessApprox.loc[(fitnessApprox["delta"]==d) & (fitnessApprox["alpha"]==a) & (fitnessApprox["epsilon"]==epsilon) & (fitnessApprox["k"]==k)]["fitness"].mean()
                    k_dist.append(derive_distance_fitness(fitness_value, origFitness_mean))
            print("k="+str(k)+":   "+str(k_dist))
        print("Fitness: "+str(fitness_list))

#deviations
        deviation_dist=[]
        k_nonaligning_all_dist=[]
        k_nonaligning_known_dist=[]
        for k in k_list:
            #print(k)
            k_nonaligning_all_dist=[]
            k_nonaligning_known_dist=[]
            deviation_dist=[]
            for d in delta_list:
                for a in alpha_list:
                    deviation_values =deviations.loc[(deviations["delta"]==d) & (deviations["alpha"]==a) & (deviations["epsilon"]==epsilon)]["deviations"]
                    deviation_dist.append(derive_distance_deviations(deviation_values, origDeviations))

                    deviation_values =deviationsApprox.loc[(deviationsApprox["delta"]==d) & (deviationsApprox["alpha"]==a) & (deviationsApprox["epsilon"]==epsilon) & (deviationsApprox["k"]==k) & (deviationsApprox["approximationMode"]=="NONALIGNING_ALL")]["deviations"]
                    k_nonaligning_all_dist.append(derive_distance_deviations(deviation_values, origDeviations))

                    deviation_values =deviationsApprox.loc[(deviationsApprox["delta"]==d) & (deviationsApprox["alpha"]==a) & (deviationsApprox["epsilon"]==epsilon) & (deviationsApprox["k"]==k) & (deviationsApprox["approximationMode"]=="NONALIGNING_KNOWN")]["deviations"]
                    k_nonaligning_known_dist.append(derive_distance_deviations(deviation_values, origDeviations))
            #print("Nonaligning_all   k="+str(k)+":   "+str(k_nonaligning_all_dist))
            print("Nonaligning_known k="+str(k)+":   "+str(k_nonaligning_known_dist))
        print("Deviations: "+str(deviation_dist))

                

def derive_distance_fitness(a, b):
    return abs(a - b)**2

def derive_distance_deviations(sample, original):
    #print(sample)
    deviations_sample=get_asynchMovesDict(sample)
    #print(deviations_sample)
    deviations_original=get_asynchMovesDict(original)
    #print(deviations_sample)

    keys=set(deviations_sample.keys())
    #print(keys.difference(deviations_original.keys()))
    keys=keys.union(deviations_original.keys())
    #print(keys)
    #print()
    dist=0
    cnt=0
    for key in keys:
        #print(key)
        if key not in deviations_sample:
            cnt=cnt+deviations_original[key]
        if key not in deviations_original:
            #print(deviations_sample[key])
            cnt=cnt+deviations_sample[key]
        if key in deviations_sample and key in deviations_original:
        #print(str(key)+": "+str(deviations_sample[key]))
        #print(str(key)+": "+str(deviations_original[key]))
            #print(deviations_sample[key])
            #print(deviations_original[key])
            dist=dist+(abs(deviations_sample[key]-deviations_original[key]))**2
    if len(keys)==0:
        return dist
    #dist=dist/len(keys)
    #print(cnt)
    #print()

    return dist
    #keys=set().union(deviations_sample.keys(),deviations_original.keys())
    
    #keys.update(deviations_original.keys())
    #print(keys)

#get map view of deviating activities and mean frequency of said deviations
def get_asynchMovesDict(df):
        asynchMovesRelDict=defaultdict(list)
        asynchMovesRel=df
        for asynchmoves in asynchMovesRel:
                 matchObject = re.search("\{(.+)\}", asynchmoves, flags=0)
                 if matchObject is None:
                     continue
                 event= matchObject.group(1).split(",")
                 for keyValuePair in event:
                         key,value=keyValuePair.split("=")
                         asynchMovesRelDict[key].append(float(value))
        for key in asynchMovesRelDict.keys():
               asynchMovesRelDict[key]=statistics.median(asynchMovesRelDict[key])
        #for key in asynchMovesRelDict.keys():
        #     asynchMovesRelDict[key]=sum(asynchMovesRelDict[key]) / len(asynchMovesRelDict[key])
        return asynchMovesRelDict


if __name__ == "__main__":
    main()
