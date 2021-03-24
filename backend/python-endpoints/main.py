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
from google.cloud import vision
import io


THRESHOLD = 0.5


def facial_recognition(request):
    # nparr = numpy.fromstring(request.data, numpy.uint8)
    # # decode image
    # img = cv2.imdecode(nparr, cv2.IMREAD_GRAYSCALE)
    img_file = flask.request.files['image']
    img_file.save('received.jpg')
    img = cv2.imread("received.jpg")
    face_vertices_list = get_faces_coords("received.jpg")
    for index, face_vertices in enumerate(face_vertices_list):
      crop_out_face("received.jpg", face_vertices, f"received_face_{index}.jpg")
    return "Done"

    candidate_embedding = get_embedding(img)
    download_blob("vggface-bucket", "embeddings.csv", "embeddings.csv")
    embeddings = pandas.read_csv("embeddings.csv")
    embeddings_2d_list = embeddings.values.tolist()
    lowest_score_person = None
    lowest_score = maxsize
    for embedding_list_and_person in embeddings_2d_list:
        embedding_list = embedding_list_and_person[:-1]
        person = embedding_list_and_person[-1]
        score = get_match_score(embedding_list, candidate_embedding)
        if score < lowest_score:
            lowest_score_person = person
            lowest_score = score
    if lowest_score <= THRESHOLD:
        return jsonify({'Person': lowest_score_person, 'Score': lowest_score})
    else:
        return "Error - Couldn't classify image"

def crop_out_face(image_src_path, vertices_list, output_image_path):
    x = vertices_list[0][0]
    y = vertices_list[0][1]
    width = vertices_list[1][0] - x
    heigth = vertices_list[2][1] - y
    img = cv2.imread(image_src_path)
    cropped_img = img[y:y+heigth, x:x+width]
    cv2.imwrite(output_image_path, cropped_img)


def extract_face(image, required_size=(224, 224)):
    # create the detector, using default weights
    detector = mtcnn.MTCNN()
    # detect faces in the image
    results = detector.detect_faces(image)

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


def get_faces_coords(path):
    """Detects faces in an image."""
    client = vision.ImageAnnotatorClient()

    with io.open(path, 'rb') as image_file:
        content = image_file.read()

    image = vision.Image(content=content)

    response = client.face_detection(image=image)
    faces = response.face_annotations

    faces_vertices_list = []

    for face in faces:
        vertices = [(vertex.x, vertex.y) for vertex in face.bounding_poly.vertices]

        faces_vertices_list.append(vertices)

    if response.error.message:
        raise Exception('{}\nFor more info on error messages, check: '
                        'https://cloud.google.com/apis/design/errors'.format(
                            response.error.message))

    return faces_vertices_list


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
    model = VGGFace(model='resnet50', include_top=False,
                    input_shape=(224, 224, 3), pooling='avg')
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


def download_blob(bucket_name, source_blob_name, destination_file_name):
    """Downloads a blob from the bucket."""
    # bucket_name = "your-bucket-name"
    # source_blob_name = "storage-object-name"
    # destination_file_name = "local/path/to/file"
    storage_client = storage.Client.from_service_account_json(
        "smart-latch-fd041937391e.json")
    bucket = storage_client.bucket(bucket_name)

    # Construct a client side representation of a blob.
    # Note `Bucket.blob` differs from `Bucket.get_blob` as it doesn't retrieve
    # any content from Google Cloud Storage. As we don't need additional data,
    # using `Bucket.blob` is preferred here.
    blob = bucket.blob(source_blob_name)
    blob.download_to_filename(destination_file_name)
