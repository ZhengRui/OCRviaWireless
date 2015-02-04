Doing OCR on mobile(client) by connecting to computer(server)

currently only using bluetooth

Setting up ocr libraries on server:

1, Download links of tesseract3.03 and leptonica1.71 (ubuntu12.04 by default is tesseract3.02 and leptonica1.69)

http://worldofprasanna.in/blog/?tag=tesseract

2, Steps of building leptonica and tesseract, need to first uninstall old leptonica and tesseract by 'sudo apt-get remove leptonica', otherwise the make step in building tesseract will report errors like "undefined reference to `pixGenerateCIData'", (see link) http://code.google.com/p/tesseract-ocr/issues/detail?id=1151, make sure "/usr/include/leptonica" and "/usr/include/tesseract" are deleted, these are the default headers' position if you install old version of leptonica and tesseract by 'apt-get install'. After manually building the newest version, the path will be "/usr/local/include/leptonica" and "/usr/local/inclue/tesseract"

http://miphol.com/muse/2013/05/install-tesseract-ocr-on-ubunt.html

after installation, `tesseract -v` to check if installed correctly

3, Installation of OpenCV and python-opencv

https://help.ubuntu.com/community/OpenCV
https://github.com/jayrambhia/Install-OpenCV/tree/master/Ubuntu

4, Building python-tesseract, don't install the deb file, opencv function got problems, that's why we have to build it from code, also building it by ourself, and we can build the newest version :)

$ svn checkout http://python-tesseract.googlecode.com/svn/trunk/ python-tesseract-read-only

$ cd python-tesseract
$ python setup.py clean
$ python setup.py build

now if directly use `sudo python setup.py install`, it can be compiled without problem, but when "import tesseract" in python, it will report "cvSetData" problem, the hack can be found from this link (very important):

http://delimitry.blogspot.jp/2014_10_01_archive.html

so we should continue like this:

$ cd src  # as build folder is inside python-tesseract/src, not python-tesseract
$ c++ -pthread -shared -Wl,-O1 -Wl,-Bsymbolic-functions -Wl,-Bsymbolic-functions -Wl,-z,relro -fno-strict-aliasing -DNDEBUG -g -fwrapv -O2 -Wall -Wstrict-prototypes -D_FORTIFY_SOURCE=2 -g -fstack-protector --param=ssp-buffer-size=4 -Wformat -Werror=format-security build/temp.linux-x86_64-2.7/tesseract_wrap.o build/temp.linux-x86_64-2.7/main.o -lstdc++ -ltesseract -llept -lopencv_superres -lopencv_video -lopencv_videostab -lopencv_ml -lopencv_ocl -lopencv_contrib -lopencv_flann -lopencv_calib3d -lopencv_imgproc -lopencv_core -lopencv_legacy -lopencv_stitching -lopencv_features2d -lopencv_photo -lopencv_ts -lopencv_objdetect -lopencv_highgui -lopencv_gpu -o build/lib.linux-x86_64-2.7/_tesseract.so

after this, `ldd build/lib.linux-x86_64-2.7/_tesseract.so | grep libopencv` will not show empty as before, but show that its linked with opencv already, then copy it to python by running

$ cd ..
$ sudo python setup.py install

last check it works

$ python test-slim/test.py 

if it works, then can start the ocr server on your laptop:

$ python rfcomm-server-sdp.py

there are many ways to connect to server through bluetooth, through specific uuid or through reflection, one important thing is at first i setup a random large port number on the server, then the mobile can find the server through uuid but just can not connect it, at last i found the reason is because the prot number in the server for that specific channel should be smaller than 30, i use 11 here.

To do:
1, send ocr result back to mobile
2, connect by wifi
3, run image recogintion algorithm on server
