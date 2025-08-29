package com.yamaha.ocr;

import org.opencv.core.Core;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OcrJavaApplication {

	public static void main(String[] args) {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		//System.setProperty("GOOGLE_APPLICATION_CREDENTIALS", "C:\\Users\\cj1300381\\source\\yamaha\\svn\\Codigo_Fonte\\OCR\\backend\\credencials\\yamaha-ocr-1e276144fe12.json");
		System.out.println("GOOGLE: "+System.getProperty("GOOGLE_APPLICATION_CREDENTIALS"));
		SpringApplication.run(OcrJavaApplication.class, args);
	}

}
