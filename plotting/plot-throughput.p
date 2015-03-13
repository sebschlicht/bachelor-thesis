set title "Request throughput"

# label
set key top right
set key spacing 1.5
set xlabel "Duration (seconds)"
set ylabel "Throughput"

# plot
set autoscale
set terminal postscript landscape enhanced lw 1 "Helvetica" 14
set output "throughput.ps"
set size 1,1
plot filename using 1:2 title 'throughput' w lines
plot filename using 1:3 title 'number of requests' w lines
