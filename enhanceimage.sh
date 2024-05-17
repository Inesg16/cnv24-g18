#!bin/bash

# Give arguments $1 as picture.jpg

# Encode in Base64.
base64 $1 > temp.txt                                            

# Append a formatting string.
echo -e "data:image/jpg;base64,$(cat temp.txt)" > temp.txt               

# Send the request.
# Local
curl -X POST http://127.0.0.1:8000/enhanceimage --data @"./temp.txt" > result.txt
# Using AWS
#curl -X POST http://proj-lb-1519616844.eu-north-1.elb.amazonaws.com/enhanceimage --data @"./temp.txt" > result.txt

# Remove a formatting string (remove everything before the comma).
sed -i 's/^[^,]*,//' result.txt

tmp=$1
b=${tmp%.*}

# Decode from Base64.
base64 -d result.txt > $b-result.jpg

# Remove temporary files
rm -f temp.txt result.txt
