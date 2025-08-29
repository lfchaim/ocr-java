package com.yamaha.ocr.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageAnnotatorSettings;
import com.google.protobuf.ByteString;
import com.yamaha.ocr.image.ImageProcessor;

@RestController
@RequestMapping("/api")
public class LabelProcessorController {

	private final Pattern codeRegex = Pattern.compile("Código:\\s*([A-Z0-9-]{4,})");

	@PostMapping("/process-label")
	public String processLabel(
			@RequestParam("image") MultipartFile file,
			@RequestParam("opencv") String opencv,
			@RequestParam("delete") String delete) {

		if (file.isEmpty()) {
			return "{\"error\":\"O arquivo enviado está vazio.\"}";
		}
		
		List<String> listMethod = null;
		if( opencv != null && !opencv.isEmpty() ) {
			listMethod = new ArrayList<String>();
			if( opencv.contains(",") ) {
				String[] methodSplit = opencv.split(",");
				for( int i = 0; i < methodSplit.length; i++ ) {
					listMethod.add(methodSplit[i]);
				}
			} else {
				listMethod.add(opencv);
			}
		}

		String retMessage = null;
		
		String processedPath = null;
		// 1. Salvar o arquivo temporariamente no disco
		Path tempFilePath = null;
		try {
			tempFilePath = Files.createTempFile("label-", file.getOriginalFilename());
			file.transferTo(tempFilePath);

			// 2. Pré-processamento da imagem usando o caminho do arquivo temporário
			Mat processedImage = ImageProcessor.preprocessImage(tempFilePath.toString(),listMethod);
			if (processedImage == null) {
				return "{\"error\":\"Erro ao processar a imagem com o OpenCV.\"}";
			}
			
			processedPath = tempFilePath.toString().replaceAll(file.getOriginalFilename(), file.getOriginalFilename()+"-PROCESSED."+file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".")));
			Imgcodecs.imwrite(processedPath, processedImage);
			
			// 3. Converte a imagem processada para bytes para o Google Vision
			byte[] imageBytes = convertMatToBytes(processedImage);

			// 4. Envia para o Google Cloud Vision
			try {
				Credentials credentials = GoogleCredentials.fromStream(new FileInputStream(System.getProperty("GOOGLE_APPLICATION_CREDENTIALS")));
				//Storage storage = StorageOptions.newBuilder().setCredentials(credentials).setProjectId("my-project-id").build().getService();
				
				//ImageAnnotatorClient client = ImageAnnotatorClient.create();
				ImageAnnotatorClient client = ImageAnnotatorClient.create(ImageAnnotatorSettings.newBuilder().setCredentialsProvider(FixedCredentialsProvider.create(credentials)).build());
				
				ByteString imgBytes = ByteString.copyFrom(imageBytes);
				Image image = Image.newBuilder().setContent(imgBytes).build();

				AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
						.addFeatures(Feature.newBuilder().setType(Feature.Type.DOCUMENT_TEXT_DETECTION)).setImage(image)
						.build();

				com.google.cloud.vision.v1.BatchAnnotateImagesResponse response = client
						.batchAnnotateImages(java.util.Collections.singletonList(request));
				String fullText = response.getResponses(0).getFullTextAnnotation().getText();

				// 5. Validação com Regex
				String validatedCode = "N/A";
				java.util.regex.Matcher matcher = codeRegex.matcher(fullText);
				if (matcher.find()) {
					validatedCode = matcher.group(1);
				}

				// Retorna a resposta
				retMessage = String.format("{\"extractedText\":\"%s\", \"validatedCode\":\"%s\"}", escapeJson(fullText),
						escapeJson(validatedCode));

			} catch( Exception e ) {
				e.printStackTrace();
				retMessage = "{\"error\":\"Erro de I/O ao processar a imagem: " + e.getMessage() + "\"}";
			}
		} catch (IOException e) {
			retMessage = "{\"error\":\"Erro de I/O ao processar a imagem: " + e.getMessage() + "\"}";
		} catch (Exception e) {
			retMessage = "{\"error\":\"Erro ao processar o OCR: " + e.getMessage() + "\"}";
		} finally {
			// 6. Limpeza: Deleta o arquivo temporário
			if (tempFilePath != null) {
				try {
					Files.deleteIfExists(tempFilePath);
				} catch (IOException e) {
					System.err.println("Erro ao deletar o arquivo temporário: " + e.getMessage());
				}
			}
		}
		try {
			if( delete != null && (delete.contains("dest") || delete.contains("both")) )
				if( new File(processedPath).exists() )
					FileUtils.forceDelete(new File(processedPath));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			if( delete != null && (delete.contains("src") || delete.contains("both")) )
				if( new File(tempFilePath.toString()).exists() )
					FileUtils.forceDelete(new File(tempFilePath.toString()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return retMessage;
	}

	// Método auxiliar para escapar caracteres especiais em JSON
	private String escapeJson(String s) {
		return s.replace("\\", "\\\\").replace("\"", "\\\"");
	}

	// Método auxiliar para converter Mat para array de bytes
	private byte[] convertMatToBytes(Mat image) throws IOException {
		if (image == null || image.empty()) {
			throw new IOException("A imagem Mat de entrada está vazia ou nula.");
		}

		// Cria um objeto MatOfByte vazio
		MatOfByte matOfByte = new MatOfByte();

		// Codifica a imagem Mat para o formato PNG (ou JPEG, se preferir)
		// O PNG é geralmente preferido para texto, pois é "lossless" (sem perda de
		// qualidade)
		Imgcodecs.imencode(".png", image, matOfByte);

		// Converte o MatOfByte para um array de bytes
		return matOfByte.toArray();
	}
}