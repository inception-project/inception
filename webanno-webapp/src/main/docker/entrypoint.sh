#!/bin/bash
#service mysql start
service webanno start
tail -F /opt/webanno/logs/*
