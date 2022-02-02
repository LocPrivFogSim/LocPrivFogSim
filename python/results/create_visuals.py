import numpy as np
from numpy import genfromtxt
import matplotlib.pyplot as plt
import pandas as pd
import os


path_nearest_results = './nearest.csv'


def visuals_for_nearest():
    
    data = pd.read_csv(path_nearest_results,sep=",",index_col=None)
    data_grouped = data.groupby('rate').mean()

    plt.figure(1)
    plt.subplot()
    plt.plot(data_grouped.index, data_grouped['total_correctness'], c='b', label='total_correctness')
    plt.title("Strategy: Nearest")
    plt.xlabel('rate of compromised fog nodes')
    plt.ylabel('correctness in <unit>')
    plt.legend()


    plt.figure(2)
    plt.subplot()
    plt.plot(data_grouped.index, data_grouped['avg_correctness'], c='b', label='avg_correctness')
    plt.title("Strategy: Nearest")
    plt.xlabel('rate of compromised fog nodes')
    plt.ylabel('correctness in <unit>')
    plt.legend()

    plt.show()




def main():
  visuals_for_nearest()

if __name__ == '__main__':
    main()