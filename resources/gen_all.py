# Created by Rui Yang
# Date: Sep-10-2019
# The file will create a figure folder under the provided path. The figure folder stores all figures.

import pandas as pd
import sys
import argparse
import os
import matplotlib.pyplot as plt
from matplotlib.lines import Line2D
import numpy as np
import random

parser = argparse.ArgumentParser()
parser.add_argument("-d", "--dir_expr", required=False, help="the path of root directory of the experiment folder destination")
parser.add_argument("-min_line", "--min_line", help="Minimal number of data line to generate a plot", default=4)
parser.add_argument("-dpi", "--dpi", help="Figure dpi, default 100 (low)", default=400)
args = parser.parse_args()
# folder = '1568308891645_VARY_maxConcurrentRtn_R_11_C_6'
folder = args.dir_expr
min_line = args.min_line
fig_dpi = args.dpi
sub_folders = next(os.walk(folder))[1]

# Plot setting for different serial mechanism
color_dict = {'GSV': 'b', 'PSV' : 'g', 'EV' : 'c', 'WV' : 'y', 'LV' : 'm', 'LAZY_FCFS' : 'r', 'LAZY_PRIORITY': 'chartreuse', 'FCFSV': 'orange', 'LzPRIOTY': 'chartreuse'}
mark_dict = {'GSV': 's', 'PSV' : 'o', 'EV': 'd', 'WV': '^', 'LV': '<', 'LAZY_FCFS': '>', 'LAZY_PRIORITY': '*'}

def get_color(line_name):
  if line_name in color_dict:
    return color_dict[line_name]
  else:
    return 'k'
def get_marker(line_name):
  if line_name in mark_dict:
    return mark_dict[line_name]
  else:
    return random.randint(1, 4)

for sub_folder in sub_folders:
  # create figure folder
  if sub_folder == 'figure':
    continue
  figure_folder = folder + '/figure/' + sub_folder 
  sub_folder_fp = folder + '/' + sub_folder
  if not os.path.exists(figure_folder):
    os.makedirs(figure_folder)

  for _, _, files in os.walk(sub_folder_fp):
    for dat_file in files:
      dat_file_fp = sub_folder_fp + '/' + dat_file
      df = pd.read_csv(dat_file_fp, sep='\t')
      if len(df.index) is 0:
        continue
      num_plot_lines = int(len(df.columns) / 2)
      plot_line_names = df.columns[1::2]
      data_names = df.columns[0::2]
      x_min = x_max = []
      for j in range(num_plot_lines):
        # Get axis data
        line_name = plot_line_names[j]
        x_axis = df[data_names[j]].to_list()
        
        y_axis = df[line_name].to_list()
        y_axis = [item for item in y_axis if item > 0]
        if len(y_axis) == 0:
          continue

        # Start ploting this line
        len_valid = len(y_axis)
        ax = plt.gca()
        # plt_line = Line2D(x_axis[:len_valid], y_axis[:len_valid], color=get_color(line_name), label=line_name, marker=get_marker(line_name))
        plt_line = Line2D(x_axis[:len_valid], y_axis[:len_valid], color=get_color(line_name), label=line_name)
        x_min.append(min(x_axis))
        x_max.append(max(x_axis))
        ax.add_line(plt_line)
        ax.legend()
        ax.set_ylabel('CDF')
      if min(x_min) != max(x_max):
        plt.xlim(min(x_min), max(x_max))
      else:
        plt.xlim(min(x_min) - 1, max(x_max) + 1)
      plt.ylim(0, 1) # I'm assuming we are all plotting CDF
      plt.title(os.path.splitext(dat_file)[0])

      # Plot saving
      figname = figure_folder + '/' + os.path.splitext(dat_file)[0] + '.png'
      # print(figname)
      if os.path.isfile(figname):
        os.remove(figname)
      plt.savefig(figname, dpi=fig_dpi)
      plt.clf()

####################################
#         Overall figures          #
####################################

fname = [ fi for fi in next(os.walk(folder))[2] if fi.endswith(".dat") ][0]
overall_fname = folder + '/' + fname
overall_figure_folder = folder + '/figure/overall/'
if not os.path.exists(overall_figure_folder):
    os.makedirs(overall_figure_folder)

def get_data_line_range(overall_fname):
  with open(overall_fname) as fin:
    for num, line in enumerate(fin):
      if 'Summary-Start' in line:
        start_line = num
      if 'Summary-End' in line:
        return [start_line, num]

# Data locating
start_line, stop_line = get_data_line_range(overall_fname)
df = pd.read_csv(overall_fname, sep='\t', skiprows=start_line + 1, nrows=stop_line - start_line - 2)
df.drop(df.columns[len(df.columns)-1], axis=1, inplace=True)
columns = df.columns
num_col = len(columns)
graph_cat_for_col = [col.split(':')[0] for col in columns]
graph_cat_for_col.remove('G0')
graphs = list(dict.fromkeys(graph_cat_for_col))

# Get common x-axis data
g0 = df[df.filter(regex='G0', axis=1).columns[0]].to_list()

for graph in graphs:

  data = df.filter(regex=graph+':', axis=1)
  y_min = []
  y_max = []
  for line_name in data.columns:
    legend_name = line_name.split(':')[1].split('_')[0]
    y_axis = df[line_name].to_list()
    
    # Start ploting this line
    ax = plt.gca()
    plt_line = Line2D(g0, y_axis, color=get_color(legend_name), label=legend_name, marker=get_marker(legend_name))
    y_min.append(min(y_axis))
    y_max.append(max(y_axis))
    ax.add_line(plt_line)
    ax.legend()

  # Set axis range
  if min(y_min) != max(y_max):
    plt.ylim(min(y_min), max(y_max))
  else:
    plt.ylim(min(y_min) - 1, max(y_max) + 1)
  plt.xlim(min(g0) - 1, max(g0) + 1)
  plt.title(line_name)

  # Plot saving
  figname = overall_figure_folder + line_name.split('_')[1] + '.png'
  if os.path.isfile(figname):
    os.remove(figname)
  plt.savefig(figname, dpi=fig_dpi)
  plt.clf()


