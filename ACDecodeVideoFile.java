package app;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import ac.ArithmeticDecoder;
import io.InputStreamBitSource;
import io.InsufficientBitsLeftException;

public class ACDecodeVideoFile {
	public static void main(String[] args) throws InsufficientBitsLeftException, IOException {
		String input_file_name = "data/video-compressed.dat";
		String output_file_name = "data/reuncompressedVideo.txt";

		FileInputStream fis = new FileInputStream(input_file_name);

		InputStreamBitSource bit_source = new InputStreamBitSource(fis);

		Integer[] intensities = new Integer[256];
		for (int i=0; i<256; i++) {
			intensities[i] = i;
		}

		//make model for each pixel intensity - 256 total
		FreqCountIntegerSymbolModel[] models = new FreqCountIntegerSymbolModel[256];
		
		for (int i=0; i<256; i++) {
			models[i] = new FreqCountIntegerSymbolModel(intensities);
		}
		
		// Read in number of pixels, frames and calculate pixels per frame and dimensions 
		int num_pixels = bit_source.next(32);

		int num_frames = bit_source.next(32);
		
		int pixels_per_frame = num_pixels/num_frames;
		
		// Read in range bit width 
		int range_bit_width = bit_source.next(8);
		
		ArithmeticDecoder<Integer> decoder = new ArithmeticDecoder<Integer>(range_bit_width);

		System.out.println("Uncompressing file: " + input_file_name);
		System.out.println("Output file: " + output_file_name);
		System.out.println("Range Register Bit Width: " + range_bit_width);
		System.out.println("Number of encoded pixels: " + num_pixels);
		
		FileOutputStream fos = new FileOutputStream(output_file_name);

		//previous frame to store intensities of past frame to access correct model for current frame
		Integer[] previous_frame = new Integer[4096];
		
		//initializes all intensities of previous frame to black 
			//- in the first frame all pixels will be decoded with model 0
		for(int i=0; i<pixels_per_frame; i++){
			previous_frame[i]= 0;
		}
		
		for (int i=0; i<num_frames; i++) {
			for(int j=0; j<pixels_per_frame; j++ ){
				//correct model is the model of intensity of previous frame same pixel
				FreqCountIntegerSymbolModel model = models[previous_frame[j]];
				int pixel = decoder.decode(model, bit_source);
				fos.write(pixel);
				// Update model used
				model.addToCount(pixel);
				//update previous frame to be this pixel
				previous_frame[j]=pixel;
			}
		}

		System.out.println("Done.");
		fos.flush();
		fos.close();
		fis.close();
	}
}
