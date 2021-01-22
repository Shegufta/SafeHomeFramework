import pandas as pd
import sys
import argparse
import os
import matplotlib.pyplot as plt
from matplotlib.lines import Line2D
import matplotlib
import random
import numpy as np
from scipy.signal import savgol_filter
import time


parser = argparse.ArgumentParser()
parser.add_argument("-d", "--deployment", required=False, help="path for morning scenario", default="morning")
parser.add_argument("-m", "--morning", required=False, help="path for morning scenario", default="morning")
parser.add_argument("-p", "--party", required=False, help="path for party scenario", default="party")
parser.add_argument("-f", "--factory", required=False, help="path for factory scenario", default="factory")
parser.add_argument("-marker", "--marker", required=False, help="whether to draw marker on the plot", default=False)
args = parser.parse_args()

scenario_folders = [args.deployment, args.morning, args.party, args.factory]

matplotlib.font_manager._rebuild()
matplotlib.rcParams['font.family'] = 'Times New Roman'
matplotlib.rcParams['font.weight'] = 'bold'
# plt.rcParams['figure.constrained_layout.use'] = True

color_dict = {}
color_dict = {'GSV': '#2a70ba', 'PSV' : '#f7c143', 'EV' : '#4ead5b', 'WV' : '#b96029', 'LAZY_FCFS' : 'r', 'LAZY_PRIORITY': 'chartreuse', 'FCFSV': 'orange', 'LzPRIOTY': 'chartreuse'}
color_dict = {'GSV': '#2a70ba', 'PSV' : '#4b0082', 'EV' : '#026440', 'WV' : '#a50000', 'LAZY_FCFS' : 'r', 'LAZY_PRIORITY': 'chartreuse', 'FCFSV': 'orange', 'LzPRIOTY': 'chartreuse'}

mark_dict = {'EV': '^', 'WV': 'P', 'PSV': 'X', 'GSV':'o'}
# linestyle_dict = {'PSV': '--', 'EV': '-.', 'WV': (0, (8, 10)), 'GSV' : '-'}
linestyle_dict = {'PSV': '--', 'EV': '-', 'WV': (0, (1, 1)), 'GSV' : '-.'}
zorder = {'PSV': 7, 'EV':5, 'WV': 6, 'GSV':3}
# linestyle_dict = {'PSV': '--', 'EV': '-.', 'WV': ':', 'GSV' : '-'}

markfacecolor_dict = {'EV' : '#ffffff', 'WV': '#8b0000', 'GSV': '#ffffff', 'WV_d': '#000000'}
# markfacecolor_dict = {'EV': '#60803f', 'WV': '#b76b38'}
markedgecolor_dict = {'EV': '#026440', 'WV': '#a50000', 'GSV': '#2a70ba'}
# markedgecolor_dict = {'EV' : '#4ead5b', 'WV': '#cd783f'}
markedgewidth_dict = {'EV': 3, 'GSV': 3, 'WV' : 0}
linewidth_dict = {}
target_lines = ['GSV', 'EV', 'WV', 'PSV']

bar_color = ['#ffffff', '#DF8244', '#4EAD5B', '#B99130', '#E7E6E6']
bar_edgecolor = ['#3f74b1', '#ffffff', '#4EAD5B', '#ffffff', '#000000']
bar_pattern = ['///', '.', '\\', '|||', '+']

# scenario_folders = ['scenario/1001-prototype-morning', 'scenario/1006-morning', 'scenario/1006-party', 'scenario/1006-factory']
figs = ['E2E_RTN_TIME.dat', 'ISVLTN5_RTN_LIFESPAN_COLLISION_PERCENT.dat', 'PARALLEL_DELTA.dat']
scenario_label = ['deployment', 'morning', 'party', 'factory']

x_labels = {
  'WAIT_TIME.dat' : 'Wait Time (seconds)', 
  'ISVLTN5_RTN_LIFESPAN_COLLISION_PERCENT.dat' : 'Temporary Incongruence (%)',
  'ORDERR_MISMATCH_BUBBLE.dat' : 'Order Mismatch (%)',
  'PARALLEL_DELTA.dat' : 'Parallelism Level',
  'E2E_RTN_TIME.dat' : 'End to End Latency (seconds)'}
y_mins = {'ISVLTN5_RTN_LIFESPAN_COLLISION_PERCENT.dat' : 0.4}
x_max_take = "value"
x_max_percentages = {'E2E_RTN_TIME.dat' : 0.1}
x_maxes = {'E2E_RTN_TIME.dat' : 10000}
x_mins = {'E2E_RTN_TIME.dat' : 1}
x_scales = {'E2E_RTN_TIME.dat' : 'log'}

fname_to_ind = {'E2E_RTN_TIME.dat': 0, 'ISVLTN5_RTN_LIFESPAN_COLLISION_PERCENT.dat': 1, 'PARALLEL_DELTA.dat': 2}
scena_to_ind = {'deployment':0 , 'morning': 1, 'party': 2, 'factory': 3}
tick_info = {
  (0, 0): {'xlim' : [1, 10000, [1, 10, 100, 1000, 10000]], 'ylim' : [0, 1, [0, 0.5, 1.0]]},
  (0, 1): {'xlim' : [1, 10000, [1, 10, 100, 1000, 10000]], 'ylim' : [0, 1, [0, 0.5, 1.0]]},
  (0, 2): {'xlim' : [1, 10000, [1, 10, 100, 1000, 10000]], 'ylim' : [0, 1, [0, 0.5, 1.0]]},
  (0, 3): {'xlim' : [1, 10000, [1, 10, 100, 1000, 10000]], 'ylim' : [0, 1, [0, 0.5, 1.0]]},
  (1, 0): {'xlim' : [-0.2, 100, [0, 25, 50, 75, 100]], 'ylim' : [0.4, 1.0, [0.4, 0.7, 1.0]]},
  (1, 1): {'xlim' : [-0.2, 100, [0, 25, 50, 75, 100]], 'ylim' : [0.4, 1.0, [0.4, 0.7, 1.0]]},
  (1, 2): {'xlim' : [-0.2, 100, [0, 25, 50, 75, 100]], 'ylim' : [0.4, 1.0, [0.4, 0.7, 1.0]]},
  (1, 3): {'xlim' : [-0.2, 100, [0, 25, 50, 75, 100]], 'ylim' : [0.4, 1.0, [0.4, 0.7, 1.0]]},
  (2, 0): {'xlim' : [-0.02, 10, [0, 5, 10]], 'ylim' : [0, 1, [0, 0.5, 1.0]]},
  (2, 1): {'xlim' : [-0.02, 10, [0, 5, 10]], 'ylim' : [0, 1, [0, 0.5, 1.0]]},
  (2, 2): {'xlim' : [-0.015, 6, [0, 2, 4, 6]], 'ylim' : [0, 1, [0, 0.5, 1.0]]},
  (2, 3): {'xlim' : [-0.18, 80, [0, 20, 40, 60, 80]], 'ylim' : [0, 1, [0, 0.5, 1.0]]}
}

marker_pos = {}
if args.marker: 
  marker_pos = {
    (0, 0): [True, [200], [140], [95], [21]],
    (0, 1): [True, [400], [280], [188], [25]],
    (0, 2): [True, [100], [300], [48], [7]],
    (0, 3): [True, [150], [420], [300], [235]],
    (1, 0): [True, [1], [1], [18], [28]],
    (1, 1): [True, [1], [1], [25], [40]],
    (1, 2): [True, [1], [1], [16], [30]],
    (1, 3): [True, [1], [1], [480], [380]],
    (2, 0): [False],
    (2, 1): [False],
    (2, 2): [False],
    (2, 3): [False],
  }

markersize = {'EV': 13, 'PSV': 15.5, 'WV': 15.5, 'GSV': 14}

labelfontsize = 29
tickfontsize = 30
legendfontsize = 24

def get_linewidth(line_name):
  if line_name in linewidth_dict:
    return linewidth_dict[line_name]
  elif line_name[:-2] in linewidth_dict:
    return linewidth_dict[line_name[:-2]]
  else:
    return 3.8

def get_zorder(line_name):
  if line_name in zorder:
    return zorder[line_name]
  elif line_name[:-2] in zorder:
    return zorder[line_name[:-2]] - 1
  else:
    return 2

def get_color(line_name):
  if line_name in color_dict:
    return color_dict[line_name]
  else:
    return 'k'

def get_marker(line_name):
  if line_name in mark_dict:
    return mark_dict[line_name]
  elif line_name[:-2] in mark_dict:
    return mark_dict[line_name[:-2]]
  else:
    return None

def get_markersize(line_name):
  if line_name in markersize:
    return markersize[line_name]
  elif line_name[:-2] in markersize:
    return markersize[line_name[:-2]]
  else:
    return None

def get_linestyle(line_name):
  if line_name in linestyle_dict:
    return linestyle_dict[line_name]
  else:
    return '-'

def get_markedgecolor(line_name):
  if line_name in markedgecolor_dict:
    return markedgecolor_dict[line_name]
  else:
    return get_color(line_name)

def get_markfacecolor(line_name):
  if line_name in markfacecolor_dict:
    return markfacecolor_dict[line_name]
  elif line_name[:-2] in markfacecolor_dict:
    return markfacecolor_dict[line_name[:-2]]
  else:
    return get_color(line_name)

def get_markedgewidth(line_name):
  if line_name in markedgewidth_dict:
    return markedgewidth_dict[line_name]
  elif line_name[:-2] in markedgewidth_dict:
    return markedgewidth_dict[line_name[:-2]]
  else:
    return 0

def get_xlabel(fname):
  if fname in x_labels:
    return x_labels[fname]
  else:
    return 'data'

def generate_subplot(full_fname, ax, fname, scenario, deployment=False):
  df = pd.read_csv(full_fname, sep='\t')
  print(full_fname)

  num_plot_lines = int(len(df.columns) / 2)
  plot_line_names = df.columns[1::2]
  data_names = df.columns[0::2]

  if num_plot_lines == 0:
    return

  xlabel = df.columns[0] 
  ylabel = "value" 
  x_axis = df[df.columns[0]].to_list()
  
  x_min = [] 
  x_max = []
  y_min = []
  y_max = []

  # ax = axs[fig_posx, fig_posy]
  
  for i in range(num_plot_lines):
    line_name = plot_line_names[i]
    if line_name not in target_lines:
      continue

    print("one line")
    x_axis = df[data_names[i]].to_list()  

    legend_name = line_name
    y_axis = df[line_name].to_list()
    y_axis = [item for item in y_axis if item >= 0]
    if len(y_axis) == 0:
      continue

    # x_smooth = np.linspace(np.array(x_axis).min(), np.array(x_axis).max(), 200)
    # y_axis = list(interp1d(x_axis, y_axis, x_smooth))
    # x_axis = list(x_smooth)

    len_valid = len(y_axis)
    
    # if fname is 'E2E_RTN_TIME.dat':
    #   if len_valid < 51:
    #     y_axis = savgol_filter(y_axis, len_valid / 2 - len_valid % 2 - 1, 3)
    #   else:
    #     y_axis = savgol_filter(y_axis, 51, 3)

    print(len(x_axis), len(y_axis))


    # Start ploting this line
    x_min.append(min(x_axis[:len_valid]))
    x_max.append(max(x_axis[:len_valid]))
    y_min.append(min(y_axis[:len_valid]))
    y_max.append(max(y_axis[:len_valid]))
    ax.set_axisbelow(True)

    fidx = fname_to_ind[fname]
    sidx = scena_to_ind[scenario]

    if fidx == 1:
      y_axis[0] = 0.4

    
    if marker_pos and marker_pos[(fidx, sidx)][0]:
        ax.plot(x_axis[:len_valid], y_axis[:len_valid], 
              color=get_color(line_name), ms=get_markersize(line_name),  linestyle = get_linestyle(line_name), 
              marker=get_marker(line_name), markerfacecolor=get_markfacecolor(line_name), 
              markeredgecolor=get_markedgecolor(line_name), markeredgewidth=get_markedgewidth(line_name),
              markevery=marker_pos[(fidx, sidx)][i + 1], clip_on= (fidx is not 1),
              label=line_name, linewidth=get_linewidth(line_name), zorder = get_zorder(line_name))
    else:
      ax.plot(x_axis[:len_valid], y_axis[:len_valid], 
            color=get_color(line_name), linestyle = get_linestyle(line_name), 
            label=line_name, linewidth=get_linewidth(line_name), zorder = get_zorder(line_name))


    ax.set_axisbelow(True)
    
  if fname in x_scales:
    ax.set_xscale(x_scales[fname])

  ax.grid(True)
  ax.tick_params(bottom=False, top=False, left = False, right = False, axis='both', which='major', labelsize=tickfontsize)
  for pos in ['top', 'bottom', 'right', 'left']:
    ax.spines[pos].set_color('#b0b0b0')

  

  if (fidx, sidx) in tick_info:
    ticks = tick_info[(fidx, sidx)]
    if 'xlim' in ticks:
      ax.set_xlim(ticks['xlim'][0], ticks['xlim'][1])
      if len(ticks['xlim']) > 2:
        ax.set_xticks(ticks['xlim'][2])

    if 'ylim' in ticks:
      ax.set_ylim(ticks['ylim'][0], ticks['ylim'][1])
      if len(ticks['ylim']) > 2:
        ax.set_yticks(ticks['ylim'][2])

    if sidx ==0:
      plt.legend(bbox_to_anchor=(0.5, 1.5), loc='upper center', fontsize=legendfontsize, 
        ncol=8, handlelength=1.2, handletextpad=0.2, columnspacing=0.3).set_zorder(-1)



  # if x_max_take == "per" and fname in x_max_percentages:
  #   x_span = max(x_max) * x_max_percentages[fname] - min(x_min)
  #   xlim_min = min(x_min) - 0.1 * x_span if x_span > 0 else min(x_min) - 1
  #   xlim_max = max(x_max) * x_max_percentages[fname] + 0.1 * x_span if x_span > 0 else max(x_max) + 1
  #   ax.set_xlim(xlim_min, xlim_max)

  # if x_max_take == "value" and fname in x_maxes:
  #   x_span = max(x_max) * x_max_percentages[fname] - min(x_min)
  #   xlim_min = x_mins[fname] if fname in x_mins else math.floor(min(x_min) - 0.1 * x_span) if x_span > 0 else min(x_min) - 1
  #   xlim_max = x_maxes[fname] # if max(x_max) > x_maxes[fname] else max(x_max) + 1
  #   ax.set_xlim(xlim_min, xlim_max)

  # if fname in y_mins:
  #   y_span = max(y_max) - y_mins[fname]
  #   print(y_mins[fname])
  #   ylim_min = y_mins[fname] - 0.1 * y_span if y_span > 0 else min(y_min) - 1
  #   ylim_max = max(y_max) + 0.1 * y_span if y_span > 0 else max(y_max) + 1
  #   ax.set_ylim(ylim_min, ylim_max)

  return 0

fig = plt.figure(figsize=(20, 10))
gs = fig.add_gridspec(len(scenario_folders), len(figs)*2 + 1)
# fig, axs = plt.subplots(3, 3, )
fig.subplots_adjust(left=0.10, bottom=0.13, right=0.98, top=0.92, wspace=0.8, hspace=0.5)
# plt.gcf().subplots_adjust(top=0.92)

latency_plotted = False
for i, figdir in enumerate(scenario_folders):
  for j, fname in enumerate(figs):
    figpath = figdir + '/benchmarking-123.0/' + fname
    # print(i, fname)
    if fname is "E2E_RTN_TIME.dat":
      ax = fig.add_subplot(gs[i, 2 * j : 2 * j + 3])
      latency_plotted = True
    elif latency_plotted is True:
      ax = fig.add_subplot(gs[i, 2 * j + 1: 2 * j + 3])
    else:
      ax = fig.add_subplot(gs[i, 2 * j : 2 * j + 2])
    # ax = fig.add_subplot(gs[i, j])
    generate_subplot(figpath, ax, fname, scenario_label[i])

    if j == 0:
      ax.set_ylabel('CDF', fontsize=labelfontsize)

    if j == 0:
      ax.text(-0.25, 0.5, scenario_label[i], {'color': 'k', 'fontsize': labelfontsize},
        horizontalalignment='left',
        verticalalignment='center',
        rotation=90,
        clip_on=False,
        transform=ax.transAxes,
        bbox=dict(boxstyle="square",
          # ec='#000000',
          # fc='#ffffff'
          ec=(1., 0.5, 0.5),
          fc=(1., 0.8, 0.8),
          )
        )

    if i == len(scenario_folders) - 1:
      ax.set_xlabel(get_xlabel(fname), fontsize=labelfontsize)

# # Set x_label
# for j, fname in enumerate(figs):
#   axs[-1, j].set_xlabel(get_xlabel(fname), fontsize=labelfontsize)

# # Set y_label
# for i in range(len(scenario_folders)):
#   axs[i, 0].set_ylabel('CDF', fontsize=labelfontsize)
#   axs[i, -1].text(1.06, 0.5, scenario_label[i], {'color': 'k', 'fontsize': labelfontsize},
#     horizontalalignment='left',
#     verticalalignment='center',
#     rotation=270,
#     clip_on=False,
#     transform=axs[i, -1].transAxes,
#     bbox=dict(boxstyle="square",
#       ec=(1., 0.5, 0.5),
#       fc=(1., 0.8, 0.8),
#       )
#     )

# plt.show()
figname = 'fig12.png'
if os.path.isfile(figname):
  os.remove(figname)
plt.savefig(figname, dpi=100)
plt.show()
plt.clf()
plt.close()

