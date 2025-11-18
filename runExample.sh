#!/bin/sh
make clean
make all
java -jar ./build/Build.jar example_input1.txt 1 2 3 4 5 6 7 8 100000 1000
