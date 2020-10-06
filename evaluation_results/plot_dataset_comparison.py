import argparse
import sys
import pandas as pd
import matplotlib.pyplot as plt
import numpy as np
import re
from collections import defaultdict
from math import log10
import matplotlib.patches as mpatches
import statistics

NO_THREADS_BASELINE = "DEFAULT"


def main():
    font = {'font.family': 'normal',
            # 'font.weight' : 'bold',
            'font.size': 18}
    plt.rcParams.update(font)
    blue_patch = mpatches.Patch(color='blue', label='Original')
    x_ticks_labels = ['f', 'fa', 'd', 'da']

    # set parameters for original-to-implementation comparison for all three logs
    chosen_delta = 0.01
    chosen_alpha = 0.99
    chosen_epsilon = 0.01
    chosen_k = 0.2

    # BPI 2012#
    bpi2012_baseline = pd.read_csv("./results/BPI_Challenge_2012_baseline.csv", sep=';')
    bpi2012_fitness = pd.read_csv("./results/BPI_Challenge_2012_fitness.csv", sep=';')
    bpi2012_fitnessApprox = pd.read_csv("./results/BPI_Challenge_2012_fitnessApprox.csv", sep=';')
    bpi2012_deviations = pd.read_csv("./results/BPI_Challenge_2012_deviations.csv", sep=';')
    bpi2012_deviationsApprox = pd.read_csv("./results/BPI_Challenge_2012_deviationsApprox.csv", sep=';')

    bpi2012_fitness = bpi2012_fitness.loc[
        (bpi2012_fitness["delta"] == chosen_delta) & (bpi2012_fitness["alpha"] == chosen_alpha) & (
                    bpi2012_fitness["epsilon"] == chosen_epsilon)]
    bpi2012_fitnessApprox = bpi2012_fitnessApprox.loc[
        (bpi2012_fitnessApprox["delta"] == chosen_delta) & (bpi2012_fitnessApprox["alpha"] == chosen_alpha) & (
                    bpi2012_fitnessApprox["epsilon"] == chosen_epsilon) & (bpi2012_fitnessApprox["k"] == chosen_k)]
    bpi2012_deviations = bpi2012_deviations.loc[
        (bpi2012_deviations["delta"] == chosen_delta) & (bpi2012_deviations["alpha"] == chosen_alpha) & (
                    bpi2012_deviations["epsilon"] == chosen_epsilon)]
    bpi2012_deviationsApprox = bpi2012_deviationsApprox.loc[
        (bpi2012_deviationsApprox["delta"] == chosen_delta) & (bpi2012_deviationsApprox["alpha"] == chosen_alpha) & (
                    bpi2012_deviationsApprox["epsilon"] == chosen_epsilon) & (
                    bpi2012_deviationsApprox["k"] == chosen_k) & (
                    bpi2012_deviationsApprox["approximationMode"] == "NONALIGNING_KNOWN")]

    # BPI 2014#
    bpi2014_baseline = pd.read_csv("./results/Detail_Incident_Activity_baseline.csv", sep=';')
    bpi2014_fitness = pd.read_csv("./results/Detail_Incident_Activity_fitness.csv", sep=';')
    bpi2014_fitnessApprox = pd.read_csv("./results/Detail_Incident_Activity_fitnessApprox.csv", sep=';')
    bpi2014_deviations = pd.read_csv("./results/Detail_Incident_Activity_deviations.csv", sep=';')
    bpi2014_deviationsApprox = pd.read_csv("./results/Detail_Incident_Activity_deviationsApprox.csv", sep=';')

    bpi2014_fitness = bpi2014_fitness.loc[
        (bpi2014_fitness["delta"] == chosen_delta) & (bpi2014_fitness["alpha"] == chosen_alpha) & (
                    bpi2014_fitness["epsilon"] == chosen_epsilon)]
    bpi2014_fitnessApprox = bpi2014_fitnessApprox.loc[
        (bpi2014_fitnessApprox["delta"] == chosen_delta) & (bpi2014_fitnessApprox["alpha"] == chosen_alpha) & (
                    bpi2014_fitnessApprox["epsilon"] == chosen_epsilon) & (bpi2014_fitnessApprox["k"] == chosen_k)]
    bpi2014_deviations = bpi2014_deviations.loc[
        (bpi2014_deviations["delta"] == chosen_delta) & (bpi2014_deviations["alpha"] == chosen_alpha) & (
                    bpi2014_deviations["epsilon"] == chosen_epsilon)]
    bpi2014_deviationsApprox = bpi2014_deviationsApprox.loc[
        (bpi2014_deviationsApprox["delta"] == chosen_delta) & (bpi2014_deviationsApprox["alpha"] == chosen_alpha) & (
                    bpi2014_deviationsApprox["epsilon"] == chosen_epsilon) & (
                    bpi2014_deviationsApprox["k"] == chosen_k) & (
                    bpi2014_deviationsApprox["approximationMode"] == "NONALIGNING_KNOWN")]

    # road traffic fines
    road_traffic_baseline = pd.read_csv("./results/Road_Traffic_Fines_Management_Process_baseline.csv",
                                        sep=';')
    road_traffic_fitness = pd.read_csv("./results/Road_Traffic_Fines_Management_Process_fitness.csv", sep=';')
    road_traffic_fitnessApprox = pd.read_csv(
        "./results/Road_Traffic_Fines_Management_Process_fitnessApprox.csv", sep=';')
    road_traffic_deviations = pd.read_csv("./results/Road_Traffic_Fines_Management_Process_deviations.csv",
                                          sep=';')
    road_traffic_deviationsApprox = pd.read_csv(
        "./results/Road_Traffic_Fines_Management_Process_deviationsApprox.csv", sep=';')

    road_traffic_fitness = road_traffic_fitness.loc[
        (road_traffic_fitness["delta"] == chosen_delta) & (road_traffic_fitness["alpha"] == chosen_alpha) & (
                    road_traffic_fitness["epsilon"] == chosen_epsilon)]
    road_traffic_fitnessApprox = road_traffic_fitnessApprox.loc[
        (road_traffic_fitnessApprox["delta"] == chosen_delta) & (
                    road_traffic_fitnessApprox["alpha"] == chosen_alpha) & (
                    road_traffic_fitnessApprox["epsilon"] == chosen_epsilon) & (
                    road_traffic_fitnessApprox["k"] == chosen_k)]
    road_traffic_deviations = road_traffic_deviations.loc[
        (road_traffic_deviations["delta"] == chosen_delta) & (road_traffic_deviations["alpha"] == chosen_alpha) & (
                    road_traffic_deviations["epsilon"] == chosen_epsilon)]
    road_traffic_deviationsApprox = road_traffic_deviationsApprox.loc[
        (road_traffic_deviationsApprox["delta"] == chosen_delta) & (
                    road_traffic_deviationsApprox["alpha"] == chosen_alpha) & (
                    road_traffic_deviationsApprox["epsilon"] == chosen_epsilon) & (
                    road_traffic_deviationsApprox["k"] == chosen_k) & (
                    road_traffic_deviationsApprox["approximationMode"] == "NONALIGNING_KNOWN")]

    #random dude traffic fines
    rdRTF_baseline = pd.read_csv("./results/RTFM_model2_baseline.csv",
                                        sep=';')
    rdRTF_fitness = pd.read_csv("./results/RTFM_model2_fitness.csv", sep=';')
    rdRTF_fitnessApprox = pd.read_csv(
        "./results/RTFM_model2_fitnessApprox.csv", sep=';')
    rdRTF_deviations = pd.read_csv("./results/RTFM_model2_deviations.csv",
                                          sep=';')
    rdRTF_deviationsApprox = pd.read_csv(
        "./results/RTFM_model2_deviationsApprox.csv", sep=';')

    rdRTF_fitness = rdRTF_fitness.loc[
        (rdRTF_fitness["delta"] == chosen_delta) & (rdRTF_fitness["alpha"] == chosen_alpha) & (
                rdRTF_fitness["epsilon"] == chosen_epsilon)]
    rdRTF_fitnessApprox = rdRTF_fitnessApprox.loc[
        (rdRTF_fitnessApprox["delta"] == chosen_delta) & (
                rdRTF_fitnessApprox["alpha"] == chosen_alpha) & (
                rdRTF_fitnessApprox["epsilon"] == chosen_epsilon) & (
                rdRTF_fitnessApprox["k"] == chosen_k)]
    rdRTF_deviations = rdRTF_deviations.loc[
        (rdRTF_deviations["delta"] == chosen_delta) & (rdRTF_deviations["alpha"] == chosen_alpha) & (
                rdRTF_deviations["epsilon"] == chosen_epsilon)]
    rdRTF_deviationsApprox = rdRTF_deviationsApprox.loc[
        (rdRTF_deviationsApprox["delta"] == chosen_delta) & (
                rdRTF_deviationsApprox["alpha"] == chosen_alpha) & (
                rdRTF_deviationsApprox["epsilon"] == chosen_epsilon) & (
                rdRTF_deviationsApprox["k"] == chosen_k) & (
                rdRTF_deviationsApprox["approximationMode"] == "NONALIGNING_KNOWN")]


    # plot computing time comparisons
    bpi2012_orig_mean = bpi2012_baseline["time"].mean()
    bpi2012_list = []
    bpi2012_list.append(bpi2012_fitness["time"].values / bpi2012_orig_mean)
    bpi2012_list.append(bpi2012_fitnessApprox["time"].values / bpi2012_orig_mean)
    bpi2012_list.append(bpi2012_deviations["time"].values / bpi2012_orig_mean)
    bpi2012_list.append(bpi2012_deviationsApprox["time"].values / bpi2012_orig_mean)

    bpi2014_orig_mean = bpi2014_baseline["time"].mean()
    bpi2014_list = []
    bpi2014_list.append(bpi2014_fitness["time"].values / bpi2014_orig_mean)
    bpi2014_list.append(bpi2014_fitnessApprox["time"].values / bpi2014_orig_mean)
    bpi2014_list.append(bpi2014_deviations["time"].values / bpi2014_orig_mean)
    bpi2014_list.append(bpi2014_deviationsApprox["time"].values / bpi2014_orig_mean)

    road_traffic_orig_mean = road_traffic_baseline["time"].mean()
    road_traffic_list = []
    road_traffic_list.append(road_traffic_fitness["time"].values / road_traffic_orig_mean)
    road_traffic_list.append(road_traffic_fitnessApprox["time"].values / road_traffic_orig_mean)
    road_traffic_list.append(road_traffic_deviations["time"].values / road_traffic_orig_mean)
    road_traffic_list.append(road_traffic_deviationsApprox["time"].values / road_traffic_orig_mean)

    f, (ax1, ax2, ax3) = plt.subplots(1, 3, sharey=True)

    ax1.set_yscale('log')
    ax1.set_ylabel('Runtime (relative)')
    ax1.set_ylim(0.0007, 10)
    ax1.set_yticks([0.001, 0.01, 0.1, 1.0, 10.0])
    ax1.set_yticklabels(["0.1%", "1%", "10%", "100%"])
    ax1.set_title('BPI-12', fontsize=18)
    ax1.boxplot(bpi2012_list)
    # print(bpi2012_list)
    # ax1.axhline(1, color='b', linestyle='--')
    ax1.set_xticklabels(x_ticks_labels, rotation=0, fontsize=18)
    ax1.tick_params(length=6, width=2)
    ax1.tick_params(which='minor', length=4, width=1)

    ax2.set_title('BPI-14', fontsize=18)
    ax2.boxplot(bpi2014_list)
    # ax2.axhline(1, color='b', linestyle='--')
    ax2.set_xticklabels(x_ticks_labels, rotation=0, fontsize=18)
    ax2.tick_params(length=6, width=2)
    ax2.tick_params(which='minor', length=4, width=1)

    ax3.set_title('Traffic Fines', fontsize=18)
    ax3.boxplot(road_traffic_list)
    # ax3.axhline(1, color='b', linestyle='--')
    ax3.set_xticklabels(x_ticks_labels, rotation=0, fontsize=18)
    ax3.tick_params(length=6, width=2)
    ax3.tick_params(which='minor', length=4, width=1)

    #f.show()
    f.savefig("./real_computing_time.pdf", bbox_inches='tight')

    # plot sampled trace comparisons
    bpi2012_orig_mean = bpi2012_baseline["logSize"].mean()
    bpi2012_list = []
    bpi2012_list.append(bpi2012_fitness["logSize"].values / bpi2012_orig_mean)
    bpi2012_list.append(bpi2012_fitnessApprox["logSize"].values / bpi2012_orig_mean)
    bpi2012_list.append(bpi2012_deviations["logSize"].values / bpi2012_orig_mean)
    bpi2012_list.append(bpi2012_deviationsApprox["logSize"].values / bpi2012_orig_mean)

    bpi2014_orig_mean = bpi2014_baseline["logSize"].mean()
    bpi2014_list = []
    bpi2014_list.append(bpi2014_fitness["logSize"].values / bpi2014_orig_mean)
    bpi2014_list.append(bpi2014_fitnessApprox["logSize"].values / bpi2014_orig_mean)
    bpi2014_list.append(bpi2014_deviations["logSize"].values / bpi2014_orig_mean)
    bpi2014_list.append(bpi2014_deviationsApprox["logSize"].values / bpi2014_orig_mean)

    road_traffic_orig_mean = road_traffic_baseline["logSize"].mean()
    road_traffic_list = []
    road_traffic_list.append(road_traffic_fitness["logSize"].values / road_traffic_orig_mean)
    road_traffic_list.append(road_traffic_fitnessApprox["logSize"].values / road_traffic_orig_mean)
    road_traffic_list.append(road_traffic_deviations["logSize"].values / road_traffic_orig_mean)
    road_traffic_list.append(road_traffic_deviationsApprox["logSize"].values / road_traffic_orig_mean)

    f, (ax1, ax2, ax3) = plt.subplots(1, 3, sharey=True)
    # f.legend(handles=[blue_patch],loc='upper right')
    # f.set_size_inches(6,5)
    ax1.set_yscale('log')
    ax1.set_ylabel('Sampled traces')
    ax1.set_ylim(0.0007, 3)
    ax1.set_yticks([0.001, 0.01, 0.1, 1.0])
    ax1.set_yticklabels(["0.1%", "1%", "10%", "100%"])
    ax1.set_title('BPI-12', fontsize=18)
    ax1.boxplot(bpi2012_list)
    ax1.boxplot(bpi2012_list)
    # ax1.axhline(1, color='b', linestyle='--')
    ax1.set_xticklabels(x_ticks_labels, rotation='horizontal', fontsize=18)
    ax1.tick_params(length=6, width=2)
    ax1.tick_params(which='minor', length=4, width=1)

    ax2.set_title('BPI-14', fontsize=18)
    ax2.boxplot(bpi2014_list)
    # ax2.axhline(1, color='b', linestyle='--')
    ax2.set_xticklabels(x_ticks_labels, rotation='horizontal', fontsize=18)
    ax2.tick_params(length=6, width=2)
    ax2.tick_params(which='minor', length=4, width=1)

    ax3.set_title('Traffic Fines', fontsize=18)
    ax3.boxplot(road_traffic_list)
    # ax3.axhline(1, color='b', linestyle='--')
    ax3.set_xticklabels(x_ticks_labels, rotation='horizontal', fontsize=18)
    ax3.tick_params(length=6, width=2)
    ax3.tick_params(which='minor', length=4, width=1)

    #f.show()
    f.savefig("./real_traces.pdf", bbox_inches='tight')

    # plot fitness comparisons
    bpi2012_orig_mean = bpi2012_baseline["fitness"].mean()
    bpi2012_list = []
    bpi2012_list.append(bpi2012_fitness["fitness"].values)
    bpi2012_list.append(bpi2012_fitnessApprox["fitness"].values)

    bpi2014_orig_mean = bpi2014_baseline["fitness"].mean()
    bpi2014_list = []
    bpi2014_list.append(bpi2014_fitness["fitness"].values)
    bpi2014_list.append(bpi2014_fitnessApprox["fitness"].values)

    road_traffic_orig_mean = road_traffic_baseline["fitness"].mean()
    road_traffic_list = []
    road_traffic_list.append(road_traffic_fitness["fitness"].values)
    road_traffic_list.append(road_traffic_fitnessApprox["fitness"].values)

    f, (ax1, ax2, ax3) = plt.subplots(1, 3, sharey=True)
    # f.set_size_inches(6,4)
    # f.legend(handles=[blue_patch],loc='upper right')
    ax1.set_ylabel('Fitness')
    ax1.set_yticks([0.5, 0.6, 0.7, 0.8, 0.9, 1.0])
    ax1.set_ylim(0.49, 1.01)

    ax1.boxplot(bpi2012_list)
    ax1.axhline(bpi2012_orig_mean, color='b', linestyle='--')
    ax1.set_xticklabels(x_ticks_labels[:2], rotation='horizontal', fontsize=18)
    ax1.tick_params(length=6, width=2)
    ax1.tick_params(which='minor', length=4, width=1)
    ax1.set_title('BPI-12', fontsize=18)

    ax2.boxplot(bpi2014_list)
    ax2.axhline(bpi2014_orig_mean, color='b', linestyle='--')
    ax2.set_xticklabels(x_ticks_labels[:2], rotation='horizontal', fontsize=18)
    ax2.tick_params(length=6, width=2)
    ax2.tick_params(which='minor', length=4, width=1)
    ax2.set_title('BPI-14', fontsize=18)

    ax3.boxplot(road_traffic_list)
    ax3.axhline(road_traffic_orig_mean, color='b', linestyle='--')
    ax3.set_xticklabels(x_ticks_labels[:2], rotation='horizontal', fontsize=18)
    ax3.tick_params(length=6, width=2)
    ax3.tick_params(which='minor', length=4, width=1)
    ax3.set_title('Traffic Fines', fontsize=18)

    #f.show()
    f.savefig("./real_fitness.pdf", bbox_inches='tight')

#construct plots for all 4 datasets
    bpi2012_orig_mean = bpi2012_baseline["time"].mean()
    bpi2012_list = []
    bpi2012_list.append(bpi2012_fitness["time"].values / bpi2012_orig_mean)
    bpi2012_list.append(bpi2012_fitnessApprox["time"].values / bpi2012_orig_mean)
    bpi2012_list.append(bpi2012_deviations["time"].values / bpi2012_orig_mean)
    bpi2012_list.append(bpi2012_deviationsApprox["time"].values / bpi2012_orig_mean)

    bpi2014_orig_mean = bpi2014_baseline["time"].mean()
    bpi2014_list = []
    bpi2014_list.append(bpi2014_fitness["time"].values / bpi2014_orig_mean)
    bpi2014_list.append(bpi2014_fitnessApprox["time"].values / bpi2014_orig_mean)
    bpi2014_list.append(bpi2014_deviations["time"].values / bpi2014_orig_mean)
    bpi2014_list.append(bpi2014_deviationsApprox["time"].values / bpi2014_orig_mean)

    road_traffic_orig_mean = road_traffic_baseline["time"].mean()
    road_traffic_list = []
    road_traffic_list.append(road_traffic_fitness["time"].values / road_traffic_orig_mean)
    road_traffic_list.append(road_traffic_fitnessApprox["time"].values / road_traffic_orig_mean)
    road_traffic_list.append(road_traffic_deviations["time"].values / road_traffic_orig_mean)
    road_traffic_list.append(road_traffic_deviationsApprox["time"].values / road_traffic_orig_mean)

    rdRTF_orig_mean = rdRTF_baseline["time"].mean()
    rdRTF_list = []
    rdRTF_list.append(rdRTF_fitness["time"].values / rdRTF_orig_mean)
    rdRTF_list.append(rdRTF_fitnessApprox["time"].values / rdRTF_orig_mean)
    rdRTF_list.append(rdRTF_deviations["time"].values / rdRTF_orig_mean)
    rdRTF_list.append(rdRTF_deviationsApprox["time"].values / rdRTF_orig_mean)
    f, (ax1, ax2, ax3, ax4) = plt.subplots(1, 4, sharey=True)

    ax1.set_yscale('log')
    ax1.set_ylabel('Runtime (relative)')
    ax1.set_ylim(0.0007, 11)
    ax1.set_yticks([0.001, 0.01, 0.1, 1.0, 10.0])
    ax1.set_yticklabels(["0.1%", "1%", "10%", "100%", "1000%"])
    ax1.set_title('BPI-12', fontsize=18)
    ax1.boxplot(bpi2012_list)
    # print(bpi2012_list)
    # ax1.axhline(1, color='b', linestyle='--')
    ax1.set_xticklabels(x_ticks_labels, rotation=0, fontsize=18)
    ax1.tick_params(length=6, width=2)
    ax1.tick_params(which='minor', length=4, width=1)

    ax2.set_title('BPI-14', fontsize=18)
    ax2.boxplot(bpi2014_list)
    # ax2.axhline(1, color='b', linestyle='--')
    ax2.set_xticklabels(x_ticks_labels, rotation=0, fontsize=18)
    ax2.tick_params(length=6, width=2)
    ax2.tick_params(which='minor', length=4, width=1)

    ax3.set_title('RTF', fontsize=18)
    ax3.boxplot(road_traffic_list)
    # ax3.axhline(1, color='b', linestyle='--')
    ax3.set_xticklabels(x_ticks_labels, rotation=0, fontsize=18)
    ax3.tick_params(length=6, width=2)
    ax3.tick_params(which='minor', length=4, width=1)
    
    ax4.set_title('RTFr', fontsize=18)
    ax4.boxplot(rdRTF_list)
    # ax4.axhline(1, color='b', linestyle='--')
    ax4.set_xticklabels(x_ticks_labels, rotation=0, fontsize=18)
    ax4.tick_params(length=6, width=2)
    ax4.tick_params(which='minor', length=4, width=1)

    #f.show()
    f.savefig("./real4_computing_time.pdf", bbox_inches='tight')

    bpi2012_orig_mean = bpi2012_baseline["logSize"].mean()
    bpi2012_list = []
    bpi2012_list.append(bpi2012_fitness["logSize"].values / bpi2012_orig_mean)
    bpi2012_list.append(bpi2012_fitnessApprox["logSize"].values / bpi2012_orig_mean)
    bpi2012_list.append(bpi2012_deviations["logSize"].values / bpi2012_orig_mean)
    bpi2012_list.append(bpi2012_deviationsApprox["logSize"].values / bpi2012_orig_mean)

    bpi2014_orig_mean = bpi2014_baseline["logSize"].mean()
    bpi2014_list = []
    bpi2014_list.append(bpi2014_fitness["logSize"].values / bpi2014_orig_mean)
    bpi2014_list.append(bpi2014_fitnessApprox["logSize"].values / bpi2014_orig_mean)
    bpi2014_list.append(bpi2014_deviations["logSize"].values / bpi2014_orig_mean)
    bpi2014_list.append(bpi2014_deviationsApprox["logSize"].values / bpi2014_orig_mean)

    road_traffic_orig_mean = road_traffic_baseline["logSize"].mean()
    road_traffic_list = []
    road_traffic_list.append(road_traffic_fitness["logSize"].values / road_traffic_orig_mean)
    road_traffic_list.append(road_traffic_fitnessApprox["logSize"].values / road_traffic_orig_mean)
    road_traffic_list.append(road_traffic_deviations["logSize"].values / road_traffic_orig_mean)
    road_traffic_list.append(road_traffic_deviationsApprox["logSize"].values / road_traffic_orig_mean)

    rdRTF_orig_mean = rdRTF_baseline["logSize"].mean()
    rdRTF_list = []
    rdRTF_list.append(rdRTF_fitness["logSize"].values / rdRTF_orig_mean)
    rdRTF_list.append(rdRTF_fitnessApprox["logSize"].values / rdRTF_orig_mean)
    rdRTF_list.append(rdRTF_deviations["logSize"].values / rdRTF_orig_mean)
    rdRTF_list.append(rdRTF_deviationsApprox["logSize"].values / rdRTF_orig_mean)

    f, (ax1, ax2, ax3, ax4) = plt.subplots(1, 4, sharey=True)
    # f.legend(handles=[blue_patch],loc='upper right')
    # f.set_size_inches(6,5)
    ax1.set_yscale('log')
    ax1.set_ylabel('Sampled traces')
    ax1.set_ylim(0.0007, 3)
    ax1.set_yticks([0.001, 0.01, 0.1, 1.0])
    ax1.set_yticklabels(["0.1%", "1%", "10%", "100%"])
    ax1.set_title('BPI-12', fontsize=18)
    ax1.boxplot(bpi2012_list)
    ax1.boxplot(bpi2012_list)
    # ax1.axhline(1, color='b', linestyle='--')
    ax1.set_xticklabels(x_ticks_labels, rotation='horizontal', fontsize=18)
    ax1.tick_params(length=6, width=2)
    ax1.tick_params(which='minor', length=4, width=1)

    ax2.set_title('BPI-14', fontsize=18)
    ax2.boxplot(bpi2014_list)
    # ax2.axhline(1, color='b', linestyle='--')
    ax2.set_xticklabels(x_ticks_labels, rotation='horizontal', fontsize=18)
    ax2.tick_params(length=6, width=2)
    ax2.tick_params(which='minor', length=4, width=1)

    ax3.set_title('RTF', fontsize=18)
    ax3.boxplot(road_traffic_list)
    # ax3.axhline(1, color='b', linestyle='--')
    ax3.set_xticklabels(x_ticks_labels, rotation='horizontal', fontsize=18)
    ax3.tick_params(length=6, width=2)
    ax3.tick_params(which='minor', length=4, width=1)

    ax4.set_title('RTFr', fontsize=18)
    ax4.boxplot(rdRTF_list)
    # ax4.axhline(1, color='b', linestyle='--')
    ax4.set_xticklabels(x_ticks_labels, rotation='horizontal', fontsize=18)
    ax4.tick_params(length=6, width=2)
    #f.show()
    f.savefig("./real4_traces.pdf", bbox_inches='tight')

    # plot fitness comparisons
    bpi2012_orig_mean = bpi2012_baseline["fitness"].mean()
    bpi2012_list = []
    bpi2012_list.append(bpi2012_fitness["fitness"].values)
    bpi2012_list.append(bpi2012_fitnessApprox["fitness"].values)

    bpi2014_orig_mean = bpi2014_baseline["fitness"].mean()
    bpi2014_list = []
    bpi2014_list.append(bpi2014_fitness["fitness"].values)
    bpi2014_list.append(bpi2014_fitnessApprox["fitness"].values)

    road_traffic_orig_mean = road_traffic_baseline["fitness"].mean()
    road_traffic_list = []
    road_traffic_list.append(road_traffic_fitness["fitness"].values)
    road_traffic_list.append(road_traffic_fitnessApprox["fitness"].values)

    rdRTF_orig_mean = rdRTF_baseline["fitness"].mean()
    rdRTF_list = []
    rdRTF_list.append(rdRTF_fitness["fitness"].values)
    rdRTF_list.append(rdRTF_fitnessApprox["fitness"].values)

    f, (ax1, ax2, ax3, ax4) = plt.subplots(1, 4, sharey=True)
    # f.set_size_inches(6,4)
    # f.legend(handles=[blue_patch],loc='upper right')
    ax1.set_ylabel('Fitness')
    ax1.set_yticks([0.5, 0.6, 0.7, 0.8, 0.9, 1.0])
    ax1.set_ylim(0.49, 1.01)

    ax1.boxplot(bpi2012_list)
    ax1.axhline(bpi2012_orig_mean, color='b', linestyle='--')
    ax1.set_xticklabels(x_ticks_labels[:2], rotation='horizontal', fontsize=18)
    ax1.tick_params(length=6, width=2)
    ax1.tick_params(which='minor', length=4, width=1)
    ax1.set_title('BPI-12', fontsize=18)

    ax2.boxplot(bpi2014_list)
    ax2.axhline(bpi2014_orig_mean, color='b', linestyle='--')
    ax2.set_xticklabels(x_ticks_labels[:2], rotation='horizontal', fontsize=18)
    ax2.tick_params(length=6, width=2)
    ax2.tick_params(which='minor', length=4, width=1)
    ax2.set_title('BPI-14', fontsize=18)

    ax3.boxplot(road_traffic_list)
    ax3.axhline(road_traffic_orig_mean, color='b', linestyle='--')
    ax3.set_xticklabels(x_ticks_labels[:2], rotation='horizontal', fontsize=18)
    ax3.tick_params(length=6, width=2)
    ax3.tick_params(which='minor', length=4, width=1)
    ax3.set_title('RTF', fontsize=18)

    ax4.boxplot(rdRTF_list)
    ax4.axhline(rdRTF_orig_mean, color='b', linestyle='--')
    ax4.set_xticklabels(x_ticks_labels[:2], rotation='horizontal', fontsize=18)
    ax4.tick_params(length=6, width=2)
    ax4.tick_params(which='minor', length=4, width=1)
    ax4.set_title('RTFr', fontsize=18)
    #f.show()
    f.savefig("./real4_fitness.pdf", bbox_inches='tight')





def convert_to_log(input):
    to_return = []
    for x in input:
        to_return.append([log10(y) for y in x])
    return to_return


if __name__ == "__main__":
    main()
