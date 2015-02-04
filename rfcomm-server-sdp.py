import bluetooth
import sys
import cv2
import cv2.cv as cv
import numpy as np
import tesseract

# initialize qt backend for opencv 
from PyQt4 import QtGui 
QtGui.QApplication(sys.argv)

server_sock=bluetooth.BluetoothSocket( bluetooth.RFCOMM )

# port number should be less than 30, after running server
# run 'sdptool browse local' to check if the channel number the same as port number
port = 11
server_sock.bind(("",port))
server_sock.listen(1)
print "listening on port %d" % port

#uuid = "00001101-0000-1000-8000-00805f9b34fb"
uuid = "1e0ca4ea-299d-4335-93eb-27fcfe7fa848"
bluetooth.advertise_service( server_sock, "Bluestream Service", uuid )

# Two way/lib to do ocr in python
# 1st: using madmaze/pytesseract, basically it's just a python wrapper of calling system's tesseract command
# Usage: > import Image
#        > import pytesseract
#        > print pytesseract.image_to_string(Image.open('blablabla.png'))
#        > print pytesseract.image_to_string(Image.open('bla.png'), lang='fra')
# 2nd: using code.google.com/p/python-tesseract, it's integrated with opencv but too difficult to get compiled successfully, especially requires tesseract 3.03, i finally give up installing the latest 0.9 version, but install a 0.7 version using deb file, but found that api.SetImage in example 1, tesseract.SetCvImage in example 3 4 6 doesn't work, author suggests updating Ubuntu to at least trusty? which is impossible for many ppl, good thing is tesseract.ProcessPagesBuffer in example 2 still workds, unfortunetely, ProcessPagesBuffer is to deal with stream data from reading a file, as additional file infos needed, so it can not deal with mat which only contains pixel values, too bad.
# Performance comparison: results is the same, but python-tesseract seems a bit faster and more professional

while True:
    try:
        print "listening ... "
        client_sock,address = server_sock.accept()
        print "Accepted connection from ",address

        cv2.namedWindow("BlueStream", cv2.CV_WINDOW_AUTOSIZE)
        fstPack = True
        width = 1280
        height = 720
        scale = 3
        channel = 1
        method = 1 # 0 for receiving raw YUV420sp frame 2Mb; 1 for for receiving compressed or clipped CvMat; 2 for raw gray frame
        if method == 0:
            frmsize = ( height + height / 2 ) * width
        elif method == 1:
            frmsize = ( height / scale ) * ( width / scale ) * channel
        else:
            frmsize = height * width

        frmnum = 1

        api = tesseract.TessBaseAPI()
        api.Init("/usr/local/share", "eng", tesseract.OEM_DEFAULT)
        api.SetPageSegMode(tesseract.PSM_AUTO)

        while True:
            if fstPack:
                # for receiving string data 
                data = client_sock.recv(1024)
                print "%s" % data
                fstPack = False
            else:
                # for receiving streaming video data
                remaining = frmsize
                frmparts = []
                while remaining > 0:
                    chunk = client_sock.recv(remaining)
                    frmparts.append(chunk)
                    remaining -= len(chunk)
                frmdata = b''.join(frmparts)
                print 'frame: ', frmnum, ', size(bytes): ', len(frmdata)
                frmnum += 1
                if method == 0:
                    frmYUV = np.frombuffer(frmdata, dtype='uint8').reshape(height + height / 2, width)
                    frmMat = cv2.cvtColor(frmYUV, cv2.COLOR_YUV420SP2BGRA)
                    cv2.imshow("BlueStream", frmMat)
                elif method == 1:
                    frmMat_LR = np.frombuffer(frmdata, dtype='uint8').reshape(height / scale, width / scale, channel)
                    cv2.imshow("BlueStream", frmMat_LR)
                else:
                    frmMat_gray = np.frombuffer(frmdata, dtype='uint8').reshape(height, width)
                    cv2.imshow("BlueStream", frmMat_gray)
                k = cv2.waitKey(30)
                if k == 27:
                    cv2.destroyAllWindows()
                    api.End()
                    client_sock.close()
                    break

                Mat4Ocr = frmMat if method == 0 else ( frmMat_LR if method == 1 else frmMat_gray)

                # ndarray to mat, then to iplimage

                # method 1:
                iplh, iplw, iplc = Mat4Ocr.shape
                iplimage_frm = cv.CreateImageHeader((iplw, iplh), cv.IPL_DEPTH_8U, iplc)
                cv.SetData(iplimage_frm, Mat4Ocr.tostring(), Mat4Ocr.dtype.itemsize * iplc * iplw)

                # method 2, requires iplimage to be color image, not binary, other wise report "Unknown Error Cannot convert IplImage" error :
                #cvmat_frm = cv.fromarray(Mat4Ocr)
                #iplimage_frm = cv.GetImage(cvmat_frm)



                tesseract.SetCvImage(iplimage_frm, api)
                text = api.GetUTF8Text()
                conf = api.MeanTextConf()
                print text

    except IOError:
        cv2.destroyAllWindows()
        api.End()
        client_sock.close()
        pass

#client_sock.close()
#server_sock.close()
print("disconnected")
