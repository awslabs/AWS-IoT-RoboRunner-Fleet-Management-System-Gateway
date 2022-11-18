#!/bin/bash

S3_BUCKET=$1
FNAME="fmsg"
ARCHIVE_DIR=`pwd`

# Remove old archives
if test -f "$FNAME"_application.zip; then
  rm "$FNAME"_application.zip
fi

if test -f "$FNAME"_configs.zip; then
  rm "$FNAME"_configs.zip
fi

# Create archives
zip -r fmsg_configs.zip configuration/*

cd build/libs
zip "$ARCHIVE_DIR"/fmsg_application.zip RoboRunnerFmsGateway.jar

# Copy to S3 bucket
cd ../..
aws s3 cp fmsg_application.zip s3://"$S3_BUCKET"/artifacts/
aws s3 cp fmsg_configs.zip s3://"$S3_BUCKET"/artifacts/

# Cleanup
rm "$FNAME"_application.zip
rm "$FNAME"_configs.zip