#!/bin/bash

# Build project and copy the designated file to the target directory
gradle clean build && cp "./T06xxyyy.zip" "./app/build/libs /"

# The below command only works for files copy
# Use rsync for robust file copying in Linux
# rsync -av "./T06xxyyy.zip" "./target/"
