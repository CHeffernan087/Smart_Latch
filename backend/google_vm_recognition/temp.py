import requests
import json
import base64

with open("Owen.jpg", "rb") as image_file:
    encoded_string = base64.b64encode(image_file.read()).decode()

with open("string.txt", "w") as text_file:
    text_file.write(encoded_string)

# base64_img_bytes = encoded_string.encode('utf-8')
with open('temp.png', 'wb') as file_to_save:
    decoded_image_data = base64.b64decode(encoded_string)
    file_to_save.write(decoded_image_data)

# print (encoded_string)
data = {"image": encoded_string}
# print(data)
#url = "http://127.0.0.1:9090/recog"
url = "http://smart-latchxyz.xyz/recog"
headers = {'Content-type': 'application/json'}
response = requests.post(url, json=data, headers=headers)
# print(response)
# print(response.text)
print(response.json())