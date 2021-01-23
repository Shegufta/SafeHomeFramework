from gen_one_plot import SinglePlot
import argparse
import os
import sys

parser = argparse.ArgumentParser()
parser.add_argument("-d", "--dir_expr", required=False, help="the path of root directory of the experiment folder destination", default='fig14')
parser.add_argument("-min_line", "--min_line", help="Minimal number of data line to generate a plot", default=4)
parser.add_argument("-dpi", "--dpi", help="Figure dpi, default 100 (low)", default=100)
args = parser.parse_args()
root_folder = args.dir_expr

bar_folder = root_folder + '/avg'
bar_files = None
bar_ylabel = {
  'dev_utilization.dat': 'Device Utilization (%)',
  'isolation.dat': 'Isolation Violation (%)',
  'latency.dat': 'Latency (s)',
  'parallel.dat': 'Parallelism Level',
  'e2e-prepost-compare.dat': 'End-to-End Latency\n(Normalized w.r.t GSV)',
  'iso-prepost-compare.dat': 'Isolation Violation (%)'
}
bar_left_margin = {
  'latency.dat': 0.15
}

if bar_files is None:
  bar_files = [ fi for fi in next(os.walk(bar_folder))[2] if fi.endswith(".dat") ]

for file in bar_files:
  bar_chart_data_file = bar_folder + '/' + file
  p1 = SinglePlot('bar')
  if file in bar_ylabel:
    p1.set_ylabel(bar_ylabel[file])
  if file in bar_left_margin:
    p1.set_left_margin(bar_left_margin[file])
  p1.generate_bar_chart(bar_chart_data_file, root_folder + '/figure', '\t')

  