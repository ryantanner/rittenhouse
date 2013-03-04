#! /bin/sh

curdir=${PWD##*/}

if [ "$curdir" != "rittenhouse" ] ; then
  exit 1
fi

find ./ -type f -name \*.scala -exec sed -iE 's/\({{{{\)\([a-z]*\)\(}}}}\)/[[http:\/\/redis.io\/commands\/\2 \2]]/g' {} \;
