set title "Average request latencies"

# label
set key top right
set key spacing 1.5
set xlabel "Duration (seconds)"
set ylabel "Average request latency (milliseconds)"

# plot
set autoscale
set terminal postscript landscape enhanced lw 1 "Helvetica" 14
set output "latencies.ps"
set size 1,1
plot filename using 1:2 title 'write' w lines
