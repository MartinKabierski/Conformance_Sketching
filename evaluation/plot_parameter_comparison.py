import os
import pandas as pd
import matplotlib.pyplot as plt
import statistics

def main(input_name):
    font = {'font.family': 'normal',
            # 'font.weight' : 'bold',
            'font.size': 18}
    plt.rcParams.update(font)

    # read in data frame
    bpi2012 = pd.read_csv(os.path.join(".", "src", str(input_name) + "_fitness.csv"), sep=';')
    bpi2012approx = pd.read_csv(os.path.join(".", "src", str(input_name) + "_fitnessApprox.csv"), sep=';')
    bpi2012_orig = pd.read_csv(os.path.join(".", "src", str(input_name) + "_baseline.csv"), sep=';')
    bpi2012_orig_mean = bpi2012_orig["fitness"].mean()
    bpi2012_orig_time_mean = bpi2012_orig["time"].mean()
    bpi2012_orig_traces = bpi2012_orig["logSize"].mean()

    print("Loaded input files")
    print()

    # set anchor parameter
    chosen_delta = 0.01
    chosen_alpha = 0.99
    chosen_epsilon = 0.01
    chosen_k = 0.2
    # heuristics="NONALIGNING_KNOWN"

    # set list of used parameters
    delta_list = [0.01, 0.05, 0.1]
    alpha_list = [0.9, 0.95, 0.99]
    epsilon_list = [0.01, 0.05, 0.1]
    k_list = [0.1, 0.2, 0.3]
    print("###used parameters for Evaluation###")
    print("Delta:    " + str(delta_list))
    print("Alpha:    " + str(alpha_list))
    print("Epsilon:  " + str(epsilon_list))
    print("K      :  " + str(k_list))

    print()
    # fitness with approximation
    # get value lists for one changing parameter at a time
    # change delta
    delta_time_list = []
    delta_trace_list = []
    delta_fitness_list = []

    alpha_time_list = []
    alpha_trace_list = []
    alpha_fitness_list = []

    epsilon_time_list = []
    epsilon_trace_list = []
    epsilon_fitness_list = []

    k_time_list = []
    k_trace_list = []
    k_fitness_list = []
    for i in range(0, 3):
        bpi2012_delta = bpi2012approx.loc[
            (bpi2012approx["delta"] == float(delta_list[i])) & (bpi2012approx["alpha"] == chosen_alpha) & (
                        bpi2012approx["epsilon"] == chosen_epsilon) & (bpi2012approx["k"] == chosen_k)]
        delta_time_list.append(bpi2012_delta["time"].values)
        delta_trace_list.append(bpi2012_delta["logSize"].values)
        delta_fitness_list.append(bpi2012_delta["fitness"].values)

        bpi2012_alpha = bpi2012approx.loc[
            (bpi2012approx["delta"] == chosen_delta) & (bpi2012approx["alpha"] == float(alpha_list[i])) & (
                        bpi2012approx["epsilon"] == chosen_epsilon) & (bpi2012approx["k"] == chosen_k)]
        alpha_time_list.append(bpi2012_alpha["time"].values)
        alpha_trace_list.append(bpi2012_alpha["logSize"].values)
        alpha_fitness_list.append(bpi2012_alpha["fitness"].values)

        bpi2012_epsilon = bpi2012approx.loc[
            (bpi2012approx["delta"] == chosen_delta) & (bpi2012approx["alpha"] == chosen_alpha) & (
                        bpi2012approx["epsilon"] == float(epsilon_list[i])) & (bpi2012approx["k"] == chosen_k)]
        epsilon_time_list.append(bpi2012_epsilon["time"].values)
        epsilon_trace_list.append(bpi2012_epsilon["logSize"].values)
        epsilon_fitness_list.append(bpi2012_epsilon["fitness"].values)

        bpi2012_k = bpi2012approx.loc[
            (bpi2012approx["delta"] == chosen_delta) & (bpi2012approx["alpha"] == chosen_alpha) & (
                        bpi2012approx["epsilon"] == chosen_epsilon) & (bpi2012approx["k"] == float(k_list[i]))]
        k_time_list.append(bpi2012_k["time"].values)
        k_trace_list.append(bpi2012_k["logSize"].values)
        k_fitness_list.append(bpi2012_k["fitness"].values)

    # make values relative
    for i in range(len(alpha_trace_list)):
        alpha_trace_list[i] = alpha_trace_list[i] / bpi2012_orig_traces
    for i in range(len(epsilon_trace_list)):
        epsilon_trace_list[i] = epsilon_trace_list[i] / bpi2012_orig_traces
    for i in range(len(delta_trace_list)):
        delta_trace_list[i] = delta_trace_list[i] / bpi2012_orig_traces
    for i in range(len(k_trace_list)):
        k_trace_list[i] = k_trace_list[i] / bpi2012_orig_traces

    for i in range(len(alpha_time_list)):
        alpha_time_list[i] = alpha_time_list[i] / bpi2012_orig_time_mean
    for i in range(len(epsilon_time_list)):
        epsilon_time_list[i] = epsilon_time_list[i] / bpi2012_orig_time_mean
    for i in range(len(delta_time_list)):
        delta_time_list[i] = delta_time_list[i] / bpi2012_orig_time_mean
    for i in range(len(k_time_list)):
        k_time_list[i] = k_time_list[i] / bpi2012_orig_time_mean

    # generate plots
    # for runtime
    f, (ax1, ax2, ax3, ax4) = plt.subplots(1, 4, sharey=True)
    ax1.set_ylabel('Runtime (relative)', fontsize=18)
    ax1.set_xlabel('Delta', fontsize=18)
    ax1.boxplot(delta_time_list)
    ax1.set_xticklabels(delta_list, rotation='vertical', fontsize=14)

    ax2.set_xlabel('Alpha', fontsize=18)
    ax2.boxplot(alpha_time_list)
    ax2.set_xticklabels(alpha_list, rotation='vertical', fontsize=14)

    ax3.set_xlabel('Epsilon', fontsize=18)
    ax3.boxplot(epsilon_time_list)
    ax3.set_xticklabels(epsilon_list, rotation='vertical', fontsize=14)

    ax4.set_xlabel('k', fontsize=18)
    ax4.xaxis.labelpad = 12
    ax4.boxplot(k_time_list)
    ax4.set_xticklabels(k_list, rotation='vertical', fontsize=14)
    # f.show()
    f.savefig(os.path.join(".", input_name, str(input_name) + "_param_time_Approx_k" + str(chosen_k).replace(".","") + ".pdf"),
              bbox_inches='tight')

    # for log size
    f, (ax1, ax2, ax3, ax4) = plt.subplots(1, 4, sharey=True)
    ax1.set_ylabel('Sampled traces', fontsize=18)
    ax1.set_yscale('log')
    ax1.set_ylim(0.0001, 1.3)
    ax1.set_yticks([0.001, 0.01, 0.1, 1.0])
    ax1.set_yticklabels(["0.1%", "1%", "10%", "100%"])

    # ax1.get_yaxis().set_major_formatter(matplotlib.ticker.ScalarFormatter())
    ax1.set_xlabel('Delta', fontsize=18)
    ax1.boxplot(delta_trace_list)
    ax1.set_xticklabels(delta_list, rotation='vertical', fontsize=14)

    ax2.set_xlabel('Alpha', fontsize=18)
    ax2.boxplot(alpha_trace_list)
    ax2.set_xticklabels(alpha_list, rotation='vertical', fontsize=14)

    ax3.set_xlabel('Epsilon', fontsize=18)
    ax3.boxplot(epsilon_trace_list)
    ax3.set_xticklabels(epsilon_list, rotation='vertical', fontsize=14)

    ax4.set_xlabel('k', fontsize=18)
    ax4.xaxis.labelpad = 12
    ax4.boxplot(k_trace_list)
    ax4.set_xticklabels(k_list, rotation='vertical', fontsize=14)
    # f.show()
    f.savefig(os.path.join(".", input_name, str(input_name) + "_param_traces_Approx_k" + str(chosen_k).replace(".","") + ".pdf"),
              bbox_inches='tight')

    # for Fitness
    f, (ax1, ax2, ax3, ax4) = plt.subplots(1, 4, sharey=True)
    ax1.set_ylabel('Fitness', fontsize=18)
    ax1.set_yticks([0.5, 0.6, 0.7, 0.8, 0.9, 1.0])
    ax1.set_ylim(0.49, 1.01)
    ax1.axhline(bpi2012_orig_mean, color='b', linestyle='--')

    ax1.set_xlabel('Delta', fontsize=18)
    ax1.boxplot(delta_fitness_list)
    ax1.set_xticklabels(delta_list, rotation='vertical', fontsize=14)
    print("Delta w. Approx")
    print(statistics.mean(delta_fitness_list[0]))
    print(statistics.mean(delta_fitness_list[1]))
    print(statistics.mean(delta_fitness_list[2]))

    print(delta_fitness_list[0])

    ax2.set_xlabel('Alpha', fontsize=18)
    ax2.boxplot(alpha_fitness_list)
    ax2.set_xticklabels(alpha_list, rotation='vertical', fontsize=14)
    ax2.axhline(bpi2012_orig_mean, color='b', linestyle='--')
    print(statistics.mean(alpha_fitness_list[0]))

    ax3.set_xlabel('Epsilon', fontsize=18)
    ax3.boxplot(epsilon_fitness_list)
    ax3.set_xticklabels(epsilon_list, rotation='vertical', fontsize=14)
    ax3.axhline(bpi2012_orig_mean, color='b', linestyle='--')

    ax4.set_xlabel('k', fontsize=18)
    ax4.xaxis.labelpad = 12
    ax4.boxplot(k_fitness_list)
    ax4.set_xticklabels(k_list, rotation='vertical', fontsize=14)
    ax4.axhline(bpi2012_orig_mean, color='b', linestyle='--')
    print("K")
    print(statistics.mean(k_fitness_list[0]))
    print(statistics.mean(k_fitness_list[1]))
    print(statistics.mean(k_fitness_list[2]))

    # f.show()
    f.savefig(os.path.join(".", input_name, str(input_name) + "_param_fitness_Approx_k" + str(chosen_k).replace(".","") + ".pdf"),
              bbox_inches='tight')

    # fitness w/o approximation
    # get value lists for one changing parameter at a time
    # change delta
    delta_time_list = []
    delta_trace_list = []
    delta_fitness_list = []

    alpha_time_list = []
    alpha_trace_list = []
    alpha_fitness_list = []

    epsilon_time_list = []
    epsilon_trace_list = []
    epsilon_fitness_list = []

    k_time_list = []
    k_trace_list = []
    k_fitness_list = []
    for i in range(0, 3):
        bpi2012_delta = bpi2012.loc[(bpi2012["delta"] == float(delta_list[i])) & (bpi2012["alpha"] == chosen_alpha) & (
                    bpi2012["epsilon"] == chosen_epsilon)]
        delta_time_list.append(bpi2012_delta["time"].values)
        delta_trace_list.append(bpi2012_delta["logSize"].values)
        delta_fitness_list.append(bpi2012_delta["fitness"].values)

        bpi2012_alpha = bpi2012.loc[(bpi2012["delta"] == chosen_delta) & (bpi2012["alpha"] == float(alpha_list[i])) & (
                    bpi2012["epsilon"] == chosen_epsilon)]
        alpha_time_list.append(bpi2012_alpha["time"].values)
        alpha_trace_list.append(bpi2012_alpha["logSize"].values)
        alpha_fitness_list.append(bpi2012_alpha["fitness"].values)

        bpi2012_epsilon = bpi2012.loc[(bpi2012["delta"] == chosen_delta) & (bpi2012["alpha"] == chosen_alpha) & (
                    bpi2012["epsilon"] == float(epsilon_list[i]))]
        epsilon_time_list.append(bpi2012_epsilon["time"].values)
        epsilon_trace_list.append(bpi2012_epsilon["logSize"].values)
        epsilon_fitness_list.append(bpi2012_epsilon["fitness"].values)

        bpi2012_k = bpi2012.loc[(bpi2012["delta"] == chosen_delta) & (bpi2012["alpha"] == chosen_alpha) & (
                    bpi2012["epsilon"] == chosen_epsilon)]
        k_time_list.append(bpi2012_k["time"].values)
        k_trace_list.append(bpi2012_k["logSize"].values)
        k_fitness_list.append(bpi2012_k["fitness"].values)

    # make values relative
    for i in range(len(alpha_trace_list)):
        alpha_trace_list[i] = alpha_trace_list[i] / bpi2012_orig_traces
    for i in range(len(epsilon_trace_list)):
        epsilon_trace_list[i] = epsilon_trace_list[i] / bpi2012_orig_traces
    for i in range(len(delta_trace_list)):
        delta_trace_list[i] = delta_trace_list[i] / bpi2012_orig_traces
    for i in range(len(k_trace_list)):
        k_trace_list[i] = k_trace_list[i] / bpi2012_orig_traces

    for i in range(len(alpha_time_list)):
        alpha_time_list[i] = alpha_time_list[i] / bpi2012_orig_time_mean
    for i in range(len(epsilon_time_list)):
        epsilon_time_list[i] = epsilon_time_list[i] / bpi2012_orig_time_mean
    for i in range(len(delta_time_list)):
        delta_time_list[i] = delta_time_list[i] / bpi2012_orig_time_mean
    for i in range(len(k_time_list)):
        k_time_list[i] = k_time_list[i] / bpi2012_orig_time_mean

    # generate plots
    # for runtime
    f, (ax1, ax2, ax3) = plt.subplots(1, 3, sharey=True)
    ax1.set_ylabel('Runtime (relative)', fontsize=18)
    ax1.set_xlabel('Delta', fontsize=18)
    ax1.boxplot(delta_time_list)
    ax1.set_xticklabels(delta_list, rotation='vertical', fontsize=14)

    ax2.set_xlabel('Alpha', fontsize=18)
    ax2.boxplot(alpha_time_list)
    ax2.set_xticklabels(alpha_list, rotation='vertical', fontsize=14)

    ax3.set_xlabel('Epsilon', fontsize=18)
    ax3.boxplot(epsilon_time_list)
    ax3.set_xticklabels(epsilon_list, rotation='vertical', fontsize=14)

    # f.show()
    # f.savefig("./bpi2012_param_time.pdf", bbox_inches='tight')
    f.savefig(os.path.join(".", input_name, str(input_name) + "_param_time.pdf"), bbox_inches='tight')

    # for log size
    f, (ax1, ax2, ax3) = plt.subplots(1, 3, sharey=True)
    ax1.set_ylabel('Sampled traces', fontsize=18)
    ax1.set_yscale('log')
    ax1.set_ylim(0.0001, 1.3)
    ax1.set_yticks([0.001, 0.01, 0.1, 1.0])
    ax1.set_yticklabels(["0.1%", "1%", "10%", "100%"])

    # ax1.get_yaxis().set_major_formatter(matplotlib.ticker.ScalarFormatter())
    ax1.set_xlabel('Delta', fontsize=18)
    ax1.boxplot(delta_trace_list)
    ax1.set_xticklabels(delta_list, rotation='vertical', fontsize=14)

    ax2.set_xlabel('Alpha', fontsize=18)
    ax2.boxplot(alpha_trace_list)
    ax2.set_xticklabels(alpha_list, rotation='vertical', fontsize=14)

    ax3.set_xlabel('Epsilon', fontsize=18)
    ax3.boxplot(epsilon_trace_list)
    ax3.set_xticklabels(epsilon_list, rotation='vertical', fontsize=14)

    # f.show()
    # f.savefig("./bpi2012_param_traces.pdf", bbox_inches='tight')
    f.savefig(os.path.join(".", input_name, str(input_name) + "_param_traces.pdf"), bbox_inches='tight')

    # for Fitness
    f, (ax1, ax2, ax3) = plt.subplots(1, 3, sharey=True)
    ax1.set_ylabel('Fitness', fontsize=18)
    ax1.set_yticks([0.5, 0.6, 0.7, 0.8, 0.9, 1.0])
    ax1.set_ylim(0.49, 1.01)
    ax1.axhline(bpi2012_orig_mean, color='b', linestyle='--')

    ax1.set_xlabel('Delta', fontsize=18)
    ax1.boxplot(delta_fitness_list)
    ax1.set_xticklabels(delta_list, rotation='vertical', fontsize=14)
    print(delta_fitness_list[2])

    ax2.set_xlabel('Alpha', fontsize=18)
    ax2.boxplot(alpha_fitness_list)
    ax2.set_xticklabels(alpha_list, rotation='vertical', fontsize=14)
    ax2.axhline(bpi2012_orig_mean, color='b', linestyle='--')

    ax3.set_xlabel('Epsilon', fontsize=18)
    ax3.boxplot(epsilon_fitness_list)
    ax3.set_xticklabels(epsilon_list, rotation='vertical', fontsize=14)
    ax3.axhline(bpi2012_orig_mean, color='b', linestyle='--')

    # f.show()
    # f.savefig("./bpi2012_param_fitness.pdf", bbox_inches='tight')
    f.savefig(os.path.join(".", input_name, str(input_name) + "_param_fitness.pdf"), bbox_inches='tight')


if __name__ == "__main__":
    main("BPI_Challenge_2012")
