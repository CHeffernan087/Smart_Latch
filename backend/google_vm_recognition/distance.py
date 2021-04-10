import cv2
import mtcnn
import numpy
import math

def distance(point_one, point_two):
    return math.sqrt( ((point_one[0]-point_two[0])**2)+((point_one[1]-point_two[1])**2) )

def point_is_on_line(start, end, point):
    if (distance(start, point) + distance(end, point) == distance(start, end)):
        return True
    return False

image = cv2.imread("dist_foreground.jpg")
height, width, channels = image.shape
color = (0, 0, 255)
eye_x1 = int(width/10)
eye_y1 = int(height/2)
eye_x2 = eye_x1+50
eye_y2 = eye_y1+50
thickness = -1 # -1 to fill shape
img = cv2.rectangle(image, (eye_x1, eye_y1), (eye_x2, eye_y2), color, thickness)
# cv2.imwrite("dist_temp_foreground.jpg", img)


detector = mtcnn.MTCNN()
results = detector.detect_faces(image)
if len(results) == 0:
    raise ValueError
face_x1, face_y1, face_width, face_height = results[0]['box']
face_x2, face_y2 = face_x1 + face_width, face_y1 + face_height

background = cv2.imread("dist_background.jpg")
foreground = cv2.imread("dist_temp_foreground_block.jpg")
# foreground = cv2.imread("dist_temp_foreground.jpg")
subtracted_image = cv2.subtract(foreground,background)
# cv2.imwrite("subtract_block.jpg", subtracted_image)



top_left = (eye_x2, eye_y1)
top_right = (face_x1, face_y1)
bottom_left = (eye_x2, eye_y2)
bottom_right = (face_x1, face_y2)

print(top_left)
print(top_right)
print(bottom_left)
print(bottom_right)
print(subtracted_image.shape)

(178, 360)
(1105, 555)
(178, 410)
(1105, 633)
(720, 1280, 3)

for col in range(top_left[1], bottom_left[1]):
    for row in range(top_left[0], top_right[0]):
        if point_is_on_line(top_left, top_right, (row, col)) or point_is_on_line(bottom_left, bottom_right, (row, col)):
            continue
        # if row > width or col > height:
        #     print("Nope")
        #     exit(-1)
        # print(f"Curent = {row}, {col} : {len(subtracted_image[col])}, {len(subtracted_image)}")
        # print(f"Coords = {top_left[1]}, {bottom_left[1]} : {top_left[0]}, {top_right[0]}")
        if sum(subtracted_image[col, row])/len(subtracted_image[col, row]) > 25:
                print(f"Something is blocking the camera! ({subtracted_image[col, row]})")
                exit(-1)
            

print("All clear!")


