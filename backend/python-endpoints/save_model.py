from flask import escape
import cv2
import numpy
from scipy.spatial.distance import cosine
from PIL import Image
from numpy import asarray
from google.cloud import storage
import pandas
import flask
from keras_vggface.vggface import VGGFace
from keras_vggface.utils import preprocess_input
from sys import maxsize
from flask import jsonify
import pickle
import tensorflow as tf
import keras


model = VGGFace(model='resnet50', include_top=False, input_shape=(224, 224, 3), pooling='avg')

print("TTTEESTTT")
print(type(preprocess_input))

temp = tf.keras.Sequential([
  base_model,
  model])

# inputs = keras.Input(shape=(224, 224, 3))
# x = preprocess_input(inputs, version=2)
# outputs = model(x)
# inference_model = keras.Model(inputs, outputs)


model.save("keras_export")
#export_path = tf.contrib.saved_model.save_keras_model(model, 'keras_export')

# filename = 'vggface_model.pkl'
# # pickle.dump(model, open(filename, 'wb'))
# # saving model
# json_model = model.to_json()
# open('model_architecture.json', 'w').write(json_model)
# # saving weights
# model.save_weights('model_weights.h5', overwrite=True)
