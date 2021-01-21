import cv2 as cv
from imutils.video import WebcamVideoStream

class VideoCamera(object):
    def __init__(self):
        self.stream = WebcamVideoStream(src=0).start()

    def get_frame(self):
        image = self.stream.read()

        ret, jpeg = cv.imencode('.jpg', image)
        data = []
        data.append(jpeg.tobytes())
        return data