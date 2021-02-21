import pandas as pd
import sys
import argparse
import os
import matplotlib.pyplot as plt
from matplotlib.lines import Line2D
import matplotlib
import numpy as np
import random

matplotlib.font_manager._rebuild()
matplotlib.rcParams['font.family'] = 'Times New Roman'
matplotlib.rcParams['font.weight'] = 'normal'

color_dict = {'GSV': '#2a70ba', 'PSV' : '#f7c143', 'EV' : '#4ead5b', 'WV' : '#b96029', 
              'LAZY_FCFS' : 'r', 'LAZY_PRIORITY': 'chartreuse', 'FCFSV': 'orange', 'LzPRIOTY': 'chartreuse', 
              '4.0': 'g', '8.0': 'r',
              'EV(incong)': 'g', 'WV(incong)': 'orange', 'EV(order)': 'g'}
mark_dict = {'EV': '^', 'WV': 's', 
             'EV(incong)': '^', 'EV(order)': 's', 'WV(incong)': 'o'}
linestyle_dict = {'PSV': '--', 'EV': '-.', 'WV': (0, (8, 10)), 'GSV' : '-'}
markfacecolor_dict = {'EV' : '#4ead5b', 'WV': '#cd783f'}
markedgecolor_dict = {'EV': '#60803f', 'WV': '#b76b38'}
markedgewidth_dict = {'EV': 1}
target_lines = ['GSV', 'EV', 'WV', 'PSV', 'SUPER_GSV']
artifact_lines = ['EV(incong)', 'WV(incong)', 'EV(order)']

bar_color = ['#ffffff', '#DF8244', '#4EAD5B', '#B99130', '#E7E6E6']
bar_edgecolor = ['#3f74b1', '#ffffff', '#4EAD5B', '#ffffff', '#000000']
bar_pattern = ['///', '.', '\\', '|||', '+']

def get_color(line_name):
  if line_name in color_dict:
    return color_dict[line_name]
  else:
    return 'k'

def get_marker(line_name):
  if line_name in mark_dict:
    return mark_dict[line_name]
  else:
    return None

def get_linestyle(line_name):
  if line_name in linestyle_dict:
    return linestyle_dict[line_name]
  else:
    return ':'

def get_markedgecolor(line_name):
  if line_name in markedgecolor_dict:
    return markedgecolor_dict[line_name]
  else:
    return get_color(line_name)

def get_markfacecolor(line_name):
  if line_name in markfacecolor_dict:
    return markfacecolor_dict[line_name]
  else:
    return get_color(line_name)

def get_markedgewidth(line_name):
  if line_name in markedgewidth_dict:
    return markedgewidth_dict[line_name]
  else:
    return 0

class SinglePlot:
  xlim_min = xlim_max = ylim_min = ylim_max = None
  xticks = yticks = None
  xlabel = ylabel = None
  xlabel_rot = 0
  fig_width = fig_height = 3
  figname = None
  fig_dpi = 500
  plot_title = False
  fig_title = "Title"
  left_margin = 0.2
  bottom_margin = 0.2
  fig_type = 'avg'
  linewidth = 1.6
  legend_style = None
  
  def __init__(self, fig_type):
    self.fig_type = fig_type
    if fig_type == 'avg':
      self.fig_width = 4
      self.fig_height = 3
    elif fig_type == 'cdf':
      self.fig_width = 3
      self.fig_height = 2
      self.ylabel = 'CDF'
    elif fig_type == 'bar':
      self.fig_width = 5
      self.fig_height = 2.5
      self.left_margin = 0.12
      self.bottom_margin = 0.12

  def set_xlim(self, xmin, xmax):
    self.xlim_min = xmin
    self.xlim_max = xmax

  def set_ylim(self, ymin, ymax):
    self.ylim_min = ymin
    self.ylim_max = ymax

  def set_xtick(self, tick):
    self.xticks = tick

  def set_ytick(self, tick):
    self.yticks = tick

  def set_xlabel(self, label):
    self.xlabel = label

  def set_ylabel(self, label):
    self.ylabel = label

  def set_xlabel_rotation(self, degree):
    self.xlabel_rot = degree

  def set_figsize(self, width, height):
    self.fig_width = width
    self.fig_height = height

  def set_figname(self, name):
    self.figname = name

  def set_figdpi(self, dpi):
    self.fig_dpi = dpi

  def set_title(self, title):
    self.plot_title = True
    self.fig_title = title

  def set_left_margin(self, margin):
    self.left_margin = margin

  def set_bottom_margin(self, margin):
    self.bottom_margin = margin

  def generate_bar_chart(self, full_fname, figure_folder, seperator=','):
    if not os.path.exists(figure_folder):
      os.makedirs(figure_folder)

    df = pd.read_csv(full_fname, sep=seperator)
    df = df.dropna(how='all', axis=1)
    x_axis = df.columns[1:]
    n_groups = len(df.columns) - 1
    bar_ind = np.arange(n_groups)
    bar_width = 0.6 / n_groups

    plt.figure(figsize=(self.fig_width, self.fig_height))
    ax = plt.gca()
    for pos in ['top', 'bottom', 'right', 'left']:
      ax.spines[pos].set_color('#b0b0b0')
    ax.set_axisbelow(True)
    ax.yaxis.grid(True)
    # plt.grid(True)
    
    for index, row in df.iterrows():
      legend_name = row[df.columns[0]]
      y_data = row[df.columns[1:]].to_list()
      ax.bar(bar_ind + bar_width * (index - n_groups / 2), y_data, width=bar_width, 
        color=bar_color[index % len(bar_color)], hatch=bar_pattern[index % len(bar_pattern)], 
        edgecolor=bar_edgecolor[index % len(bar_pattern)], linewidth=0, label=legend_name)

    if index <= 3:
      plt.legend(loc=9, ncol=index + 1, bbox_to_anchor=(0.5, 1.18))
    else:
      plt.legend()

    plt.xticks(bar_ind, x_axis)
    self.ylabel = "value" if self.ylabel is None else self.ylabel
    plt.ylabel(self.ylabel)
    if self.plot_title:
      plt.title(self.plot_title)
    plt.tick_params(bottom=False, top=False, left = False, right = False)
    if self.left_margin is not None:
      plt.gcf().subplots_adjust(left=self.left_margin)
    if self.bottom_margin is not None:
      plt.gcf().subplots_adjust(bottom=self.bottom_margin)

    self.figname = figure_folder + '/' + full_fname.split('/')[-1].split('.')[0] + '.png' if self.figname is None else (figure_folder + '/' + self.figname)
    if os.path.isfile(self.figname):
      os.remove(self.figname)
    plt.savefig(self.figname, dpi=self.fig_dpi)
    plt.clf()
    plt.close()

  def generate_plot(self, full_fname, figure_folder):
    if not os.path.exists(figure_folder):
      os.makedirs(figure_folder)

    df = pd.read_csv(full_fname, sep='\t')

    if self.fig_type == 'avg':
      df.dropna(axis='columns', how='all', inplace=True)
    print(full_fname)
    
    if self.fig_type is 'avg':
      num_plot_lines = len(df.columns) - 1
      plot_line_names = df.columns[1:]
      data_names = df.columns[1:]
    elif self.fig_type is 'cdf':
      num_plot_lines = int(len(df.columns) / 2)
      plot_line_names = df.columns[1::2]
      data_names = df.columns[0::2]

    if num_plot_lines == 0:
      return

    plt.figure(figsize=(self.fig_width, self.fig_height))
    self.xlabel = df.columns[0] if self.xlabel is None else self.xlabel
    self.ylabel = "value" if self.ylabel is None else self.ylabel
    x_axis = df[df.columns[0]].to_list()
    
    x_min = [] 
    x_max = []
    y_min = []
    y_max = []

    for i in range(num_plot_lines):
      line_name = plot_line_names[i]
    
      if self.fig_type is not 'cdf' and line_name not in target_lines and line_name not in artifact_lines:
        continue

      if self.fig_type is 'cdf':
        x_axis = df[data_names[i]].to_list()  
        
      legend_name = line_name
      y_axis = df[line_name].to_list()
      y_axis = [item for item in y_axis if item >= 0]
      if len(y_axis) == 0:
        continue

      # Start ploting this line
      len_valid = len(y_axis)
      ax = plt.gca()
      ax.get_yaxis().get_major_formatter().set_useOffset(False)
      if self.fig_type is 'avg': 
        plt_line = Line2D(x_axis[:len_valid], y_axis[:len_valid], 
          color=get_color(line_name), linestyle = get_linestyle(line_name), 
          marker=get_marker(line_name), markerfacecolor=get_markfacecolor(line_name), 
          markeredgecolor=get_markedgecolor(line_name), markeredgewidth=get_markedgewidth(line_name),
          label=line_name, linewidth = 1.6)
      else:
        print("drawing one line")
        plt_line = Line2D(x_axis[:len_valid], y_axis[:len_valid], 
          color=get_color(line_name), linestyle = get_linestyle(line_name), 
          label=line_name, linewidth = 1.2)
    
      x_min.append(min(x_axis[:len_valid]))
      x_max.append(max(x_axis[:len_valid]))
      y_min.append(min(y_axis[:len_valid]))
      y_max.append(max(y_axis[:len_valid]))
      ax.add_line(plt_line)
      ax.legend()
      for pos in ['top', 'bottom', 'right', 'left']:
          ax.spines[pos].set_color('#b0b0b0')

    if self.xlim_min is None or self.xlim_max is None:
      x_span = max(x_max) - min(x_min)
      self.xlim_min = min(x_min) - 0.1 * x_span if min(x_min) != max(x_max) else min(x_min) - 1
      self.xlim_max = max(x_max) + 0.1 * x_span if min(x_min) != max(x_max) else max(x_max) + 1

    if self.ylim_min is None or self.ylim_max is None:
      y_span = max(y_max) - min(y_min)
      self.ylim_min = min(y_min) - 0.1 * y_span if min(y_min) != max(y_max) else min(y_min) - 1
      self.ylim_max = max(y_max) + 0.1 * y_span if min(y_min) != max(y_max) else max(y_max) + 1

    plt.xlim(self.xlim_min, self.xlim_max)
    plt.ylim(self.ylim_min, self.ylim_max)

    plt.xlabel(self.xlabel)
    plt.ylabel(self.ylabel)
    if self.xlabel_rot > 0:
      plt.xticks(rotation=self.xlabel_rot)
    if self.plot_title:
      plt.title(self.plot_title)
    plt.grid(True)
    plt.tick_params(bottom=False, top=False, left = False, right = False)
    plt.gcf().subplots_adjust(left=self.left_margin)
    plt.gcf().subplots_adjust(bottom=self.bottom_margin)

    # Plot saving
    self.figname = figure_folder + '/' + full_fname.split('/')[-1].split('.')[0] + '.png' if self.figname is None else (figure_folder + '/' + self.figname)
    if os.path.isfile(self.figname):
      os.remove(self.figname)
    plt.savefig(self.figname, dpi=self.fig_dpi)
    plt.clf()
    plt.close()

