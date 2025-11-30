#!/bin/sh
make clean
make all
java -jar ./build/Build.jar input_test12.txt 1 2 3 4 5 6 1 8 100000000 1
