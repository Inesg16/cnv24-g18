#!bin/bash

# Give arguments $1 as picture.jpg

# Encode in Base64.
base64 $1 > temp.txt                                            

# Append a formatting string.
echo -e "data:image/jpg;base64,$(cat temp.txt)" > temp.txt               

# Send the request
# Local test
curl -X POST http://localhost:8000/blurimage --data @"./temp.txt" > result.txt
# Using AWS
#curl -X POST http://proj-lb-1519616844.eu-north-1.elb.amazonaws.com/blurimage --data @"./temp.txt" > result.txt

# Other experiments
#curl -X POST http://13.60.68.14:8000/blurimage --data @"./temp.txt" > result.txt &   
#seq 1 200 | xargs -n1 -P10 curl -X POST http://proj-lb-1519616844.eu-north-1.elb.amazonaws.com/blurimage --data @"./temp.txt" > result.txt   
#ab -c 1000 -n 5000 -p temp.txt -T "multipart/form-data; boundary=1234567890" http://proj-lb-1519616844.eu-north-1.elb.amazonaws.com/blurimage

# Remove a formatting string (remove everything before the comma).
sed -i 's/^[^,]*,//' result.txt

tmp=$1
b=${tmp%.*}

# Decode from Base64.
base64 -d result.txt > $b-result.jpg

# Remove temporary files
rm -f temp.txt result.txt
