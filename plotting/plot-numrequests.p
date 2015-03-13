set title "Number of Requests"

# label
set key top right
set key spacing 1.5
set xlabel "Duration (seconds)"
set ylabel "Requests handled"

# plot
set autoscale
set terminal postscript landscape enhanced lw 1 "Helvetica" 14
set output "numrequests.ps"
set size 1,1
plot filename using 1:2 title 'write' w lines
