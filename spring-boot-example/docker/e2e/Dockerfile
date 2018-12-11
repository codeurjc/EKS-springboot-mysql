FROM ubuntu:16.04

ENV DEBIAN_FRONTEND noninteractive
ENV DISPLAY :99.0

# Install Java
RUN apt-get update && apt-get install -qqy \
    default-jdk \
    maven \
    apt-transport-https \
    software-properties-common \
    xvfb \
    wget

# Install Chrome
RUN wget -q -O - https://dl-ssl.google.com/linux/linux_signing_key.pub | apt-key add - \
  && echo "deb http://dl.google.com/linux/chrome/deb/ stable main" >> /etc/apt/sources.list.d/google-chrome.list \
  && apt-get update -qqy \
  && apt-get -qqy install \
    google-chrome-stable \
  && rm /etc/apt/sources.list.d/google-chrome.list
COPY wrap_chrome_binary /opt/bin/wrap_chrome_binary
RUN /opt/bin/wrap_chrome_binary

# Cleanup
RUN apt-get autoremove --purge

COPY entrypoint.sh /entrypoint.sh 
ENTRYPOINT /entrypoint.sh

