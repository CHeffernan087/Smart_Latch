import base64
import os
import traceback
from sys import maxsize
from time import time

import cv2
import flask
import mtcnn
import pandas
from PIL import Image
from flask import jsonify
from google.cloud import firestore
from google.cloud import storage
from keras_vggface.utils import preprocess_input
from keras_vggface.vggface import VGGFace
from numpy import asarray
from scipy.spatial.distance import cosine

app = flask.Flask(__name__)

THRESHOLD = 0.5
VECTOR_SIZE = 2048


class NoFaceFoundException(Exception):
    pass


class NoDocumentFoundException(Exception):
    pass


class NoVectorFoundException(Exception):
    pass


def download_blob(bucket_name, source_blob_name, destination_file_name):
    """Downloads a blob from the bucket."""
    storage_client = storage.Client()
    # storage_client = storage.Client.from_service_account_json("smart-latch-fd041937391e.json")
    bucket = storage_client.bucket(bucket_name)

    blob = bucket.blob(source_blob_name)
    blob.download_to_filename(destination_file_name)


MTCNN = mtcnn.MTCNN()
VGG_FACE = VGGFace(model='resnet50', include_top=False,
                   input_shape=(224, 224, 3), pooling='avg')
download_blob("vggface-bucket", "embeddings.csv", "embeddings.csv")
VECTORS = pandas.read_csv("embeddings.csv")
VECTORS_2D_LIST = VECTORS.values.tolist()
DB = firestore.Client()
# DB = firestore.Client.from_service_account_json("smart-latch-fd041937391e.json")


@app.route("/check")
def check():
    return "Healthcheck"


@app.route("/register", methods=["POST"])
def register_face():
    image_path_str = f"{time()}_register.jpg"
    try:
        email = flask.request.form.to_dict()['email']
        flask.request.files['image'].save(image_path_str)
        face_vector = get_face_vector(cv2.imread(image_path_str))
        doc_ref = DB.collection('Users').document(email)
        user_doc = doc_ref.get()
        if user_doc.exists is False:
            raise NoDocumentFoundException(f"No doc for {email}")
        doc_ref.update({
            'face_vector': face_vector.tolist()[0]
        })
        fields_dict = doc_ref.get().to_dict()
        if 'face_vector' in fields_dict and len(fields_dict['face_vector']) == VECTOR_SIZE:
            return jsonify({"Message": f"Successfully wrote {len(fields_dict['face_vector'])} vector to user {email}"}), 200
        else:
            return jsonify({"Error": f"Failed to write recognition vector to user {email}"}), 500
    except Exception:
        return jsonify({"Error": str(traceback.format_exc())}), 500
    finally:
        if os.path.exists(image_path_str):
            os.remove(image_path_str)


@app.route("/", methods=["POST"])
def facial_recognition():
    img_path_str = f"{time()}_received.png"
    try:
        write_base64_image(img_path_str, flask.request.json["image"])
        face_vector = get_face_vector(cv2.imread(img_path_str))
        lowest_score_person = None
        lowest_score = maxsize
        for vector_list_and_person in VECTORS_2D_LIST:
            embedding_list = vector_list_and_person[:-1]
            person = vector_list_and_person[-1]
            score = get_match_score(embedding_list, face_vector)
            if score < lowest_score:
                lowest_score_person = person
                lowest_score = score
        if lowest_score <= THRESHOLD:
            return jsonify({'Person': lowest_score_person, 'Score': lowest_score})
        else:
            return jsonify({"Error": "Failed Threshold check"})
    except NoFaceFoundException:
        return jsonify({"Error": "No Face Found In Image"})
    except Exception:
        return jsonify({"Error": str(traceback.format_exc())})
    finally:
        if os.path.exists(img_path_str):
            os.remove(img_path_str)


@app.route("/verify", methods=["POST"])
def verify_face():
    img_path_str = f"{time()}_received.png"
    try:
        write_base64_image(img_path_str, flask.request.json["image"])
        door_id = flask.request.json["door"]
        fields_dict = DB.collection('Doors').document(str(door_id)).get().to_dict()
        if fields_dict is None:
            raise NoDocumentFoundException(f"No door for {door_id}")
        vectors_2d_list = []
        for user_refs in list(fields_dict['Authorised']):
            user_doc = user_refs.get().to_dict()
            #
            if user_doc is None:
                continue
            if "face_vector" not in user_doc:
                # raise NoVectorFoundException(f"User {user_doc['email']} has no face vector")
                continue
            #
            vector = user_doc['face_vector']
            vector.append(f"{user_doc['firstname']}_{user_doc['lastname']}")
            vectors_2d_list.append(vector)

        face_vector = get_face_vector(cv2.imread(img_path_str))
        lowest_score_person = None
        lowest_score = maxsize
        for embedding_list_and_person in vectors_2d_list:
            embedding_list = embedding_list_and_person[:-1]
            person = embedding_list_and_person[-1]
            score = get_match_score(embedding_list, face_vector)
            if score < lowest_score:
                lowest_score_person = person
                lowest_score = score
        if lowest_score <= THRESHOLD:
            return jsonify({'Person': lowest_score_person, 'Score': lowest_score})
        else:
            return jsonify({"Error": "Couldn't classify image"})
    except NoFaceFoundException:
        return jsonify({"Error": "No Face Found In Image"})
    except Exception:
        return jsonify({"Error": str(traceback.format_exc())})
    finally:
        if os.path.exists(img_path_str):
            os.remove(img_path_str)


def write_base64_image(img_path_str, base64_string):
    with open(img_path_str, 'wb') as file_to_save:
        decoded_image_data = base64.b64decode(base64_string)
        temp = base64.b64encode(decoded_image_data).decode('ascii').encode()
        file_to_save.write(base64.decodebytes(temp))


def extract_face(image, required_size=(224, 224)):
    detector = MTCNN
    results = detector.detect_faces(image)
    if len(results) == 0:
        raise NoFaceFoundException
    x1, y1, width, height = results[0]['box']
    x2, y2 = x1 + width, y1 + height
    face = image[y1:y2, x1:x2, :]
    image = Image.fromarray(face)
    image = image.resize(required_size)
    face_array = asarray(image)
    return face_array


def get_match_score(known_embedding, candidate_embedding):
    return cosine(known_embedding, candidate_embedding)


def get_face_vector(image):
    faces = [extract_face(image)]
    samples = asarray(faces, 'float32')
    samples = preprocess_input(samples, version=2)
    model = VGG_FACE
    y_hat = model.predict(samples)
    return y_hat


if __name__ == '__main__':
    app.run(debug=True, port=8080)
