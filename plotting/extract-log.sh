#!/bin/bash
F_SOURCE=$1
D_TARGET=$2

if [ ! ${NAME} -o ! ${D_TARGET} ]; then
  echo 'usage: "main.sh <logFile> <targetDir>"'
  exit
fi

if [ -a ${D_TARGET} ]; then
  echo 'target directory is existing, exiting.'
  exit
fi
mkdir -p ${D_TARGET}

F_LOG=${D_TARGET}/benchmark.log
F_DATA=${D_TARGET}/requests.dat
F_ERR=${D_TARGET}/errors.dat

# extract single log
tac ${F_SOURCE} | sed '/will now attack/q' | tac > ${F_LOG}
# separate valid data from errors
grep -e "Exception" -e "exception" -e "timed out" -e "null" ${F_LOG} > ${F_ERR}
grep -v -e "Exception" -e "exception" -e "timed out" -e "null" -e "AsyncClient" -e "limit reached" ${F_LOG} |
  awk '{if(NR==1){delta=$1};$1=$1-delta;print $1,$9,$10,$11,$12}' | grep -v "-"  > ${F_DATA}

# sort in seconds
F_DPS=${D_TARGET}/requests-s.dat
awk '{print int($1/1000),$2,$3,$4,$5}' ${F_DATA} | sort -s -n -k 1,1 > ${F_DPS}

# throughput
F_TRO=${D_TARGET}/throughput.dat
awk '{if(NR==1){second=0}if(second!=$1){ttro=ttro+tro;print second,tro,ttro;second=$1;tro=0}tro=tro+1} END {print second,tro}' ${F_DPS} > ${F_TRO}

# average request latencies
F_AVGLAT=${D_TARGET}/avg-latencies.dat
awk '{if(NR==1){second=0}if(second!=$1){print second,num,lat/num;second=$1;num=0;lat=0}lat=lat+$3;num=num+1} END {print second,num,lat/num}' ${F_DPS} > ${F_AVGLAT}

# overall statistics
F_STATS=${D_TARGET}/stats.dat
awk '{if(second!=$1){second=$1;numb=numb+1}lat=lat+$3;num=num+1} END {print num,num/numb,lat/num}' ${F_DPS} > ${F_STATS}

# create plots
pushd ${D_TARGET} 2>&1 /dev/null
gnuplot -e "filename='avg-latencies.dat'" ../../../plot-avglatencies.p
gnuplot -e "filename='throughput.dat'" ../../../plot-throughput.p
popd 2>&1 /dev/null
exit

gnuplot -e "filename='num-requests.dat'" ../plot-numrequests.p
