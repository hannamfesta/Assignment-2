package app;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import ac.ArithmeticDecoder;
import app.FreqCountIntegerSymbolModel;
import io.InputStreamBitSource;
import io.InsufficientBitsLeftException;

public class ACDecodeVideoFileAvg {
	public static void main(String[] args) throws InsufficientBitsLeftException, IOException {
		String input_file_name = "data/videoAvg-compressed.dat";
		String output_file_name = "data/reuncompressedVideoAvg.txt";

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
		
		// Read in number of pixels 
		int num_pixels = bit_source.next(32);

		int num_frames = bit_source.next(32);
		
		int pixels_per_frame = num_pixels/num_frames;
		
		int dim = (int)Math.sqrt((double)pixels_per_frame); 

		// Read in range bit width 
		int range_bit_width = bit_source.next(8);
		
		ArithmeticDecoder<Integer> decoder = new ArithmeticDecoder<Integer>(range_bit_width);

		System.out.println("Uncompressing file: " + input_file_name);
		System.out.println("Output file: " + output_file_name);
		System.out.println("Range Register Bit Width: " + range_bit_width);
		System.out.println("Number of encoded pixels: " + num_pixels);
		
		FileOutputStream fos = new FileOutputStream(output_file_name);

		Integer[][] previous_frame = new Integer[64][64];

		//initializes all intensities of previous frame to black 
		//- in the first frame all pixels will be encoded with model 0
		for(int i=0; i<dim; i++){
			for(int j=0; j<dim; j++){
				previous_frame[i][j]= 0;
			}
		}
		
		//outer loop that goes through each frame of the input 
				//inner loop goes through each pixel of frame 
		for (int i=0; i<num_frames; i++) {
			for(int j=0; j<dim; j++ ){
				for(int k=0; k<dim; k++){
					//calculate avg intensity 
					int avg_intensity=0;
					int pixelCount = 0;
					for(int m=j-1; m<=j+1; m++){
						for(int n=k-1; n<=k+1; n++){
							if(m>=0 && m<dim){
								if(n>=0 && n<dim){
									avg_intensity = avg_intensity+previous_frame[m][n];
									pixelCount++;									
								}
							}
						}
					}
					avg_intensity = avg_intensity/pixelCount;
					FreqCountIntegerSymbolModel model = models[avg_intensity];
					int pixel = decoder.decode(model, bit_source);
					fos.write(pixel);							
					//update model used
					model.addToCount(pixel);
					//update previous frame to be this pixel intensity value 
					previous_frame[j][k]=pixel;
				}
			}
		}


		System.out.println("Done.");
		fos.flush();
		fos.close();
		fis.close();
	}
}

