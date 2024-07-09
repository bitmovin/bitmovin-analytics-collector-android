#!/bin/bash

if [ -z "$1" ]; then
  echo "Usage: $0 file_path"
  exit 1
fi

DIR=$1

if [ ! -d "$DIR" ]; then
  echo "Directory $DIR does not exist."
  exit 1
fi

# Iterate over all files in the directory
find "$DIR" -type f | while read -r FILE; do
  echo "Processing $FILE"

  # Skip the script.sh and index.html files
  if [ "$FILE" == "$DIR/script.sh" ] || [ "$FILE" == "$DIR/index.html" ]; then
    continue
  fi

  # Verify if the file is a text file
  if file "$FILE" | grep -q text; then
    # Replace index.html with ../index.html in all text files
    sed -i '' 's#"index.html"#"../index.html"#g' "$FILE"
  else
    echo "$FILE is not a text file. Ignore."
  fi
done