from flask import escape
import cv2
import numpy
from scipy.spatial.distance import cosine
from PIL import Image
from numpy import asarray
import mtcnn
from google.cloud import storage
import pandas
import flask
from keras_vggface.vggface import VGGFace
from keras_vggface.utils import preprocess_input
from sys import maxsize
from flask import jsonify
from time import time
import os
import base64
import traceback

app = flask.Flask(__name__)

THRESHOLD = 0.5


class NoFaceFoundException(Exception):
    pass


def download_blob(bucket_name, source_blob_name, destination_file_name):
    """Downloads a blob from the bucket."""
    # bucket_name = "your-bucket-name"
    # source_blob_name = "storage-object-name"
    # destination_file_name = "local/path/to/file"
    storage_client = storage.Client()
    # storage_client = storage.Client.from_service_account_json("smart-latch-fd041937391e.json")
    bucket = storage_client.bucket(bucket_name)

    # Construct a client side representation of a blob.
    # Note `Bucket.blob` differs from `Bucket.get_blob` as it doesn't retrieve
    # any content from Google Cloud Storage. As we don't need additional data,
    # using `Bucket.blob` is preferred here.
    blob = bucket.blob(source_blob_name)
    blob.download_to_filename(destination_file_name)


MTCNN = mtcnn.MTCNN()
VGG_FACE = VGGFace(model='resnet50', include_top=False,
                   input_shape=(224, 224, 3), pooling='avg')
download_blob("vggface-bucket", "embeddings.csv", "embeddings.csv")
EMBEDDINGS = pandas.read_csv("embeddings.csv")
EMBEDDINGS_2D_LIST = EMBEDDINGS.values.tolist()


@app.route("/recog", methods=["POST"])
def facial_recognition():
    response = None
    try:
        img_path_str = f"{time()}_received.png"
        with open(img_path_str, 'wb') as file_to_save:
            decoded_image_data = base64.b64decode(flask.request.json["image"])
            temp = base64.b64encode(decoded_image_data).decode('ascii').encode()
            file_to_save.write(base64.decodebytes(temp))

        # with open("string.txt", "w") as text_file:
        #     text_file.write(base64.b64encode(decoded_image_data).decode('ascii'))

        img = cv2.imread(img_path_str)
        candidate_embedding = get_embedding(img)
        lowest_score_person = None
        lowest_score = maxsize
        for embedding_list_and_person in EMBEDDINGS_2D_LIST:
            embedding_list = embedding_list_and_person[:-1]
            person = embedding_list_and_person[-1]
            score = get_match_score(embedding_list, candidate_embedding)
            if score < lowest_score:
                lowest_score_person = person
                lowest_score = score
        if lowest_score <= THRESHOLD:
            response = jsonify({'Person': lowest_score_person, 'Score': lowest_score})
        else:
            response = jsonify({"Error": "Couldn't classify image"})
    except NoFaceFoundException:
        response = jsonify({"Error": "No Face Found In Image"})
    except Exception as e:
        response = jsonify({"Error": str(traceback.format_exc())})
    finally:
        os.remove(img_path_str)
        return response


def extract_face(image, required_size=(224, 224)):
    # create the detector, using default weights
    detector = MTCNN
    # detect faces in the image
    results = detector.detect_faces(image)

    if len(results) == 0:
        raise NoFaceFoundException

    # extract the bounding box from the first face
    x1, y1, width, height = results[0]['box']
    x2, y2 = x1 + width, y1 + height

    # extract the face
    face = image[y1:y2, x1:x2, :]

    # resize pixels to the model size
    image = Image.fromarray(face)
    image = image.resize((224, 224))
    face_array = asarray(image)
    return face_array


def get_match_score(known_embedding, candidate_embedding):
    return cosine(known_embedding, candidate_embedding)


def get_embedding(image):
    # get faces
    # faces = [cv2.imread((str(f)))for f in filenames]
    faces = [extract_face(image)]
    # convert into an array of samples
    samples = asarray(faces, 'float32')
    # prepare the face for the model, e.g. center pixels
    samples = preprocess_input(samples, version=2)
    # create a vggface model
    model = VGG_FACE
    # perform prediction
    yhat = model.predict(samples)
    return yhat


def is_match(known_embedding, candidate_embedding, thresh=0.5):
    # calculate distance between embeddings
    score = cosine(known_embedding, candidate_embedding)
    if score <= thresh:
        print('>face is a Match (%.3f <= %.3f)' % (score, thresh))
    else:
        print('>face is NOT a Match (%.3f > %.3f)' % (score, thresh))


if __name__ == '__main__':
    app.run(debug=True, port=9090)
