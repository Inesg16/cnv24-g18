#!bin/bash

# Give arguments $1 as scene.txt
for i in 1 2 3
do
# Add scene.txt raw content to JSON.
cat $1 | jq -sR '{scene: .}' > payload$i.json

# Add texmap.bmp binary to JSON (optional step, required only for some scenes).
hexdump -ve '1/1 "%u\n"' texmap.bmp | jq -s --argjson original "$(<payload$i.json)" '$original * {texmap: .}' > payload$i.json
done

for i in 1 2 3
do
# Send the request.
curl -X POST http://127.0.0.1:8000/raytracer?scols=400\&srows=300\&wcols=400\&wrows=300\&coff=0\&roff=0\&aa=false --data @"./payload$i.json" > result.txt &
done

for i in 1 2 3
do
# Remove a formatting string (remove everything before the comma).
sed -i 's/^[^,]*,//' result$i.txt

tmp=$1
a=${tmp%.*}

# Decode from Base64.
base64 -d result$i.txt > $a-result$i.bmp

# Remove temporary files
rm -f payload$i.json result$i.txt
done