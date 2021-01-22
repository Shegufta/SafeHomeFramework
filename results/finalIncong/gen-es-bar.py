import pandas as pd
import numpy as np
import matplotlib
import matplotlib.pyplot as plt
import matplotlib.ticker as ticker
import os
import math

xlim_min = xlim_max = None
ylim_min = 0
ylim_max = 0.4
y_ticks = [0, 0.1, 0.2, 0.3, 0.4]
xticks = yticks = None
xlabel = None
ylabel = "Final\nIncongruence"
xlabel_rot = 0
fig_width = 4
fig_height = 2.5
figname = None
fig_dpi = 200
plot_title = False
fig_title = "Title"
left_margin = None
bottom_margin = None
right_margin = None
top_margin = 1.2
fig_type = 'avg'
linewidth = 1.6
legend_style = None
tickfontsize = 18
legendsize = 16  
xlabelsize = 22
ylabelsize = 22
num_minor_ver_grid = 3
num_minor_hor_grid = 4
legend_loc = 'upper center'
legend_bbox = [0.5, 1.25]
legend_col = 3
linewidth = 2

bar_color = ['#ffffff', '#ffffff', '#ffffff', '#ffffff', '#ffffff']
# bar_edgecolor = ['#000000', '#000000', '#000000', '#000000', '#000000']
bar_edgecolor = ['#225a96', '#60803f', '#b96029', '#000000', '#000000']
# bar_color = ['#ffffff', '#DF8244', '#4EAD5B', '#B99130', '#E7E6E6']
# bar_edgecolor = ['#3f74b1', '#ffffff', '#ffffff', '#ffffff', '#000000']

bar_pattern = ['////', '+', '\\\\', '|||', '+']
matplotlib.font_manager._rebuild()
matplotlib.rcParams['font.family'] = 'Times New Roman'
matplotlib.rcParams['font.weight'] = 'normal'
matplotlib.rcParams['font.size'] = 16
plt.rcParams['figure.constrained_layout.use'] = True



df_raw = pd.read_csv("finalIncong.txt", sep=',',  index_col=0)
df_raw['GSV'] = 0
df_raw['EV'] = 0
df_raw.rename(columns={'mean':'WV'}, inplace=True)
wv_stdev = list(df_raw['stdev'])
wv_stdev = [[0]*len(wv_stdev), [0] * len(wv_stdev), wv_stdev]
df_raw[['GSV', 'EV', 'WV']].transpose().to_csv("tmp.csv")

df = pd.read_csv('tmp.csv')


x_axis = df.columns[1:]
n_groups = 3
# n_groups = len(df.columns) - 1
bar_ind = np.arange(n_groups)
bar_width = 0.8 / n_groups

plt.figure(figsize=(fig_width, fig_height))
ax = plt.gca()
for pos in ['top', 'bottom', 'right', 'left']:
  ax.spines[pos].set_color('#b0b0b0')
ax.yaxis.grid(True)
ax.set_axisbelow(True)
# plt.grid(True)
print(df)
ax.text(0.17, 0.2, 'No Incongruence\nfor PSV, GSV', horizontalalignment='center',
    bbox={'facecolor': 'white', 'alpha': 0.8, 'pad': 2}, zorder=3)

ax.annotate("", xy=(-0.3, 0.02),xytext=(-0.3, 0.18), 
  arrowprops=dict(arrowstyle="-|>", color='gray'))

ax.annotate("", xy=(0.7, 0.02),xytext=(0.3, 0.179), 
  arrowprops=dict(arrowstyle="-|>", color='gray'))

ax.annotate("", xy=(1.7, 0.02),xytext=(0.9, 0.176), 
  arrowprops=dict(arrowstyle="-|>", color='gray'))

for index, row in df.iterrows():
  legend_name = row[df.columns[0]]
  # print(df.columns)
  y_data = row[df.columns[1:n_groups + 1]].to_list()  

  ax.bar(bar_ind + bar_width * (index - n_groups / 2), y_data, width=bar_width, 
    color=bar_color[index % len(bar_color)], hatch=bar_pattern[index % len(bar_pattern)], 
    edgecolor=bar_edgecolor[index % len(bar_pattern)], linewidth=2, label=legend_name, 
    yerr=wv_stdev[index][:n_groups], error_kw=dict(zorder=5), ecolor=bar_edgecolor[index % len(bar_pattern)], 
    capsize=2.5, zorder = 4)

  # ax.annotate('No Incongruence\nfor PSV, GSV', xy=(-0.3, 0.02), xytext=(-0.3, 0.2),
  #   arrowprops=dict(facecolor='black', shrink=0.01))
  
# if index <= 3:
#   plt.legend(loc=9, ncol=index + 1, bbox_to_anchor=(0.5, 1.18), fontsize=legendsize)
# else:
#   plt.legend()
plt.legend(fontsize=legendsize, loc=legend_loc, bbox_to_anchor=legend_bbox,
  ncol=legend_col, handlelength=1.2, handletextpad=0.3, columnspacing=0.5)

plt.xticks(bar_ind, x_axis)
if xlabel_rot > 0:
  plt.xticks(rotation=xlabel_rot)
ylabel = "value" if ylabel is None else ylabel
plt.ylabel(ylabel, fontsize=ylabelsize)

plt.ylim(ylim_min, ylim_max)
plt.yticks(y_ticks)
if plot_title:
  plt.title(plot_title)
plt.tick_params(bottom=False, top=False, left = False, right = False, labelsize=tickfontsize)
# plt.gcf().subplots_adjust(left=left_margin, bottom=bottom_margin, right=right_margin, top=top_margin)

figname = 'end-state.png'
if os.path.isfile(figname):
  os.remove(figname)
plt.savefig(figname, dpi=fig_dpi)
plt.clf()
plt.close()