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


app = flask.Flask(__name__)

@app.route("/", methods=["POST"])
def facial_recognition(request):
    # nparr = numpy.fromstring(request.data, numpy.uint8)
    # # decode image
    # img = cv2.imdecode(nparr, cv2.IMREAD_GRAYSCALE)
    img_file = flask.request.files['image']
    img_file.save('received.jpg')
    img = cv2.imread("received.jpg")
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
    return jsonify({'Person' : lowest_score_person, 'Score' : lowest_score})

    
def extract_face(image, required_size=(224, 224)):
  # create the detector, using default weights
  detector = mtcnn.MTCNN()
  # detect faces in the image
  results = detector.detect_faces(image)

  # extract the bounding box from the first face
  x1, y1, width, height = results[0]['box']
  x2, y2 = x1 + width, y1 + height

  # extract the face
  face = image[y1:y2, x1:x2,:]

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
  model = VGGFace(model='resnet50', include_top=False, input_shape=(224, 224, 3), pooling='avg')
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
    storage_client = storage.Client.from_service_account_json("smart-latch-fd041937391e.json")
    bucket = storage_client.bucket(bucket_name)

    # Construct a client side representation of a blob.
    # Note `Bucket.blob` differs from `Bucket.get_blob` as it doesn't retrieve
    # any content from Google Cloud Storage. As we don't need additional data,
    # using `Bucket.blob` is preferred here.
    blob = bucket.blob(source_blob_name)
    blob.download_to_filename(destination_file_name)



def hello_http(request):
    """HTTP Cloud Function.
    Args:
        request (flask.Request): The request object.
        <https://flask.palletsprojects.com/en/1.1.x/api/#incoming-request-data>
    Returns:
        The response text, or any set of values that can be turned into a
        Response object using `make_response`
        <https://flask.palletsprojects.com/en/1.1.x/api/#flask.make_response>.
    """
    request_json = request.get_json(silent=True)
    request_args = request.args

    if request_json and 'name' in request_json:
        name = request_json['name']
    elif request_args and 'name' in request_args:
        name = request_args['name']
    else:
        name = 'World'
    return 'Hello {}!'.format(escape(name))