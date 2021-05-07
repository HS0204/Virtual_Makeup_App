import os.path
import numpy as np
import cv2 as cv
import json
from flask import Flask, request, Response
import uuid
import lip

# 얼굴 인식
def faceDetect(img):
    face_cascade = cv.CascadeClassifier('haarcascade_frontalface_default.xml')
    gray = cv.cvtColor(img, cv.COLOR_BGR2GRAY)
    faces = face_cascade.detectMultiScale(gray, 1.3, 5)
    for(x,y,w,h) in faces:
        img = cv.rectangle(img, (x,y), (x+w,y+h), (0,255,0))
        # 인식한 이미지 저장
        path_file = ('static/%s.jpg' %uuid.uuid4().hex)
        cv.imwrite(path_file, img)
        return json.dumps(path_file)

""" API """
app = Flask(__name__)

@app.route('/api/upload', methods=['POST'])
def upload():
    # 이미지 받아오기
    img = cv.imdecode(np.fromstring(request.files['image'].read(), np.uint8), cv.IMREAD_UNCHANGED)
    # 서버 내 이미지 저장
    path_file = ('static/Input.jpg')
    cv.imwrite(path_file, img)
    # 이미지로 메이크업
    lip.makeUpFace()
    img_processed = path_file
    # json string으로 돌려받기
    return Response(response=img_processed, status=200, mimetype="application/json")

if __name__ == "__main__":
    app.run(host='0.0.0.0', port=5000)
    #lip.makeUpFace()