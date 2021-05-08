import os.path
import numpy as np
import cv2 as cv
import json
from flask import Flask, request, Response
from lip import makeUp

def data(size, r, g, b, index):
    global GS, GR, GG, GB, ID
    GS = size
    GR = r
    GG = g
    GB = b
    ID = index

""" API """
app = Flask(__name__)

@app.route('/api/upload', methods=['POST'])
def upload():
    # 이미지 받아오기
    img = cv.imdecode(np.fromstring(request.files['image'].read(), np.uint8), cv.IMREAD_UNCHANGED)
    # 서버 내 이미지 저장
    path_file = ('static/Input.jpg')
    cv.imwrite(path_file, img)
    make = makeUp()
    make.readImg() # 이미지 초기화
    make.makeUpFeatures(r=GR, g=GG, b=GB, size=(GS, GS), index=ID)
    img_processed = json.dumps(path_file)
    # json string으로 돌려받기
    return Response(response=img_processed, status=200, mimetype="application/json")

@app.route('/api/makeupP', methods=['POST'])
def parameter():
    #print(request.is_json)
    params = request.get_json()
    #print(params)
    size = params.get('size')
    r = params.get('rColor')
    g = params.get('gColor')
    b = params.get('bColor')
    index = params.get('index')
    data(size, r, g, b, index)
    return Response()

if __name__ == "__main__":
    app.run(host='0.0.0.0', port=5000)