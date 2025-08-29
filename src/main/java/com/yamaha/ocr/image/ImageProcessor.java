package com.yamaha.ocr.image;

import java.util.List;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class ImageProcessor {

    public static Mat preprocessImage(String imagePath, List<String> listMethod) {
        // Carrega a imagem
        Mat image = Imgcodecs.imread(imagePath);
        if (image.empty()) {
            return null;
        }

        Mat matConverted = image;
        
        if( listMethod != null && listMethod.contains("cvtColor") ) {
	        // Converte para escala de cinza
	        Imgproc.cvtColor(image, matConverted, Imgproc.COLOR_BGR2GRAY);
	        
        }

        if( listMethod != null && listMethod.contains("adaptiveThreshold") ) {
	        // Binarização Adaptativa
        	Mat temp = new Mat();
	        Imgproc.adaptiveThreshold(matConverted, temp, 255, 
	                                 Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, 
	                                 Imgproc.THRESH_BINARY, 11, 2);
	        matConverted = temp;
        }
        
        if( listMethod != null && listMethod.contains("medianBlur") ) {
	        // Filtro de Mediana para remover ruído
	        Mat temp = new Mat();
	        Imgproc.medianBlur(matConverted, temp, 3);
	        matConverted = temp;
        }
        
        if( listMethod != null && listMethod.contains("GaussianBlur") ) {
	        // Filtro para remover ruído do toner
	        Mat temp = new Mat();
	        Imgproc.GaussianBlur(matConverted, temp, new Size(5,5), 0);
	        matConverted = temp;
        }
        
        if( listMethod != null && listMethod.contains("medianBlur") ) {
	        // Filtro para remover pontos isolados
	        Mat temp = new Mat();
	        Imgproc.medianBlur(matConverted, temp, 3);
	        matConverted = temp;
        }

        if( listMethod != null && listMethod.contains("bilateralFilter") ) {
	        // Filtro para remover pontos isolados
	        Mat temp = new Mat();
	        Imgproc.bilateralFilter(matConverted, temp, 9, 75, 75);
	        matConverted = temp;
        }

        if( listMethod != null && listMethod.contains("threshold") ) {
	        // Filtro para limiarizacao simples
	        Mat temp = new Mat();
	        Imgproc.threshold(matConverted, temp, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
	        matConverted = temp;
        }

        if( listMethod != null && listMethod.contains("morphologyEx") ) {
	        // Filtro para remover sujeira
	        Mat temp = new Mat();
	        Imgproc.morphologyEx(matConverted, temp, Imgproc.MORPH_OPEN, new Mat(3,3,CvType.CV_8U, new Scalar(1))); //Remove ruidos pequenos
	        matConverted = temp;
        }

        if( listMethod != null && listMethod.contains("equalizeHist") ) {
	        // Filtro para remover pontos isolados
	        Mat temp = new Mat();
	        Imgproc.equalizeHist(matConverted, temp);
	        matConverted = temp;
        }

        if( listMethod != null && listMethod.contains("resize") ) {
	        // Filtro para remover pontos isolados
	        Mat temp = new Mat();
	        Imgproc.resize(matConverted, temp, new Size(), 2, 2, Imgproc.INTER_CUBIC);
	        matConverted = temp;
        }

        return matConverted;
    }
}