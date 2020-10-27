import pandas as pd
import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
import statistics

def main():
        font = {'font.family' : 'normal',
        #'font.weight' : 'bold',
        'font.size'   : 18}
        plt.rcParams.update(font)

        blue_patch = mpatches.Patch(color='blue', label='Original')
        x_ticks_labels = ['PrA','PrB','PrC','PrD', 'PrE', 'PrF', 'PrG']


        #build a dataframe for each result file
        #original
        pra_baseline=pd.read_csv("./results/prAm6_baseline.csv", sep=';')
        prb_baseline=pd.read_csv("./results/prBm6_baseline.csv", sep=';')
        prc_baseline=pd.read_csv("./results/prCm6_baseline.csv", sep=';')
        prd_baseline=pd.read_csv("./results/prDm6_baseline.csv", sep=';')
        pre_baseline=pd.read_csv("./results/prEm6_baseline.csv", sep=';')
        prf_baseline=pd.read_csv("./results/prFm6_baseline.csv", sep=';')
        prg_baseline=pd.read_csv("./results/prGm6_baseline.csv", sep=';')

        #fitness
        pra_synthetic=pd.read_csv("./results/prAm6_synthetic.csv", sep=';')
        prb_synthetic=pd.read_csv("./results/prBm6_synthetic.csv", sep=';')
        prc_synthetic=pd.read_csv("./results/prCm6_synthetic.csv", sep=';')
        prd_synthetic=pd.read_csv("./results/prDm6_synthetic.csv", sep=';')
        pre_synthetic=pd.read_csv("./results/prEm6_synthetic.csv", sep=';')
        prf_synthetic=pd.read_csv("./results/prFm6_synthetic.csv", sep=';')
        prg_synthetic=pd.read_csv("./results/prGm6_synthetic.csv", sep=';')

        print("Loaded input files")
        print()

        #set parameters for original-to-implementation comparison for all three logs
        chosen_delta=0.05
        chosen_alpha=0.99
        chosen_epsilon=0.01

        print("###used parameters for Evaluation###")
        print("Delta:    " + str(chosen_delta))
        print("Alpha:    " + str(chosen_alpha))
        print("Epsilon:  " + str(chosen_epsilon))
        print()

        #long copy and paste list that creates the dataframes for the plots
        pra_synthetic=pra_synthetic.loc[(pra_synthetic["delta"]==chosen_delta) & (pra_synthetic["alpha"]==chosen_alpha) & (pra_synthetic["epsilon"]==chosen_epsilon)]
        prb_synthetic=prb_synthetic.loc[(prb_synthetic["delta"]==chosen_delta) & (prb_synthetic["alpha"]==chosen_alpha) & (prb_synthetic["epsilon"]==chosen_epsilon)]
        prc_synthetic=prc_synthetic.loc[(prc_synthetic["delta"]==chosen_delta) & (prc_synthetic["alpha"]==chosen_alpha) & (prc_synthetic["epsilon"]==chosen_epsilon)]
        prd_synthetic=prd_synthetic.loc[(prd_synthetic["delta"]==chosen_delta) & (prd_synthetic["alpha"]==chosen_alpha) & (prd_synthetic["epsilon"]==chosen_epsilon)]
        pre_synthetic=pre_synthetic.loc[(pre_synthetic["delta"]==chosen_delta) & (pre_synthetic["alpha"]==chosen_alpha) & (pre_synthetic["epsilon"]==chosen_epsilon)]
        prf_synthetic=prf_synthetic.loc[(prf_synthetic["delta"]==chosen_delta) & (prf_synthetic["alpha"]==chosen_alpha) & (prf_synthetic["epsilon"]==chosen_epsilon)]
        prg_synthetic=prg_synthetic.loc[(prg_synthetic["delta"]==chosen_delta) & (prg_synthetic["alpha"]==chosen_alpha) & (prg_synthetic["epsilon"]==chosen_epsilon)]
        #plot computing time comparisons
        traces_list=[]
        traces_list.append(pra_synthetic["logSize"].values/pra_baseline["logSize"].mean())
        traces_list.append(prb_synthetic["logSize"].values/prb_baseline["logSize"].mean())
        traces_list.append(prc_synthetic["logSize"].values/prc_baseline["logSize"].mean())
        traces_list.append(prd_synthetic["logSize"].values/prd_baseline["logSize"].mean())
        traces_list.append(pre_synthetic["logSize"].values/pre_baseline["logSize"].mean())
        traces_list.append(prf_synthetic["logSize"].values/prf_baseline["logSize"].mean())
        traces_list.append(prg_synthetic["logSize"].values/prg_baseline["logSize"].mean())

        time_list=[]
        time_list.append(pra_synthetic["time"].values/pra_baseline["time"].mean())
        time_list.append(prb_synthetic["time"].values/prb_baseline["time"].mean())
        time_list.append(prc_synthetic["time"].values/prc_baseline["time"].mean())
        time_list.append(prd_synthetic["time"].values/prd_baseline["time"].mean())
        time_list.append(pre_synthetic["time"].values/pre_baseline["time"].mean())
        time_list.append(prf_synthetic["time"].values/prf_baseline["time"].mean())
        time_list.append(prg_synthetic["time"].values/prg_baseline["time"].mean())

        fitness_list=[]
        fitness_list.append(pra_synthetic["fitness"].values)
        fitness_list.append(prb_synthetic["fitness"].values)
        fitness_list.append(prc_synthetic["fitness"].values)
        fitness_list.append(prd_synthetic["fitness"].values)
        fitness_list.append(pre_synthetic["fitness"].values)
        fitness_list.append(prf_synthetic["fitness"].values)
        fitness_list.append(prg_synthetic["fitness"].values)

        orig_fitness_list=[0]
        orig_fitness_list.append(pra_baseline["fitness"].mean())
        orig_fitness_list.append(prb_baseline["fitness"].mean())
        orig_fitness_list.append(prc_baseline["fitness"].mean())
        orig_fitness_list.append(prd_baseline["fitness"].mean())
        orig_fitness_list.append(pre_baseline["fitness"].mean())
        orig_fitness_list.append(prf_baseline["fitness"].mean())
        orig_fitness_list.append(prg_baseline["fitness"].mean())
        
        #plot traces
        f, (ax1) = plt.subplots(1, 1, sharey=True)
        #ax1.set_yscale('log')
        ax1.set_ylabel('Fraction of Traces Sampled')
        # ax1.axhline(1, color='b', linestyle='--')
        ax1.boxplot(traces_list)
        ax1.set_xticklabels(x_ticks_labels, rotation=0, fontsize=18)
        ax1.tick_params(length=6, width=2)
        ax1.tick_params(which='minor', length=4, width=1)
        #f.show()
        f.savefig("./benchmark_traces.pdf", bbox_inches='tight')

        print("traces")
        for x in traces_list:
                print(statistics.mean(x))


        #plot time
        f, (ax1) = plt.subplots(1, 1, sharey=True)
        #ax1.set_yscale('log')
        ax1.set_ylabel('Runtime (relative)')
        ax1.set_ylim(0.0, 1.05)
        ax1.set_yticks([0.0, 0.20, 0.4, 0.6, 0.8, 1.0])
        ax1.set_yticklabels(["0%", "20%", "40%", "60%", "80%", "100%"])
        # ax1.axhline(1, color='b', linestyle='--')
        ax1.boxplot(time_list)
        ax1.set_xticklabels(x_ticks_labels, rotation=0, fontsize=18)
        ax1.tick_params(length=6, width=2)
        ax1.tick_params(which='minor', length=4, width=1)
        #f.show()
        f.savefig("./benchmark_computing_time.pdf", bbox_inches='tight')

        print("times")
        for x in time_list:
                print(statistics.mean(x))


        #plot fitness
        f, (ax1) = plt.subplots(1, 1, sharey=True)
        ax1.set_ylabel('Fitness')
        ax1.set_yticks([0.5,0.6,0.7, 0.8, 0.9, 1.0])
        ax1.set_ylim(0.48, 1.02)
        ax1.set_xlim(0.5, 7.5)
        ax1.boxplot(fitness_list)
        ax1.plot(orig_fitness_list,'bx',markersize=14, markeredgewidth=1)
        ax1.set_xticklabels(x_ticks_labels, rotation=0, fontsize=18)
        ax1.tick_params(length=6, width=2)
        ax1.tick_params(which='minor', length=4, width=1)
        #f.show()
        f.savefig("./benchmark_fitness.pdf", bbox_inches='tight')

if __name__ == "__main__":
    main()
