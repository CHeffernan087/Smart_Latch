import requests
import json
import base64

with open("McNally.jpg", "rb") as image_file:
    encoded_string = base64.b64encode(image_file.read()).decode()

with open("string.txt", "w") as text_file:
    text_file.write(encoded_string)

# data = {"image": encoded_string}
# # url = "http://127.0.0.1:8080/"
# url = "https://recognition.smart-latchxyz.xyz/"
# headers = {'Content-type': 'application/json'}
# response = requests.post(url, json=data, headers=headers)
# print(response.json())


# data = {"image": encoded_string, "door": 31415}
# # url = "http://127.0.0.1:8080/verify"
# url = "https://recognition.smart-latchxyz.xyz/verify"
# headers = {'Content-type': 'application/json'}
# response = requests.post(url, json=data, headers=headers)
# print(response.json())


data = {"email": "mcnallc2@tcd.ie"}
# url = "http://127.0.0.1:8080/register"
url = "https://recognition.smart-latchxyz.xyz/register"
files = {'image': open('McNally.jpg', 'rb')}
response = requests.post(url, files=files, data=data)
print(response.status_code)
print(response.json())

