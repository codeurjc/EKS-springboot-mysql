#!/bin/bash -x

TIMEOUT=60
i=0
until $(curl --connect-timeout 1 --output /dev/null --silent --head --fail $1); do
  printf '.'
  sleep 1
  i=$(expr $i + 1)
  if [ "$i" == "$TIMEOUT" ]; then
    echo "Timeout!"
    exit 1
  fi
done
